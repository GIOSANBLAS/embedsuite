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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.stringResource
import com.embedsuite.app.connection.BruceCommands
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.connection.TehLinkConsoleChips
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.ConsoleViewModel

@Composable
fun ConsoleScreen(viewModel: ConsoleViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val detectedProfile by viewModel.detectedProfile.collectAsState()
    val macros by viewModel.macros.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val isBruceLegacy = detectedProfile == FirmwareProfile.BRUCE
    val isXibalba = !isBruceLegacy
    val bruceCommands = BruceCommands.safeConsoleChips
    val tehLinkChips = TehLinkConsoleChips.chips

    val bruceImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val name = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "imported"
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()?.let { content ->
                viewModel.importBruceScript(name, content)
            }
        }
    }

    val isConnected = connectionState is ConnectionState.Connected
    val connectionStatus = when (val s = connectionState) {
        ConnectionState.Disconnected -> "DESCONECTADO — revisa USB OTG"
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
        line.startsWith("{") -> NeonCyan
        else -> MatrixGreen
    }

    LaunchedEffect(uiState.logs.size) {
        if (uiState.logs.isNotEmpty()) listState.animateScrollToItem(uiState.logs.size - 1)
    }

    val suggestions = remember(uiState.inputText, isXibalba) {
        if (uiState.inputText.isBlank()) emptyList()
        else if (isXibalba) {
            tehLinkChips
                .filter { it.label.contains(uiState.inputText, ignoreCase = true) ||
                    it.json.contains(uiState.inputText, ignoreCase = true) }
                .take(6)
                .map { it.json }
        } else {
            bruceCommands.filter { it.contains(uiState.inputText, ignoreCase = true) }.take(6)
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (isXibalba) {
                    stringResource(R.string.console_title_teh_link, connectionStatus)
                } else {
                    stringResource(R.string.console_title_bruce_legacy, connectionStatus)
                },
                color = if (isConnected) MatrixGreen else NeonRed,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            Row {
                if (isBruceLegacy) {
                    IconButton(onClick = { bruceImportLauncher.launch(arrayOf("text/*", "*/*")) }) {
                        Icon(Icons.Default.Upload, stringResource(R.string.console_import_bruce), tint = NeonCyan)
                    }
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
        if (isXibalba) {
            Text("TEH-Link:", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tehLinkChips.forEach { chip ->
                    TextButton(onClick = { viewModel.sendCommand(chip.json) }) {
                        Text(chip.label, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonCyan)
                    }
                }
            }
        }
        if (uiState.showSuggestions && suggestions.isNotEmpty()) {
            suggestions.forEach { s ->
                TextButton(onClick = { viewModel.setInput(s, false) }, modifier = Modifier.fillMaxWidth()) {
                    Text(s, fontFamily = FontFamily.Monospace, color = NeonCyan, fontSize = 10.sp)
                }
            }
        }
        if (isBruceLegacy && macros.isNotEmpty()) {
            Text(stringResource(R.string.console_bruce_scripts), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted)
            macros.take(3).forEach { m ->
                TextButton(onClick = { viewModel.runMacro(m) }) {
                    Text("▶ ${m.name}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonOrange)
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
                        if (isXibalba) stringResource(R.string.console_placeholder_teh_link)
                        else stringResource(R.string.console_placeholder_bruce),
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
}
