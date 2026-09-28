package app.pwhs.universalinstaller.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import eu.darken.porter.sdk.Porter
import eu.darken.porter.sdk.PorterBackend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The apps that serve the Shizuku API here: Shizuku itself, and Porter, which the app hands to
 * the Shizuku API through Porter's bridge while it is running.
 */
object ShizukuServices {

    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    const val PORTER_PACKAGE = "eu.darken.porter"

    fun isShizukuInstalled(context: Context): Boolean = isInstalled(context, SHIZUKU_PACKAGE)

    fun isPorterInstalled(context: Context): Boolean = isInstalled(context, PORTER_PACKAGE)

    /** A stopped service is Porter's: Porter is installed and Shizuku is not. */
    fun isPorterOnly(context: Context): Boolean = !isShizukuInstalled(context) && isPorterInstalled(context)

    /** Whether Porter, not a Shizuku server, is what the Shizuku API talks to. */
    val porterServing: Flow<Boolean> = Porter.connection.map { it?.backend == PorterBackend.PORTER }

    fun isPorterServing(): Boolean = Porter.connection.value?.backend == PorterBackend.PORTER

    private fun isInstalled(context: Context, packageName: String): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
