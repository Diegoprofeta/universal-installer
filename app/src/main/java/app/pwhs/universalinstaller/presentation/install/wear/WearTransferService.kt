package app.pwhs.universalinstaller.presentation.install.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import app.pwhs.core.telemetry.AnalyticsHelper
import app.pwhs.core.telemetry.TelemetryEvents
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.install.WatchSendState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns an APK transfer to the watch. A transfer over Bluetooth outlives the install screen, so it
 * cannot live in a ViewModel scope.
 */
class WearTransferService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var transferJob: Job? = null
    private var transferStartTime = 0L
    private val hasLoggedResult = AtomicBoolean(false)

    private fun logResultOnce(status: String, errorReason: String? = null) {
        if (hasLoggedResult.compareAndSet(false, true)) {
            val duration = if (transferStartTime > 0L) {
                (System.currentTimeMillis() - transferStartTime).coerceAtLeast(0L)
            } else 0L
            AnalyticsHelper.logWatchSendResult(
                status = status,
                durationMs = duration,
                errorReason = errorReason,
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android gives 5 seconds from startForegroundService() to this call.
        startForeground(notification(getString(R.string.watch_send_connecting)))

        when (intent?.action) {
            ACTION_CANCEL -> {
                logResultOnce(status = TelemetryEvents.STATUS_WATCH_CANCELLED)
                transferJob?.cancel()
                WearTransferState.update(WatchSendState.Idle)
                stopSelf()
            }

            ACTION_SEND -> {
                val uri = IntentCompat.getParcelableExtra(intent, EXTRA_URI, Uri::class.java)
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME)
                val isWatchApp = intent.getBooleanExtra(EXTRA_IS_WATCH_APP, true)
                val deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME)
                if (uri == null || fileName == null || transferJob?.isActive == true) {
                    if (transferJob?.isActive != true) stopSelf()
                    return START_NOT_STICKY
                }
                transferJob = scope.launch { runTransfer(uri, fileName, isWatchApp, deviceName) }
            }

            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private suspend fun runTransfer(
        uri: Uri,
        fileName: String,
        isWatchApp: Boolean,
        deviceName: String?,
    ) {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        val wakeLock = powerManager?.newWakeLock(
            android.os.PowerManager.PARTIAL_WAKE_LOCK,
            "UniversalInstaller:WearTransferSender",
        )
        wakeLock?.acquire(15 * 60 * 1000L) // 15 mins max
        transferStartTime = System.currentTimeMillis()
        hasLoggedResult.set(false)

        try {
            val fileSizeBytes = getUriFileSize(applicationContext, uri)
            val targetDeviceName = deviceName ?: WearApkSender.connectedWatchName(applicationContext)
            AnalyticsHelper.logWatchSendStarted(
                fileSizeBytes = fileSizeBytes,
                isWatchApp = isWatchApp,
                deviceName = targetDeviceName,
            )

            WearTransferState.update(WatchSendState.CheckingWatch)
            // Parsing the icon reads the whole archive, so it runs beside the transfer rather than
            // delaying it; the watch shows a placeholder until the icon lands.
            scope.launch { WearApkSender.sendIcon(applicationContext, uri, fileName) }
            val result = WearApkSender.send(
                context = applicationContext,
                apkUri = uri,
                fileName = fileName,
                onProgress = { p ->
                    WearTransferState.update(
                        WatchSendState.Sending(
                            progress = p.progress,
                            bytesSent = p.bytesSent,
                            totalBytes = p.totalBytes,
                            speedBytesPerSec = p.speedBytesPerSec,
                            etaSeconds = p.etaSeconds,
                        )
                    )
                    updateNotification(p.progress, p.etaSeconds)
                },
            )

            val (status, errorReason) = when (result) {
                is WearApkSender.SendResult.Success -> TelemetryEvents.STATUS_WATCH_SUCCESS to null
                is WearApkSender.SendResult.NoWatchFound -> TelemetryEvents.STATUS_WATCH_FAILED to "no_watch_found"
                is WearApkSender.SendResult.Unsupported -> TelemetryEvents.STATUS_WATCH_FAILED to result.reason
                is WearApkSender.SendResult.Error -> TelemetryEvents.STATUS_WATCH_FAILED to result.message
            }
            logResultOnce(status = status, errorReason = errorReason)

            if (!scope.isActive) return
            WearTransferState.update(
                when (result) {
                    is WearApkSender.SendResult.Success -> WatchSendState.Success
                    is WearApkSender.SendResult.NoWatchFound -> WatchSendState.NoWatch
                    is WearApkSender.SendResult.Unsupported -> WatchSendState.Unsupported(result.reason)
                    is WearApkSender.SendResult.Error -> WatchSendState.Error(result.message)
                }
            )
        } catch (e: CancellationException) {
            logResultOnce(status = TelemetryEvents.STATUS_WATCH_CANCELLED)
            throw e
        } catch (e: Throwable) {
            logResultOnce(status = TelemetryEvents.STATUS_WATCH_FAILED, errorReason = e.message ?: "unknown_error")
        } finally {
            if (wakeLock?.isHeld == true) {
                wakeLock.release()
            }
            stopSelf()
        }
    }

    private fun getUriFileSize(context: Context, uri: Uri): Long {
        if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.OpenableColumns.SIZE),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
                        val size = cursor.getLong(sizeIndex)
                        if (size > 0L) return size
                    }
                }
            }
        }
        if (uri.scheme == "file" || !uri.path.isNullOrEmpty()) {
            val file = runCatching { java.io.File(uri.path.orEmpty()) }.getOrNull()
            if (file != null && file.exists() && file.length() > 0L) {
                return file.length()
            }
        }
        val fdSize = runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
        }.getOrNull()
        if (fdSize != null && fdSize > 0L) {
            return fdSize
        }
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.available().toLong() }
        }.getOrDefault(0L)?.coerceAtLeast(0L) ?: 0L
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun startForeground(notification: Notification) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    private fun updateNotification(progress: Float, etaSeconds: Long? = null) {
        val percent = (progress * 100).toInt().coerceIn(0, 100)
        val etaStr = app.pwhs.core.util.TransferFormatter.formatEta(this, etaSeconds)
        val text = if (etaStr != null) {
            "${getString(R.string.watch_send_sending, percent)} ($etaStr)"
        } else {
            getString(R.string.watch_send_sending, percent)
        }
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification(text, percent))
    }

    private fun notification(text: String, progress: Int = -1): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.watch_send_channel_name),
                        NotificationManager.IMPORTANCE_LOW,
                    )
                )
            }
        }

        val cancelIntent = Intent(this, WearTransferService::class.java).setAction(ACTION_CANCEL)
        val cancelPending = PendingIntent.getService(
            this, 0, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.watch_send_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.cancel),
                cancelPending,
            )
            .apply { if (progress in 0..100) setProgress(100, progress, false) }
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "watch_send"
        private const val NOTIFICATION_ID = 1102
        private const val ACTION_SEND = "app.pwhs.universalinstaller.WATCH_SEND"
        private const val ACTION_CANCEL = "app.pwhs.universalinstaller.WATCH_SEND_CANCEL"
        private const val EXTRA_URI = "uri"
        private const val EXTRA_FILE_NAME = "file_name"
        private const val EXTRA_IS_WATCH_APP = "is_watch_app"
        private const val EXTRA_DEVICE_NAME = "device_name"

        fun start(
            context: Context,
            uri: Uri,
            fileName: String,
            isWatchApp: Boolean = true,
            deviceName: String? = null,
        ) {
            val intent = Intent(context, WearTransferService::class.java)
                .setAction(ACTION_SEND)
                .putExtra(EXTRA_URI, uri)
                .putExtra(EXTRA_FILE_NAME, fileName)
                .putExtra(EXTRA_IS_WATCH_APP, isWatchApp)
                .apply {
                    if (deviceName != null) putExtra(EXTRA_DEVICE_NAME, deviceName)
                }
            ContextCompat.startForegroundService(context, intent)
        }

        fun cancel(context: Context) {
            val intent = Intent(context, WearTransferService::class.java).setAction(ACTION_CANCEL)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
