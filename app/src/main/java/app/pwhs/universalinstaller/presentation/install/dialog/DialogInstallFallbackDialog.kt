package app.pwhs.universalinstaller.presentation.install.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R

@Composable
fun DialogInstallFallbackDialog(
    backendName: String,
    onContinueWithDefault: () -> Unit,
    onOpenApp: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.install_fallback_dialog_title, backendName))
        },
        text = {
            Text(stringResource(R.string.install_fallback_dialog_message, backendName))
        },
        confirmButton = {
            Button(onClick = onContinueWithDefault) {
                Text(stringResource(R.string.install_fallback_dialog_continue))
            }
        },
        dismissButton = {
            Row {
                if (onOpenApp != null) {
                    TextButton(onClick = onOpenApp) {
                        Text(stringResource(R.string.install_fallback_dialog_open_app, backendName))
                    }
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
    )
}
