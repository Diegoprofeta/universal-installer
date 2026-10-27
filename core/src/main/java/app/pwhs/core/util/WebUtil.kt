package app.pwhs.core.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.UriHandler
import app.pwhs.core.R

private const val TAG = "WebUtil"

/**
 * Copies [text] to the system clipboard.
 */
fun Context.copyToClipboard(text: CharSequence, label: String = "Link") {
    try {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to copy to clipboard", e)
    }
}

/**
 * Safely opens a [uri] using [Intent.ACTION_VIEW].
 * If no application can handle the intent or an exception is thrown,
 * the URI is copied to the clipboard and a toast is shown to the user.
 */
fun Context.openUriSafely(uri: String) {
    try {
        val parsedUri = Uri.parse(uri)
        val intent = Intent(Intent.ACTION_VIEW, parsedUri).apply {
            if (this@openUriSafely !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        startActivity(intent)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to open URI: $uri", e)
        copyToClipboard(uri)
        Toast.makeText(this, getString(R.string.error_cannot_open_link_copied), Toast.LENGTH_SHORT).show()
    }
}

/**
 * Safely opens a [uri] via Compose's [UriHandler].
 * If opening fails (e.g. ActivityNotFoundException wrapped in IllegalArgumentException),
 * the URI is copied to clipboard and a toast is displayed.
 */
fun UriHandler.openUriSafely(context: Context, uri: String) {
    try {
        openUri(uri)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to open URI: $uri", e)
        context.copyToClipboard(uri)
        Toast.makeText(context, context.getString(R.string.error_cannot_open_link_copied), Toast.LENGTH_SHORT).show()
    }
}
