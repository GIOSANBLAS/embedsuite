package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.connection.DeviceEvent
import com.embedsuite.app.connection.LinkDebugLog
import com.embedsuite.app.connection.TransportType
import com.embedsuite.app.core.AppVersion
import com.embedsuite.app.core.error.HumanErrorMapper
import com.embedsuite.app.data.BruceCustomCommandEntity
import com.embedsuite.app.data.BruceCustomCommandRepository
import com.embedsuite.app.data.MacroEntity
import com.embedsuite.app.data.MacroRepository
import com.embedsuite.app.macro.MacroEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private fun consoleHeader(): String =
    "[SYSTEM] EMBED SUITE v${AppVersion.NAME} — Bruce CLI companion"

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
    private val macroEngine: MacroEngine,
    private val customCommandRepository: BruceCustomCommandRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsoleUiState())
    val uiState: StateFlow<ConsoleUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState
    val activeTransport: StateFlow<TransportType> = connectionManager.activeTransportType
    val detectedProfile: StateFlow<FirmwareProfile> = connectionManager.detectedProfile
    val customCommands: StateFlow<List<BruceCustomCommandEntity>> =
        customCommandRepository.allCommands.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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
                    is DeviceEvent.TehLinkNotice -> appendLog("[AVISO] ${event.message}")
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
                    is DeviceEvent.TehLinkAsyncEvent -> Unit
                    is DeviceEvent.RfJammerStopped -> Unit
                    is DeviceEvent.RfScanStopped -> Unit
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
        if (trimmed.startsWith("{")) {
            appendLog("[ERROR] ${com.embedsuite.app.core.bruce.BruceLimits.JSON_REJECTED}")
            return
        }
        appendLog("> $trimmed")
        val history = (_uiState.value.commandHistory + trimmed).takeLast(100)
        _uiState.update { it.copy(commandHistory = history, historyIndex = history.size, inputText = "", showSuggestions = false) }
        viewModelScope.launch {
            connectionManager.sendBruceCliLine(trimmed)
                .onSuccess { response ->
                    if (response.isNotBlank()) {
                        response.lines().forEach { appendLog(it) }
                    }
                }
                .onFailure { appendLog("[ERROR] ${HumanErrorMapper.map(it)}") }
        }
    }

    fun saveCustomCommand(name: String, command: String) {
        if (name.isBlank() || command.isBlank()) return
        viewModelScope.launch {
            customCommandRepository.save(name, command)
            appendLog("[INFO] Comando guardado: $name")
        }
    }

    fun deleteCustomCommand(id: Long) {
        viewModelScope.launch { customCommandRepository.delete(id) }
    }

    fun clearLog() {
        _uiState.update { it.copy(logs = listOf(consoleHeader())) }
    }

    fun runMacro(macro: MacroEntity) {
        viewModelScope.launch {
            macroEngine.execute(macro).onFailure { appendLog("[ERROR] ${it.message}") }
        }
    }

    fun importBruceCliMacro(name: String, content: String) {
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
                if (cmd.startsWith("{")) {
                    appendLog("[ERROR] JSON obsoleto — usa comandos CLI Bruce o wait Nms")
                    return@launch
                }
            }
            macroRepository.save(MacroEntity(name = name, commands = content))
            appendLog("[INFO] Macro Bruce '$name' importado (${lines.size} líneas)")
        }
    }

    fun runBruceScript(content: String, saveAsMacro: Boolean = false, macroName: String = "Script") {
        viewModelScope.launch {
            val macro = MacroEntity(name = macroName, commands = content)
            macroEngine.execute(macro).fold(
                onSuccess = { count ->
                    appendLog("[INFO] Script .bruce OK ($count pasos)")
                    if (saveAsMacro) {
                        macroRepository.save(macro)
                        appendLog("[INFO] Guardado como macro '$macroName'")
                    }
                },
                onFailure = { appendLog("[ERROR] ${it.message}") }
            )
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
