package app.pwhs.universalinstaller.presentation.install.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.install.wear.WearApkSender
import app.pwhs.universalinstaller.presentation.manage.ManageActivity
import app.pwhs.universalinstaller.presentation.setting.InstallMode
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.universalinstaller.presentation.setting.SettingViewModel
import app.pwhs.universalinstaller.presentation.setting.ShizukuState
import app.pwhs.universalinstaller.presentation.setting.security.util.SystemInstallerManager
import app.pwhs.universalinstaller.util.MicroGCompat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.random.Random
import org.koin.androidx.compose.koinViewModel

enum class InstallFeatureTipType(
    val id: String,
    val icon: ImageVector,
    val titleRes: Int,
    val badgeRes: Int,
    val descRes: Int,
    val actionRes: Int,
) {
    SYNC_SERVER(
        id = "sync_server",
        icon = Icons.Rounded.WifiTethering,
        titleRes = R.string.tip_sync_server_title,
        badgeRes = R.string.tip_sync_server_badge,
        descRes = R.string.tip_sync_server_desc,
        actionRes = R.string.tip_sync_server_action,
    ),
    WEAR_OS(
        id = "wear_os",
        icon = Icons.Rounded.Watch,
        titleRes = R.string.tip_wear_os_title,
        badgeRes = R.string.tip_wear_os_badge,
        descRes = R.string.tip_wear_os_desc,
        actionRes = R.string.tip_wear_os_action,
    ),
    URL_DOWNLOAD(
        id = "url_download",
        icon = Icons.Rounded.CloudDownload,
        titleRes = R.string.tip_url_download_title,
        badgeRes = R.string.tip_url_download_badge,
        descRes = R.string.tip_url_download_desc,
        actionRes = R.string.tip_url_download_action,
    ),
    MANAGE_APPS(
        id = "manage_apps",
        icon = Icons.Rounded.Inventory2,
        titleRes = R.string.tip_manage_apps_title,
        badgeRes = R.string.tip_manage_apps_badge,
        descRes = R.string.tip_manage_apps_desc,
        actionRes = R.string.tip_manage_apps_action,
    );

    companion object {
        fun availableTips(): List<InstallFeatureTipType> {
            return entries.filter { tip ->
                if (tip == WEAR_OS) WearApkSender.isAvailable else true
            }
        }
    }
}

@Composable
fun InstallTipOverlay(
    onOpenSyncServer: () -> Unit,
    onOpenWatchSheet: () -> Unit,
    onOpenDownloadTab: () -> Unit,
    modifier: Modifier = Modifier,
    settingViewModel: SettingViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingState by settingViewModel.uiState.collectAsState()
    val useDhizuku by settingViewModel.useDhizuku.collectAsState()
    val dhizukuState by settingViewModel.dhizukuState.collectAsState()

    var isSystemInstallerFrozen by remember {
        mutableStateOf(SystemInstallerManager.isSystemPackageInstallerDisabled(context))
    }

    var sessionRecorded by rememberSaveable { mutableStateOf(false) }
    var isSessionEligible by rememberSaveable { mutableStateOf(false) }
    var isTipDismissedInSession by rememberSaveable { mutableStateOf(false) }
    var activeTipId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!sessionRecorded) {
            sessionRecorded = true
            val prefs = context.dataStore.data.first()
            val sessionCount = prefs[PreferencesKeys.TIP_SESSION_COUNT] ?: 0
            val nextTipSession = prefs[PreferencesKeys.NEXT_TIP_SESSION] ?: 0

            val eligible = sessionCount >= nextTipSession
            context.dataStore.edit { p ->
                if (eligible) {
                    p[PreferencesKeys.NEXT_TIP_SESSION] = sessionCount + Random.nextInt(1, 4)
                }
                p[PreferencesKeys.TIP_SESSION_COUNT] = sessionCount + 1
            }
            isSessionEligible = eligible
        }
    }

    LifecycleResumeEffect(Unit) {
        isSystemInstallerFrozen = SystemInstallerManager.isSystemPackageInstallerDisabled(context)
        settingViewModel.updateShizukuState()
        onPauseOrDispose {}
    }

    val isShizukuDismissed by remember(context) {
        context.dataStore.data.map {
            it[PreferencesKeys.DISMISS_SHIZUKU_PROMO_BANNER] ?: false
        }
    }.collectAsState(initial = true)

    val dismissedFeatureTips by remember(context) {
        context.dataStore.data.map {
            it[PreferencesKeys.DISMISSED_FEATURE_TIPS] ?: emptySet()
        }
    }.collectAsState(initial = emptySet())

    val microGAvailable = remember(context) { MicroGCompat.isAvailable(context) }

    val configuredMode = remember(
        settingState.useShizuku,
        settingState.useRoot,
        useDhizuku,
        settingState.useCustomAuthorizer,
        settingState.useMicroG,
    ) {
        InstallMode.from(
            useShizuku = settingState.useShizuku,
            useRoot = settingState.useRoot,
            useDhizuku = useDhizuku,
            useCustomAuthorizer = settingState.useCustomAuthorizer,
            useMicroG = settingState.useMicroG,
        )
    }

    val effectiveMode = remember(
        configuredMode,
        settingState.shizukuState,
        settingState.rootState,
        dhizukuState,
        microGAvailable,
        isSystemInstallerFrozen,
    ) {
        InstallMode.resolveEffective(
            configuredMode = configuredMode,
            shizukuState = settingState.shizukuState,
            rootState = settingState.rootState,
            dhizukuState = dhizukuState,
            isMicroGAvailable = microGAvailable,
            isSystemInstallerFrozen = isSystemInstallerFrozen,
        )
    }

    val shizukuState = settingState.shizukuState

    val shouldShowShizuku = !isShizukuDismissed &&
        effectiveMode == InstallMode.DEFAULT &&
        shizukuState != ShizukuState.READY &&
        shizukuState != ShizukuState.UNSUPPORTED

    var wasShizukuShownThisSession by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(shouldShowShizuku, isSessionEligible, isTipDismissedInSession) {
        if (shouldShowShizuku && isSessionEligible && !isTipDismissedInSession) {
            wasShizukuShownThisSession = true
        }
    }

    if (wasShizukuShownThisSession) {
        if (isSessionEligible) {
            ShizukuPromoBanner(
                modifier = modifier,
                settingViewModel = settingViewModel,
                onDismissed = {
                    isTipDismissedInSession = true
                },
            )
        }
        return
    }

    val availableTips = remember { InstallFeatureTipType.availableTips() }
    val unDismissedTips = remember(availableTips, dismissedFeatureTips) {
        availableTips.filter { !dismissedFeatureTips.contains(it.id) }
    }

    LaunchedEffect(unDismissedTips, isSessionEligible) {
        if (isSessionEligible && activeTipId == null && unDismissedTips.isNotEmpty()) {
            activeTipId = unDismissedTips.random().id
        }
    }

    val currentFeatureTip = remember(activeTipId, unDismissedTips) {
        unDismissedTips.firstOrNull { it.id == activeTipId }
    }

    val isTipVisible = isSessionEligible && !isTipDismissedInSession && currentFeatureTip != null

    AnimatedVisibility(
        visible = isTipVisible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + shrinkVertically() + slideOutVertically { it },
        modifier = modifier,
    ) {
        if (currentFeatureTip == null) return@AnimatedVisibility

        val onDismiss: () -> Unit = {
            isTipDismissedInSession = true
            scope.launch {
                context.dataStore.edit { prefs ->
                    val current = prefs[PreferencesKeys.DISMISSED_FEATURE_TIPS] ?: emptySet()
                    prefs[PreferencesKeys.DISMISSED_FEATURE_TIPS] = current + currentFeatureTip.id
                }
            }
        }

        val onActionClick: () -> Unit = {
            onDismiss()
            when (currentFeatureTip) {
                InstallFeatureTipType.SYNC_SERVER -> onOpenSyncServer()
                InstallFeatureTipType.WEAR_OS -> onOpenWatchSheet()
                InstallFeatureTipType.URL_DOWNLOAD -> onOpenDownloadTab()
                InstallFeatureTipType.MANAGE_APPS -> {
                    val intent = Intent(context, ManageActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                    context.startActivity(intent)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = currentFeatureTip.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = stringResource(currentFeatureTip.titleRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.width(6.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = stringResource(currentFeatureTip.badgeRes),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.shizuku_promo_dismiss),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = stringResource(currentFeatureTip.descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(
                        onClick = onActionClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Text(
                            text = stringResource(currentFeatureTip.actionRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
