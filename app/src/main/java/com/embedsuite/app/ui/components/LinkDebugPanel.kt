package com.embedsuite.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.embedsuite.app.R
import com.embedsuite.app.connection.LinkDebugLog
import com.embedsuite.app.connection.DebugCategory
import com.embedsuite.app.connection.DebugDirection
import com.embedsuite.app.ui.theme.*

@Composable
fun LinkDebugPanel(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val context = LocalContext.current
    val lines by LinkDebugLog.lines.collectAsState()
    var filter by remember { mutableStateOf(DebugCategory.ALL) }
    var fullscreen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val filtered = remember(lines, filter) { LinkDebugLog.filtered(filter) }

    LaunchedEffect(filtered.size) {
        if (filtered.isNotEmpty()) listState.animateScrollToItem(filtered.lastIndex)
    }

    val content: @Composable () -> Unit = {
        Column(modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.link_debug_title, filtered.size),
                    fontFamily = FontFamily.Monospace,
                    fontSize = if (compact) 10.sp else 12.sp,
                    color = NeonCyan
                )
                Row {
                    if (!compact) {
                        IconButton(onClick = { fullscreen = true }) {
                            Icon(Icons.Default.Fullscreen, null, tint = MatrixGreen)
                        }
                    }
                    IconButton(onClick = { copyDebugLog(context, filter) }) {
                        Icon(Icons.Default.ContentCopy, null, tint = NeonCyan)
                    }
                    IconButton(onClick = { LinkDebugLog.clear() }) {
                        Icon(Icons.Default.DeleteSweep, null, tint = NeonOrange)
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DebugCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = filter == cat,
                        onClick = { filter = cat },
                        label = {
                            Text(
                                debugCategoryLabel(cat),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                color = if (filter == cat) BlackAMOLED else MatrixGreen,
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MatrixGreen,
                            containerColor = BlackAMOLED
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 120.dp else 180.dp)
                    .background(BlackAMOLED, RoundedCornerShape(4.dp))
                    .border(1.dp, MatrixGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                if (filtered.isEmpty()) {
                    Text(
                        stringResource(R.string.tools_debug_empty),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = TextGray
                    )
                } else {
                    LazyColumn(state = listState) {
                        items(filtered, key = { "${it.timestamp}-${it.text.hashCode()}" }) { line ->
                            LinkDebugLineRow(line)
                        }
                    }
                }
            }
        }
    }

    content()

    if (fullscreen) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(BlackAMOLED)
                    .padding(12.dp)
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(R.string.link_debug_fullscreen_title),
                            fontFamily = FontFamily.Monospace,
                            color = MatrixGreen
                        )
                        TextButton(onClick = { fullscreen = false }) {
                            Text(
                                stringResource(R.string.action_close),
                                fontFamily = FontFamily.Monospace,
                                color = NeonRed
                            )
                        }
                    }
                    LinkDebugPanel(modifier = Modifier.weight(1f), compact = false)
                }
            }
        }
    }
}

@Composable
private fun LinkDebugLineRow(line: com.embedsuite.app.connection.DebugLine) {
    val color = when {
        line.category == DebugCategory.ERROR -> NeonRed
        line.direction == DebugDirection.OUT -> NeonCyan
        line.category == DebugCategory.RF -> NeonOrange
        line.category == DebugCategory.STORAGE -> KaliBlue
        else -> MatrixGreen
    }
    val prefix = if (line.direction == DebugDirection.OUT) "TX>" else "RX>"
    Text(
        "$prefix ${line.text}",
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        color = color,
        lineHeight = 12.sp,
        modifier = Modifier.padding(vertical = 1.dp)
    )
}

private fun copyDebugLog(context: Context, filter: DebugCategory) {
    val text = LinkDebugLog.asPlainText(filter)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("TEH-Link debug", text))
    Toast.makeText(context, context.getString(R.string.tools_debug_copied, text.lines().size), Toast.LENGTH_SHORT).show()
}

@Composable
private fun debugCategoryLabel(cat: DebugCategory): String = when (cat) {
    DebugCategory.ALL -> stringResource(R.string.debug_cat_all)
    DebugCategory.RF -> stringResource(R.string.debug_cat_rf)
    DebugCategory.SYSTEM -> stringResource(R.string.debug_cat_system)
    DebugCategory.ERROR -> stringResource(R.string.debug_cat_error)
    DebugCategory.STORAGE -> stringResource(R.string.debug_cat_storage)
    DebugCategory.OTHER -> stringResource(R.string.debug_cat_other)
}
