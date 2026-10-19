package app.pwhs.universalinstaller.presentation.install.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.domain.model.ApkInfo
import app.pwhs.universalinstaller.domain.model.VtStatus

@Composable
fun ApkInfoFooter(
    apkInfo: ApkInfo,
    isExpanded: Boolean,
    startCompact: Boolean,
    strictSecurity: Boolean,
    confirmText: String?,
    cancelText: String?,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    onCheckVirusTotal: () -> Unit,
) {
    if (isExpanded) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }

    val isDowngrade = apkInfo.installedVersionCode != null &&
        apkInfo.installedVersionCode > 0 &&
        apkInfo.versionCode < apkInfo.installedVersionCode

    val hasVerdict = apkInfo.vtResult?.status in setOf(
        VtStatus.CLEAN,
        VtStatus.MALICIOUS,
        VtStatus.SUSPICIOUS,
    )
    val isScanCompleted = hasVerdict || !strictSecurity
    val isScanning = apkInfo.vtResult?.status in setOf(
        VtStatus.SCANNING,
        VtStatus.UPLOADING,
        VtStatus.QUEUED,
        VtStatus.ANALYZING,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpand)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.dialog_menu_details),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = {
                    if (cancelText == null && isExpanded && startCompact) {
                        onCollapse()
                    } else {
                        onCancel()
                    }
                },
                modifier = Modifier
                    .weight(0.35f)
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = cancelText ?: if (isExpanded && startCompact) {
                        stringResource(R.string.dialog_back_btn)
                    } else {
                        stringResource(R.string.cancel)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Button(
                onClick = onInstall,
                modifier = Modifier
                    .weight(0.65f)
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !apkInfo.isBlocked,
                colors = if (isDowngrade) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                },
            ) {
                if (confirmText == null) {
                    Icon(Icons.Rounded.InstallMobile, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = confirmText ?: if (isDowngrade) {
                        stringResource(R.string.dialog_downgrade_btn)
                    } else {
                        stringResource(R.string.txt_install)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (strictSecurity && !isScanCompleted) {
            OutlinedButton(
                onClick = onCheckVirusTotal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !isScanning,
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.scanning_progress), style = MaterialTheme.typography.labelMedium)
                } else {
                    Icon(Icons.Rounded.Security, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.scan_virustotal_btn), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
