package com.embedsuite.app

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.data.SignalRepository
import com.embedsuite.app.rf.RfReplayEngine
import com.embedsuite.app.ui.components.NeonButton
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SignalLibraryScreen(
    signalRepository: SignalRepository,
    connectionManager: DeviceConnectionManager,
    rfReplayEngine: RfReplayEngine,
    highlightSignalId: Long? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("TODOS") }
    var editingSignal by remember { mutableStateOf<CapturedSignalEntity?>(null) }
    var replayTarget by remember { mutableStateOf<CapturedSignalEntity?>(null) }
    var replayStatus by remember { mutableStateOf("") }
    var labelInput by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val signals by signalRepository.search(searchQuery).collectAsState(initial = emptyList())
    val filtered = signals.filter {
        when (filterType) {
            "RF" -> it.signalType == "RF"
            "WIFI" -> it.signalType == "WIFI"
            "BLE" -> it.signalType == "BLE"
            "FAV" -> it.favorite
            else -> true
        }
    }.let { list ->
        if (filterType == "TODOS") list.sortedByDescending { it.favorite } else list
    }

    LaunchedEffect(highlightSignalId, filtered) {
        highlightSignalId?.let { id ->
            val index = filtered.indexOfFirst { it.id == id }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Text(stringResource(R.string.library_title), fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MatrixGreen)
        Text(stringResource(R.string.library_entries, filtered.size), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray)
        if (replayStatus.isNotBlank()) {
            Text(replayStatus, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonCyan)
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.library_search_hint), color = TextGray, fontFamily = FontFamily.Monospace) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MatrixGreen, focusedTextColor = MatrixGreen, unfocusedTextColor = MatrixGreen)
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("TODOS", "RF", "WIFI", "BLE", "FAV").forEach { f ->
                FilterChip(
                    selected = filterType == f,
                    onClick = { filterType = f },
                    label = {
                        Text(f, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = if (filterType == f) BlackAMOLED else MatrixGreen)
                    },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MatrixGreen, containerColor = DarkSurface)
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.library_empty), fontFamily = FontFamily.Monospace, color = TextGray)
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filtered, key = { it.id }) { signal ->
                    SignalLibraryCard(
                        signal = signal,
                        highlighted = signal.id == highlightSignalId,
                        onRetransmit = { replayTarget = signal },
                        onToggleFavorite = {
                            scope.launch {
                                val next = !signal.favorite
                                signalRepository.setFavorite(signal.id, next)
                                if (next) {
                                    com.embedsuite.app.widget.WidgetStateStore.updateFavoriteLabel(
                                        context,
                                        signal.label.ifBlank { signal.protocol.ifBlank { signal.name } }
                                    )
                                } else {
                                    val top = signalRepository.getFavoriteRf(1).firstOrNull()
                                    com.embedsuite.app.widget.WidgetStateStore.updateFavoriteLabel(
                                        context,
                                        top?.label?.ifBlank { top.protocol.ifBlank { top.name } }
                                    )
                                }
                            }
                        },
                        onEdit = {
                            editingSignal = signal
                            labelInput = signal.label.ifBlank { signal.name }
                            tagsInput = signal.tags
                        },
                        onDelete = { scope.launch { signalRepository.delete(signal.id) } }
                    )
                }
            }
        }
    }

    replayTarget?.let { signal ->
        val preview = remember(signal) { rfReplayEngine.preview(signal) }
        AlertDialog(
            onDismissRequest = { replayTarget = null },
            containerColor = DarkSurface,
            title = { Text("Preview TX", fontFamily = FontFamily.Monospace, color = NeonOrange) },
            text = {
                Column {
                    Text("Protocolo: ${preview.protocol}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
                    Text("Freq: ${preview.frequency} MHz", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray)
                    Text("Cmd: ${preview.command}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonCyan)
                    Text(preview.summary.take(200), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted)
                    if (!preview.canTransmit) {
                        Text(
                            preview.blockerMessage,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = NeonOrange,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                NeonButton(
                    text = if (preview.canTransmit) "TRANSMITIR" else "NO DISPONIBLE",
                    enabled = preview.canTransmit,
                    onClick = {
                        scope.launch {
                            rfReplayEngine.replay(signal).fold(
                                onSuccess = { replayStatus = it; replayTarget = null },
                                onFailure = { replayStatus = "Error: ${it.message}"; replayTarget = null }
                            )
                        }
                    }
                )
            },
            dismissButton = { TextButton(onClick = { replayTarget = null }) { Text("Cancelar", color = TextGray) } }
        )
    }

    editingSignal?.let { signal ->
        AlertDialog(
            onDismissRequest = { editingSignal = null },
            containerColor = DarkSurface,
            title = { Text("Editar", fontFamily = FontFamily.Monospace, color = MatrixGreen) },
            text = {
                Column {
                    OutlinedTextField(labelInput, { labelInput = it }, label = { Text("Label") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(tagsInput, { tagsInput = it }, label = { Text("Tags") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        signalRepository.updateLabel(signal.id, labelInput, tagsInput)
                        editingSignal = null
                    }
                }) { Text("Guardar", color = MatrixGreen) }
            },
            dismissButton = { TextButton(onClick = { editingSignal = null }) { Text("Cancelar", color = TextGray) } }
        )
    }
}

@Composable
private fun SignalLibraryCard(
    signal: CapturedSignalEntity,
    highlighted: Boolean = false,
    onRetransmit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = FlipperCardBg),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().border(
            width = if (highlighted) 2.dp else 1.dp,
            color = if (highlighted) NeonOrange else MatrixGreen.copy(alpha = 0.2f),
            shape = RoundedCornerShape(6.dp)
        )
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    (if (signal.favorite) "★ " else "") + signal.label.ifBlank { signal.protocol.ifBlank { signal.name } },
                    fontFamily = FontFamily.Monospace,
                    color = FlipperSignalNeon,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(signal.signalType, fontFamily = FontFamily.Monospace, color = NeonCyan, fontSize = 10.sp)
            }
            Text(
                "${signal.frequency} // ${signal.deviceId.ifBlank { signal.macAddress }}",
                fontFamily = FontFamily.Monospace,
                color = TextGray,
                fontSize = 10.sp
            )
            if (signal.decodedFields.isNotBlank()) {
                Text(
                    signal.decodedFields.lineSequence().firstOrNull() ?: signal.decodedFields.take(40),
                    fontFamily = FontFamily.Monospace, color = FlipperAccentCyan, fontSize = 9.sp
                )
            }
            Row(Modifier.padding(top = 6.dp)) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (signal.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (signal.favorite) "Quitar favorito" else "Favorito",
                        tint = if (signal.favorite) NeonOrange else TextGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (signal.signalType == "RF") {
                    IconButton(onClick = onRetransmit) {
                        Icon(Icons.AutoMirrored.Filled.Send, "TX", tint = NeonOrange, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, tint = MatrixGreen, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = NeonRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
