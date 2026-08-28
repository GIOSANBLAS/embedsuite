package com.embedsuite.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.core.bruce.BruceCliCatalog
import com.embedsuite.app.core.bruce.BruceConsoleChips
import com.embedsuite.app.ui.components.BruceNavPad
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.ConsoleViewModel

@Composable
fun ConsoleScreen(viewModel: ConsoleViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val activeTransport by viewModel.activeTransport.collectAsState()
    val customCommands by viewModel.customCommands.collectAsState()
    val listState = rememberLazyListState()
    val cliChips = BruceConsoleChips.chips
    var showCustomDialog by remember { mutableStateOf(false) }
    var showCatalogDialog by remember { mutableStateOf(false) }
    var showAddCustom by remember { mutableStateOf(false) }
    var newCmdName by remember { mutableStateOf("") }
    var newCmdText by remember { mutableStateOf("") }
    var showScriptDialog by remember { mutableStateOf(false) }
    var scriptText by remember { mutableStateOf("info\nwait 500ms\nuptime") }

    val isConnected = connectionState is ConnectionState.Connected
    val connectionStatus = when (val s = connectionState) {
        ConnectionState.Disconnected -> "DESCONECTADO"
        ConnectionState.Connecting -> "CONECTANDO..."
        is ConnectionState.Connected -> "CONECTADO // ${s.detail}"
        is ConnectionState.Error -> "ERROR: ${s.message}"
    }

    fun colorForLine(line: String) = when {
        line.startsWith("[ERROR]") || line.contains("error", ignoreCase = true) -> NeonRed
        line.startsWith(">") -> NeonCyan
        line.startsWith("[RF]") -> NeonOrange
        line.startsWith("[SYS]") -> KaliBlue
        line.startsWith("[WARN]") -> NeonOrange
        line.startsWith("[INFO]") || line.startsWith("[SYSTEM]") -> MatrixGreen.copy(alpha = 0.7f)
        else -> MatrixGreen
    }

    LaunchedEffect(uiState.logs.size) {
        if (uiState.logs.isNotEmpty()) listState.animateScrollToItem(uiState.logs.size - 1)
    }

    val suggestions = remember(uiState.inputText) {
        if (uiState.inputText.isBlank()) emptyList()
        else cliChips
            .filter {
                it.label.contains(uiState.inputText, ignoreCase = true) ||
                    it.command.contains(uiState.inputText, ignoreCase = true)
            }
            .take(6)
            .map { it.command }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.console_title_bruce_cli, connectionStatus),
                    color = if (isConnected) MatrixGreen else NeonRed,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
                if (isConnected) {
                    Text(
                        "${activeTransport.name} · 115200 8N1 · Bruce CLI",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                }
            }
            Row {
                TextButton(onClick = { showScriptDialog = true }) {
                    Text(".bruce", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonOrange)
                }
                TextButton(onClick = { showCatalogDialog = true }) {
                    Text("Ref", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonCyan)
                }
                TextButton(onClick = { showCustomDialog = true }) {
                    Text("Mis", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = EmbedGreen)
                }
                IconButton(onClick = { viewModel.clearLog() }) {
                    Icon(Icons.Default.DeleteSweep, "Limpiar", tint = TextMuted)
                }
                IconButton(onClick = { viewModel.reconnect() }) {
                    Icon(Icons.Default.Refresh, null, tint = MatrixGreen)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.weight(1f).fillMaxWidth().background(DarkSurfaceElevated, MaterialTheme.shapes.small)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(8.dp)) {
                items(uiState.logs) { line ->
                    Text(line, color = colorForLine(line), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
        if (isConnected) {
            BruceNavPad(
                enabled = true,
                compact = true,
                onNav = { viewModel.sendCommand(it) },
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Text(stringResource(R.string.console_bruce_cli_chips), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted)
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            cliChips.forEach { chip ->
                SuggestionChip(
                    onClick = { viewModel.sendCommand(chip.command) },
                    label = {
                        Text(chip.label, fontFamily = FontFamily.Monospace, fontSize = 9.sp, maxLines = 1)
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = DarkSurfaceElevated,
                        labelColor = NeonCyan
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = NeonCyan.copy(alpha = 0.45f)
                    )
                )
            }
        }
        if (uiState.showSuggestions && suggestions.isNotEmpty()) {
            suggestions.forEach { s ->
                TextButton(onClick = { viewModel.setInput(s, false) }, modifier = Modifier.fillMaxWidth()) {
                    Text(s, fontFamily = FontFamily.Monospace, color = NeonCyan, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = { viewModel.setInput(it) },
                placeholder = {
                    Text(
                        stringResource(R.string.console_placeholder_bruce_cli),
                        color = TextGray,
                        fontFamily = FontFamily.Monospace
                    )
                },
                modifier = Modifier.weight(1f).onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && uiState.commandHistory.isNotEmpty()) {
                        when (event.key) {
                            Key.DirectionUp -> { viewModel.navigateHistory(up = true); true }
                            Key.DirectionDown -> { viewModel.navigateHistory(up = false); true }
                            else -> false
                        }
                    } else false
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MatrixGreen, focusedTextColor = MatrixGreen, unfocusedTextColor = MatrixGreen),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.sendCommand(uiState.inputText) })
            )
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = { viewModel.sendCommand(uiState.inputText) }, modifier = Modifier.background(MatrixGreen, MaterialTheme.shapes.small)) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = BlackAMOLED)
            }
        }
    }

    if (showCatalogDialog) {
        AlertDialog(
            onDismissRequest = { showCatalogDialog = false },
            title = { Text("Referencia CLI Bruce", color = EmbedGreen, fontFamily = FontFamily.Monospace) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BruceCliCatalog.entries) { entry ->
                        Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)) {
                            Column(Modifier.padding(10.dp).fillMaxWidth()) {
                                Text(entry.name, color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                Text(entry.command, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                Text(entry.description, color = TextMuted, fontSize = 9.sp)
                                TextButton(onClick = {
                                    viewModel.sendCommand(entry.command)
                                    showCatalogDialog = false
                                }) { Text("Ejecutar", color = MatrixGreen) }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCatalogDialog = false }) { Text("Cerrar") }
            }
        )
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Mis comandos CLI", color = EmbedGreen, fontFamily = FontFamily.Monospace) },
            text = {
                Column {
                    if (customCommands.isEmpty()) {
                        Text("Sin comandos guardados. Pulsa + Añadir.", color = TextMuted, fontSize = 12.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 320.dp)) {
                            items(customCommands, key = { it.id }) { cmd ->
                                Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(cmd.name, color = EmbedGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                            Text(cmd.command, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                        }
                                        Row {
                                            TextButton(onClick = { viewModel.sendCommand(cmd.command) }) {
                                                Text("Run", color = MatrixGreen, fontSize = 10.sp)
                                            }
                                            IconButton(onClick = { viewModel.deleteCustomCommand(cmd.id) }) {
                                                Icon(Icons.Default.Close, null, tint = NeonRed, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddCustom = true }) { Text("+ Añadir", color = EmbedGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("Cerrar") }
            }
        )
    }

    if (showScriptDialog) {
        AlertDialog(
            onDismissRequest = { showScriptDialog = false },
            title = { Text("Script .bruce (multi-línea)", color = NeonOrange, fontFamily = FontFamily.Monospace) },
            text = {
                Column {
                    Text(
                        "Una línea = un comando CLI. # comentarios. wait 500ms entre pasos.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = scriptText,
                        onValueChange = { scriptText = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MatrixGreen, unfocusedTextColor = MatrixGreen)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.runBruceScript(scriptText)
                    showScriptDialog = false
                }) { Text("Ejecutar", color = MatrixGreen) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.runBruceScript(scriptText, saveAsMacro = true, macroName = "Script ${System.currentTimeMillis() % 10000}")
                        showScriptDialog = false
                    }) { Text("Run+Guardar", color = NeonCyan) }
                    TextButton(onClick = { showScriptDialog = false }) { Text("Cancelar") }
                }
            }
        )
    }

    if (showAddCustom) {
        AlertDialog(
            onDismissRequest = { showAddCustom = false },
            title = { Text("Nuevo comando", color = EmbedGreen) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newCmdName, onValueChange = { newCmdName = it }, label = { Text("Nombre") }, singleLine = true)
                    OutlinedTextField(value = newCmdText, onValueChange = { newCmdText = it }, label = { Text("CLI Bruce") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveCustomCommand(newCmdName, newCmdText)
                    newCmdName = ""
                    newCmdText = ""
                    showAddCustom = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustom = false }) { Text("Cancelar") }
            }
        )
    }
}
