package app.pwhs.universalinstaller.presentation.composable

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.domain.model.BackendIcon
import app.pwhs.universalinstaller.domain.model.InstallBackend
import app.pwhs.universalinstaller.presentation.install.controller.RootState
import app.pwhs.universalinstaller.presentation.setting.InstallMode
import app.pwhs.universalinstaller.presentation.setting.SettingViewModel
import app.pwhs.universalinstaller.presentation.setting.ShizukuState
import app.pwhs.universalinstaller.presentation.setting.security.util.SystemInstallerManager
import app.pwhs.universalinstaller.util.DhizukuCompat
import app.pwhs.universalinstaller.util.DhizukuState
import app.pwhs.universalinstaller.util.MicroGCompat
import org.koin.androidx.compose.koinViewModel

/**
 * Pill showing the active install backend. Tapping it opens a picker so the user can switch
 * engine (PackageInstaller / Shizuku / Dhizuku / Root) right from the Install and Manage screens —
 * the switch reuses [SettingViewModel.setInstallMode], which runs the same Shizuku-permission
 * / root-request ladder as the Settings screen.
 */
@Composable
fun InstallerModeBadge(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settingViewModel: SettingViewModel = koinViewModel()
    val settingState by settingViewModel.uiState.collectAsState()
    val useDhizuku by settingViewModel.useDhizuku.collectAsState()
    val dhizukuState by settingViewModel.dhizukuState.collectAsState()

    var isSystemInstallerFrozen by remember {
        mutableStateOf(SystemInstallerManager.isSystemPackageInstallerDisabled(context))
    }
    var canInstallPackages by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else true
        )
    }

    LifecycleResumeEffect(Unit) {
        isSystemInstallerFrozen = SystemInstallerManager.isSystemPackageInstallerDisabled(context)
        canInstallPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
        onPauseOrDispose {}
    }

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

    val activeBackend = remember(
        settingState.backendPriority,
        settingState.useShizuku,
        settingState.useRoot,
        useDhizuku,
        settingState.useCustomAuthorizer,
        settingState.useMicroG,
        settingState.shizukuState,
        settingState.rootState,
        dhizukuState,
        microGAvailable,
        isSystemInstallerFrozen,
    ) {
        var resolved: InstallBackend? = null
        for (backend in settingState.backendPriority) {
            when (backend) {
                InstallBackend.SHIZUKU -> {
                    if (settingState.useShizuku && settingState.shizukuState == ShizukuState.READY) {
                        resolved = backend
                        break
                    }
                }
                InstallBackend.DHIZUKU -> {
                    if (useDhizuku && dhizukuState == DhizukuState.READY) {
                        resolved = backend
                        break
                    }
                }
                InstallBackend.ROOT -> {
                    if (settingState.useRoot && (settingState.rootState == RootState.READY || settingState.rootState == RootState.UNKNOWN)) {
                        resolved = backend
                        break
                    }
                }
                InstallBackend.CUSTOM -> {
                    if (settingState.useCustomAuthorizer) {
                        resolved = backend
                        break
                    }
                }
                InstallBackend.MICROG -> {
                    if (settingState.useMicroG && microGAvailable) {
                        resolved = backend
                        break
                    }
                }
                InstallBackend.DEFAULT -> {
                    if (!isSystemInstallerFrozen) {
                        resolved = backend
                        break
                    }
                }
            }
        }
        resolved ?: if (!isSystemInstallerFrozen) InstallBackend.DEFAULT
        else if (settingState.shizukuState == ShizukuState.READY) InstallBackend.SHIZUKU
        else if (settingState.rootState == RootState.READY || settingState.rootState == RootState.UNKNOWN) InstallBackend.ROOT
        else if (dhizukuState == DhizukuState.READY) InstallBackend.DHIZUKU
        else InstallBackend.DEFAULT
    }

    LaunchedEffect(settingState.useRoot, settingState.rootState) {
        if (settingState.useRoot && settingState.rootState == RootState.UNKNOWN) {
            settingViewModel.retryRoot()
        }
    }

    var showPicker by remember { mutableStateOf(false) }

    // Surface the hint events the switcher emits (e.g. "install Shizuku", "permission denied")
    // so the user isn't left wondering why the engine didn't change.
    LaunchedEffect(Unit) {
        settingViewModel.events.collect { stringRes ->
            Toast.makeText(context, context.getString(stringRes), Toast.LENGTH_LONG).show()
        }
    }

    val label = stringResource(activeBackend.titleRes)
    val privileged = activeBackend != InstallBackend.DEFAULT
    val container = if (privileged)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (privileged)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable { showPicker = true }
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackendIcon(
            backend = activeBackend,
            modifier = Modifier.size(16.dp),
            tint = content,
        )
        Text(
            text = stringResource(R.string.installer_mode_using, label),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(start = 6.dp),
        )
    }

    if (showPicker) {
        InstallPriorityBottomSheet(
            onDismissRequest = { showPicker = false },
            settingViewModel = settingViewModel,
        )
    }
}

