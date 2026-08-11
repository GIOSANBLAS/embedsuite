package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.connection.TehLinkResponseParser
import com.embedsuite.app.connection.LinkDebugLog
import com.embedsuite.app.connection.DeviceEvent
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.connection.TransportType
import com.embedsuite.app.core.AppVersion
import com.embedsuite.app.core.error.HumanErrorMapper
import com.embedsuite.app.engine.terminal.NaturalLanguageTranslator
import com.embedsuite.app.data.MacroEntity
import com.embedsuite.app.data.MacroRepository
import com.embedsuite.app.macro.MacroEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private fun consoleHeader(): String =
    "[SYSTEM] EMBED SUITE v${AppVersion.NAME} — TEH-Link / Xibalba"

data class ConsoleUiState(
    val logs: List<String> = listOf(consoleHeader()),
    val inputText: String = "",
    val showSuggestions: Boolean = false,
    val commandHistory: List<String> = emptyList(),
    val historyIndex: Int = -1
)

class ConsoleViewModel(
    private val connectionManager: DeviceConnectionManager,
    private val macroRepository: MacroRepository,
    private val macroEngine: MacroEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsoleUiState())
    val uiState: StateFlow<ConsoleUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState
    val detectedProfile: StateFlow<FirmwareProfile> = connectionManager.detectedProfile
    val macros: StateFlow<List<MacroEntity>> = macroRepository.allMacros
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            connectionManager.events.collect { event ->
                when (event) {
                    is DeviceEvent.RawLine -> appendLog(event.line)
                    is DeviceEvent.SubGhzSignal -> appendLog("[RF] ${event.entry.protocol} @ ${event.entry.frequency}")
                    is DeviceEvent.SubGhzSignalSaved -> appendLog("[RF] saved #${event.signalId} ${event.entry.protocol}")
                    is DeviceEvent.SystemInfoUpdate -> appendLog("[SYS] uptime=${event.info.uptime} heap=${event.info.freeHeap}")
                    is DeviceEvent.TehLinkNotice -> appendLog("[TEH-LINK] ${event.message}")
                    is DeviceEvent.OtaCompleted -> appendLog(
                        "[OTA] state=${event.status.state} sha256=${event.status.sha256Verified} total=${event.status.totalSize}B"
                    )
                    is DeviceEvent.WaveformSample -> Unit
                    is DeviceEvent.BleAdSpamProgress -> Unit
                    is DeviceEvent.MousejackDongle -> Unit
                    is DeviceEvent.NfcCloneProgress -> Unit
                    is DeviceEvent.SubGhzDecodedFrame -> Unit
                    is DeviceEvent.SubGhzSample -> Unit
                    is DeviceEvent.WifiProbe -> Unit
                    is DeviceEvent.RfScanSample ->
                        appendLog("[SCAN] ${event.freqMhz} MHz @ ${event.rssi} dBm")
                    is DeviceEvent.RfScanStateChanged ->
                        appendLog(if (event.running) "[SCAN] iniciado" else "[SCAN] detenido ${event.detail}")
                    is DeviceEvent.RfJammerStateChanged ->
                        appendLog(if (event.running) "[JAMMER] ACTIVO ${event.freqMhz} MHz" else "[JAMMER] detenido")
                    is DeviceEvent.NfcCardDetected ->
                        appendLog("[NFC] ${event.uid} (${event.type})")
                    is DeviceEvent.NfcReaderStateChanged ->
                        appendLog(if (event.running) "[NFC] lector activo" else "[NFC] lector detenido")
                }
            }
        }
    }

    fun setInput(text: String, showSuggestions: Boolean = text.isNotBlank()) {
        _uiState.update { it.copy(inputText = text, showSuggestions = showSuggestions) }
    }

    fun navigateHistory(up: Boolean) {
        val history = _uiState.value.commandHistory
        if (history.isEmpty()) return
        val current = _uiState.value.historyIndex
        val newIndex = when {
            up -> if (current <= 0) history.lastIndex else current - 1
            else -> (current + 1).coerceAtMost(history.lastIndex)
        }
        _uiState.update {
            it.copy(
                historyIndex = newIndex,
                inputText = history.getOrElse(newIndex) { "" }
            )
        }
    }

    fun sendCommand(cmd: String) {
        if (cmd.isBlank()) return
        val trimmed = cmd.trim()
        val payload = if (!trimmed.startsWith("{")) {
            val translated = NaturalLanguageTranslator.translate(trimmed)
            if (translated == null) {
                appendLog("[ERROR] ${HumanErrorMapper.mapMessage("nl_not_understood: $trimmed")}")
                return
            }
            appendLog("[NL] ${translated.explanation}")
            translated.json
        } else {
            trimmed
        }
        val display = TehLinkResponseParser.redactSensitiveRequest(payload)
        appendLog("> $display")
        val history = _uiState.value.commandHistory + cmd
        _uiState.update { it.copy(commandHistory = history, historyIndex = history.size, inputText = "", showSuggestions = false) }
        viewModelScope.launch {
            connectionManager.sendTehLinkRaw(payload)
                .onFailure { appendLog("[ERROR] ${HumanErrorMapper.map(it)}") }
        }
    }

    fun runMacro(macro: MacroEntity) {
        viewModelScope.launch {
            macroEngine.execute(macro).onFailure { appendLog("[ERROR] ${it.message}") }
        }
    }

    fun importTehLinkMacro(name: String, content: String) {
        viewModelScope.launch {
            val lines = content.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
            if (lines.isEmpty()) {
                appendLog("[ERROR] Macro vacío o sin pasos válidos")
                return@launch
            }
            for (cmd in lines) {
                if (cmd.startsWith("wait ", ignoreCase = true)) continue
                if (!cmd.startsWith("{")) {
                    appendLog("[ERROR] Macro inválido: solo JSON TEH-Link o wait Nms")
                    return@launch
                }
            }
            macroRepository.save(MacroEntity(name = name, commands = content))
            appendLog("[INFO] Macro TEH-Link '$name' importado (${lines.size} líneas)")
        }
    }

    fun reconnect() {
        viewModelScope.launch { connectionManager.connect(TransportType.USB) }
    }

    private fun appendLog(line: String) {
        val safe = LinkDebugLog.sanitize(line)
        _uiState.update { it.copy(logs = (it.logs + safe).takeLast(500)) }
    }
}
