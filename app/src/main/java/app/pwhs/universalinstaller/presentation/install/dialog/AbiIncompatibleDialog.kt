package app.pwhs.universalinstaller.presentation.install.dialog

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
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.install.util.AbiCompatibilityHelper

@Composable
fun AbiIncompatibleDialog(
    apkAbis: List<String>,
    onDismiss: () -> Unit,
    onInstallAnyway: (() -> Unit)? = null,
) {
    val apkAbisText = if (apkAbis.isNotEmpty()) {
        val bitness = if (AbiCompatibilityHelper.isApk64BitOnly(apkAbis)) "64-bit: " else if (AbiCompatibilityHelper.isApk32BitOnly(apkAbis)) "32-bit: " else ""
        bitness + apkAbis.joinToString(", ")
    } else "Unknown"

    val deviceAbis = android.os.Build.SUPPORTED_ABIS
    val deviceAbisText = if (deviceAbis.isNotEmpty()) {
        val bitness = if (AbiCompatibilityHelper.isDevice32BitOnly(deviceAbis)) "32-bit: " else if (AbiCompatibilityHelper.isDevice64BitOnly(deviceAbis)) "64-bit: " else ""
        bitness + deviceAbis.joinToString(", ")
    } else "Unknown"

    val recommended = AbiCompatibilityHelper.getRecommendedArchitecture(apkAbis, deviceAbis)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Memory,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = stringResource(R.string.install_abi_incompatible_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.install_abi_incompatible_dialog_message,
                        apkAbisText,
                        deviceAbisText,
                        recommended,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "APK:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(70.dp),
                            )
                            Text(
                                text = apkAbisText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Device:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(70.dp),
                            )
                            Text(
                                text = deviceAbisText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (onInstallAnyway != null) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onInstallAnyway()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.install_abi_incompatible_dialog_install_anyway))
                }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}
