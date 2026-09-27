package app.pwhs.universalinstaller.presentation.composable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMerge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.pwhs.universalinstaller.R

@Composable
fun AntiSplitPromptDialog(
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.CallMerge,
                contentDescription = null,
            )
        },
        title = {
            Text(text = stringResource(R.string.antisplit_not_installed_title))
        },
        text = {
            Text(text = stringResource(R.string.antisplit_not_installed_desc))
        },
        confirmButton = {
            Button(
                onClick = {
                    onDownload()
                    onDismiss()
                },
            ) {
                Text(text = stringResource(R.string.antisplit_download_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}
