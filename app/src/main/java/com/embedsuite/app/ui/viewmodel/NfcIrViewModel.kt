package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.connection.BruceCommands
import com.embedsuite.app.connection.BruceEvent
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.data.IrButtonEntity
import com.embedsuite.app.data.IrRepository
import com.embedsuite.app.data.NfcDumpEntity
import com.embedsuite.app.data.NfcDumpRepository
import com.embedsuite.app.nfc.MifareParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class NfcIrUiState(
    val modo: String = "NFC / RFID",
    val estadoOperacion: String = "ESPERANDO TARJETA O SEÑAL...",
    val nfcUid: String = "—",
    val nfcDump: String = "",
    val parsedMifare: String = "",
    val savedDumps: List<NfcDumpEntity> = emptyList(),
    val selectedDumpId: Long? = null
)

class NfcIrViewModel(
    private val connectionManager: DeviceConnectionManager,
    private val irRepository: IrRepository,
    private val nfcDumpRepository: NfcDumpRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NfcIrUiState())
    val uiState: StateFlow<NfcIrUiState> = _uiState.asStateFlow()

    val connectionState = connectionManager.connectionState
    val irButtons: StateFlow<List<IrButtonEntity>> = irRepository.allButtons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            if (irRepository.allButtons.first().isEmpty()) {
                listOf(
                    IrButtonEntity(buttonName = "POWER", bruceCommand = BruceCommands.irTx("NEC", "00FF", "00FF")),
                    IrButtonEntity(buttonName = "VOL+", bruceCommand = BruceCommands.irTx("NEC", "00FF", "807F")),
                    IrButtonEntity(buttonName = "VOL-", bruceCommand = BruceCommands.irTx("NEC", "00FF", "40BF")),
                    IrButtonEntity(buttonName = "CH+", bruceCommand = BruceCommands.irTx("NEC", "00FF", "20DF")),
                    IrButtonEntity(buttonName = "CH-", bruceCommand = BruceCommands.irTx("NEC", "00FF", "A05F")),
                    IrButtonEntity(buttonName = "MUTE", bruceCommand = BruceCommands.irTx("NEC", "00FF", "906F"))
                ).forEach { irRepository.save(it) }
            }
        }
        viewModelScope.launch {
            nfcDumpRepository.observeAll().collect { dumps ->
                _uiState.update { it.copy(savedDumps = dumps) }
            }
        }
        viewModelScope.launch {
            connectionManager.events.collect { event ->
                if (event is BruceEvent.RawLine) handleLine(event.line)
            }
        }
    }

    private fun handleLine(line: String) {
        val trimmed = line.trim()
        if (trimmed.contains("UID", ignoreCase = true) || trimmed.matches(Regex("""[0-9A-Fa-f: ]{8,}"""))) {
            Regex("""[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){3,}""").find(trimmed)?.value?.let { uid ->
                _uiState.update { it.copy(nfcUid = uid, estadoOperacion = "TARJETA DETECTADA") }
            }
        }
        if (_uiState.value.modo == "NFC / RFID" && trimmed.isNotBlank()) {
            val dump = (_uiState.value.nfcDump + trimmed + "\n").takeLast(4000)
            val sectors = MifareParser.parseDump(dump)
            val parsed = if (sectors.isNotEmpty()) MifareParser.formatVisual(_uiState.value.nfcUid, sectors) else _uiState.value.parsedMifare
            _uiState.update { it.copy(nfcDump = dump, parsedMifare = parsed) }
        }
    }

    fun setModo(modo: String) { _uiState.update { it.copy(modo = modo) } }

    fun readNfc() {
        _uiState.update {
            it.copy(estadoOperacion = BruceCommands.NFC_CLI_UNSUPPORTED, nfcDump = "", parsedMifare = "")
        }
    }

    fun emulateUid(uid: String? = null) {
        _uiState.update {
            it.copy(estadoOperacion = BruceCommands.NFC_CLI_UNSUPPORTED)
        }
    }

    fun emulateFromDump(dump: NfcDumpEntity) {
        _uiState.update {
            it.copy(
                nfcUid = dump.uid,
                nfcDump = dump.rawDump,
                parsedMifare = dump.parsedSectors,
                selectedDumpId = dump.id,
                estadoOperacion = BruceCommands.NFC_CLI_UNSUPPORTED
            )
        }
    }

    fun saveDump() {
        viewModelScope.launch {
            val state = _uiState.value
            nfcDumpRepository.save(
                NfcDumpEntity(
                    uid = state.nfcUid,
                    tagType = "MIFARE Classic 1K",
                    rawDump = state.nfcDump,
                    parsedSectors = state.parsedMifare
                )
            )
            _uiState.update { it.copy(estadoOperacion = "Dump NFC guardado") }
        }
    }

    fun clearDump() { _uiState.update { it.copy(nfcDump = "", parsedMifare = "") } }

    fun sendIr(cmd: String) {
        val normalized = BruceCommands.normalizeIrCommand(cmd)
        _uiState.update { it.copy(estadoOperacion = "TX: $normalized") }
        viewModelScope.launch { connectionManager.sendCommand(normalized) }
    }

    fun captureIr() {
        _uiState.update { it.copy(estadoOperacion = "ESCUCHANDO SEÑAL IR (10s)...") }
        viewModelScope.launch { connectionManager.sendCommand(BruceCommands.irRxRaw(10)) }
    }

    fun saveIrButton(name: String, command: String) {
        viewModelScope.launch {
            irRepository.save(
                IrButtonEntity(buttonName = name, bruceCommand = BruceCommands.normalizeIrCommand(command))
            )
        }
    }
}
