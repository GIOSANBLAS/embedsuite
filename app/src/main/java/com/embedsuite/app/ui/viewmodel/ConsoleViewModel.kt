package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.connection.BruceCommandValidator
import com.embedsuite.app.connection.TehLinkResponseParser
import com.embedsuite.app.connection.BruceDebugLog
import com.embedsuite.app.connection.BruceEvent
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.connection.TransportType
import com.embedsuite.app.core.AppVersion
import com.embedsuite.app.data.MacroEntity
import com.embedsuite.app.data.MacroRepository
import com.embedsuite.app.macro.MacroEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private fun consoleHeader(profile: FirmwareProfile): String = when (profile) {
    FirmwareProfile.XIBALBA -> "[SYSTEM] EMBED SUITE v${AppVersion.NAME} — TEH-Link / Xibalba"
    FirmwareProfile.BRUCE -> "[SYSTEM] EMBED SUITE v${AppVersion.NAME} — BRUCE CLI"
    else -> "[SYSTEM] EMBED SUITE v${AppVersion.NAME} — CLI"
}

data class ConsoleUiState(
    val logs: List<String> = listOf(consoleHeader(FirmwareProfile.XIBALBA)),
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
            connectionManager.detectedProfile.collect { profile ->
                val header = consoleHeader(profile)
                _uiState.update { state ->
                    val logs = if (state.logs.size == 1 && state.logs[0].startsWith("[SYSTEM]")) {
                        listOf(header)
                    } else {
                        state.logs
                    }
                    state.copy(logs = logs)
                }
            }
        }
        viewModelScope.launch {
            connectionManager.events.collect { event ->
                when (event) {
                    is BruceEvent.RawLine -> appendLog(event.line)
                    is BruceEvent.SubGhzSignal -> appendLog("[RF] ${event.entry.protocol} @ ${event.entry.frequency}")
                    is BruceEvent.SubGhzSignalSaved -> appendLog("[RF] saved #${event.signalId} ${event.entry.protocol}")
                    is BruceEvent.SystemInfoUpdate -> appendLog("[SYS] uptime=${event.info.uptime} heap=${event.info.freeHeap}")
                    is BruceEvent.TehLinkNotice -> appendLog("[TEH-LINK] ${event.message}")
                    is BruceEvent.WaveformSample -> Unit
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
        val display = if (trimmed.startsWith("{")) {
            TehLinkResponseParser.redactSensitiveRequest(trimmed)
        } else {
            trimmed
        }
        appendLog("> $display")
        val history = _uiState.value.commandHistory + cmd
        _uiState.update { it.copy(commandHistory = history, historyIndex = history.size, inputText = "", showSuggestions = false) }
        viewModelScope.launch {
            val result = if (trimmed.startsWith("{")) {
                connectionManager.sendTehLinkRaw(trimmed)
            } else {
                connectionManager.sendCommand(trimmed)
            }
            result.onFailure { appendLog("[ERROR] ${it.message}") }
        }
    }

    fun runMacro(macro: MacroEntity) {
        viewModelScope.launch {
            macroEngine.execute(macro).onFailure { appendLog("[ERROR] ${it.message}") }
        }
    }

    fun importBruceScript(name: String, content: String) {
        viewModelScope.launch {
            val lines = content.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
            if (lines.isEmpty()) {
                appendLog("[ERROR] Script vacío o sin comandos válidos")
                return@launch
            }
            for (cmd in lines) {
                if (cmd.startsWith("wait ", ignoreCase = true)) continue
                BruceCommandValidator.validate(cmd).getOrElse {
                    appendLog("[ERROR] Script inválido: ${it.message}")
                    return@launch
                }
            }
            macroRepository.save(MacroEntity(name = name, commands = content))
            appendLog("[INFO] Script '$name' importado (${lines.size} líneas)")
        }
    }

    fun reconnect() {
        viewModelScope.launch { connectionManager.connect(TransportType.USB) }
    }

    private fun appendLog(line: String) {
        val safe = BruceDebugLog.sanitize(line)
        _uiState.update { it.copy(logs = (it.logs + safe).takeLast(500)) }
    }
}
