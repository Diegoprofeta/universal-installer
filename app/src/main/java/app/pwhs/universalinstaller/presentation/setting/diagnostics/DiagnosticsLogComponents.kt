package app.pwhs.universalinstaller.presentation.setting.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pwhs.universalinstaller.R

enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR, OTHER }

data class LogEntry(
    val time: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val raw: String,
)

fun parseLogLine(line: String): LogEntry {
    val threadTimeRegex =
        Regex("""^(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEF])\s+(.*?)\s*:\s*(.*)$""")
    val match = threadTimeRegex.find(line.trim())

    return if (match != null) {
        val (dateTime, _, _, levelChar, tag, message) = match.destructured
        LogEntry(
            time = dateTime.substringAfterLast(" ").trim(),
            level = when (levelChar) {
                "V" -> LogLevel.VERBOSE
                "D" -> LogLevel.DEBUG
                "I" -> LogLevel.INFO
                "W" -> LogLevel.WARN
                "E", "F" -> LogLevel.ERROR
                else -> LogLevel.OTHER
            },
            tag = tag.trim(),
            message = message,
            raw = line,
        )
    } else {
        val timeRegex = Regex("""(\d{2}:\d{2}:\d{2}\.\d{3})""")
        val extractedTime = timeRegex.find(line)?.value ?: ""

        val level = when {
            line.contains(" E ") || line.contains(" F ") || line.contains("E/") -> LogLevel.ERROR
            line.contains(" W ") || line.contains("W/") -> LogLevel.WARN
            line.contains(" I ") || line.contains("I/") -> LogLevel.INFO
            line.contains(" D ") || line.contains("D/") -> LogLevel.DEBUG
            line.contains(" V ") || line.contains("V/") -> LogLevel.VERBOSE
            else -> LogLevel.OTHER
        }

        LogEntry(extractedTime, level, "", line, line)
    }
}

@Composable
fun levelColor(level: LogLevel): Color {
    return when (level) {
        LogLevel.VERBOSE -> Color(0xFFBBBBBB)
        LogLevel.DEBUG -> Color(0xFF3382DD)
        LogLevel.INFO -> Color(0xFF2BBAC5)
        LogLevel.WARN -> Color(0xFFFF9100)
        LogLevel.ERROR -> Color(0xFFFF5353)
        LogLevel.OTHER -> Color(0xFFEEEEEE)
    }
}

@Composable
fun LogItem(entry: LogEntry) {
    val color = levelColor(entry.level)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (entry.time.isNotEmpty()) {
            Text(
                text = entry.time,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = Color(0xFFAAAAAA),
                ),
                modifier = Modifier.width(80.dp),
            )
            Spacer(Modifier.width(4.dp))
        }

        Text(
            text = when (entry.level) {
                LogLevel.VERBOSE -> "V"
                LogLevel.DEBUG -> "D"
                LogLevel.INFO -> "I"
                LogLevel.WARN -> "W"
                LogLevel.ERROR -> "E"
                LogLevel.OTHER -> "?"
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            ),
            modifier = Modifier.width(10.dp),
        )

        Spacer(Modifier.width(4.dp))

        if (entry.tag.isNotEmpty()) {
            Text(
                text = entry.tag,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = color.copy(alpha = 0.7f),
                ),
                modifier = Modifier.width(80.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(4.dp))
        }

        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = if (entry.level == LogLevel.OTHER) Color(0xFFEEEEEE) else color,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun LogList(lines: List<String>, listState: LazyListState) {
    if (lines.isEmpty() || (lines.size == 1 && lines[0].isBlank())) {
        EmptyLogsState(stringResource(R.string.diagnostics_no_session_logs))
        return
    }

    val parsed = remember(lines) {
        lines.map { parseLogLine(it) }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF121212),
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "TIME",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFAAAAAA),
                    modifier = Modifier.width(80.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "L",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFAAAAAA),
                    modifier = Modifier.width(10.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "TAG",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFAAAAAA),
                    modifier = Modifier.width(80.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("MESSAGE", style = MaterialTheme.typography.labelSmall, color = Color(0xFFAAAAAA))
            }
            HorizontalDivider(color = Color(0xFF333333))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                itemsIndexed(parsed, key = { index, _ -> index }) { _, entry ->
                    LogItem(entry = entry)
                }
            }
        }
    }
}

@Composable
fun CrashContent(text: String, onClearRequest: () -> Unit) {
    val noCrashes = text.isBlank() || text.trim() == "No crash logs"

    Column(Modifier.fillMaxSize()) {
        if (noCrashes) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyLogsState(stringResource(R.string.diagnostics_no_crashes))
            }
        } else {
            val lines = remember(text) { text.lines() }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF121212),
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    items(lines) { line ->
                        val entry = remember(line) { parseLogLine(line) }
                        if (entry.time.isNotEmpty() || entry.tag.isNotEmpty()) {
                            LogItem(entry = entry)
                        } else {
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp,
                                    fontSize = 10.sp,
                                ),
                                color = when {
                                    line.contains("Exception") || line.contains("CRASH") || line.contains("Error") ->
                                        Color(0xFFFF5353)
                                    line.startsWith("=") ->
                                        MaterialTheme.colorScheme.primary
                                    else ->
                                        Color(0xFFEEEEEE)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onClearRequest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.diagnostics_clear_crashes))
            }
        }
    }
}

@Composable
fun EmptyLogsState(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.diagnostics_loading),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
