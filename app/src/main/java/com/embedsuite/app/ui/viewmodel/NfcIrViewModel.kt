package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.connection.TehLinkIrUtils
import com.embedsuite.app.connection.DeviceEvent
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.FirmwareProfile
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
    val detectedProfile = connectionManager.detectedProfile
    val systemInfo = connectionManager.systemInfo
    val irButtons: StateFlow<List<IrButtonEntity>> = irRepository.allButtons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nfcDeviceEnabled: StateFlow<Boolean> = combine(
        connectionState,
        detectedProfile,
        systemInfo
    ) { conn, profile, _ ->
        conn is ConnectionState.Connected &&
            (profile != FirmwareProfile.XIBALBA || connectionManager.hasXibalbaCapability("nfc"))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val irDeviceEnabled: StateFlow<Boolean> = combine(
        connectionState,
        detectedProfile,
        systemInfo
    ) { conn, profile, _ ->
        conn is ConnectionState.Connected &&
            (profile != FirmwareProfile.XIBALBA || connectionManager.hasXibalbaCapability("ir"))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            if (irRepository.allButtons.first().isEmpty()) {
                listOf(
                    IrButtonEntity(buttonName = "POWER", irPayload = TehLinkIrUtils.irTx("NEC", "00FF", "00FF")),
                    IrButtonEntity(buttonName = "VOL+", irPayload = TehLinkIrUtils.irTx("NEC", "00FF", "807F")),
                    IrButtonEntity(buttonName = "VOL-", irPayload = TehLinkIrUtils.irTx("NEC", "00FF", "40BF")),
                    IrButtonEntity(buttonName = "CH+", irPayload = TehLinkIrUtils.irTx("NEC", "00FF", "20DF")),
                    IrButtonEntity(buttonName = "CH-", irPayload = TehLinkIrUtils.irTx("NEC", "00FF", "A05F")),
                    IrButtonEntity(buttonName = "MUTE", irPayload = TehLinkIrUtils.irTx("NEC", "00FF", "906F"))
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
                if (event is DeviceEvent.RawLine) handleLine(event.line)
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
        viewModelScope.launch {
            _uiState.update { it.copy(estadoOperacion = "LEYENDO TAG...", nfcDump = "", parsedMifare = "") }
            if (detectedProfile.value != FirmwareProfile.XIBALBA) {
                _uiState.update { it.copy(estadoOperacion = "NFC requiere firmware Xibalba (TEH-Link).") }
                return@launch
            }
            connectionManager.tehLinkRunNfcRead().fold(
                onSuccess = { result ->
                    val uid = result.state.nfc?.uid.orEmpty().ifBlank { "—" }
                    _uiState.update {
                        it.copy(
                            nfcUid = uid,
                            nfcDump = if (uid != "—") "UID: $uid\nSAK: ${result.state.nfc?.sak ?: 0}" else "",
                            estadoOperacion = result.state.message.ifBlank { result.state.state }.ifBlank { "LECTURA OK" }
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(estadoOperacion = "ERROR NFC: ${err.message}") }
                }
            )
        }
    }

    fun emulateUid(uid: String? = null) {
        val targetUid = uid?.takeIf { it.isNotBlank() && it != "—" } ?: _uiState.value.nfcUid
        if (targetUid.isBlank() || targetUid == "—") {
            _uiState.update { it.copy(estadoOperacion = "UID vacío — lee un tag primero.") }
            return
        }
        viewModelScope.launch {
            if (detectedProfile.value != FirmwareProfile.XIBALBA) {
                _uiState.update { it.copy(estadoOperacion = "Emulación UID requiere Xibalba (TEH-Link).") }
                return@launch
            }
            connectionManager.tehLinkRunNfcEmulate(targetUid.replace(":", "")).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            estadoOperacion = result.state.message.ifBlank {
                                "UID staged (validación hardware pendiente)"
                            }
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(estadoOperacion = "Emulate error: ${err.message}") }
                }
            )
        }
    }

    fun emulateFromDump(dump: NfcDumpEntity) {
        _uiState.update {
            it.copy(
                nfcUid = dump.uid,
                nfcDump = dump.rawDump,
                parsedMifare = dump.parsedSectors,
                selectedDumpId = dump.id,
                estadoOperacion = "Dump cargado (emulación vía TEH-Link)."
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
        viewModelScope.launch {
            val normalized = TehLinkIrUtils.normalizeIrCommand(cmd)
            if (detectedProfile.value != FirmwareProfile.XIBALBA) {
                _uiState.update { it.copy(estadoOperacion = "IR requiere firmware Xibalba (TEH-Link).") }
                return@launch
            }
            val match = Regex("""(?i)^ir\s+tx\s+(\w+)\s+([0-9a-f]+)\s+([0-9a-f]+)$""").find(normalized)
            if (match == null) {
                _uiState.update { it.copy(estadoOperacion = "Comando IR inválido: $normalized") }
                return@launch
            }
            connectionManager.tehLinkRunIrSend(
                protocol = match.groupValues[1],
                address = match.groupValues[2],
                command = match.groupValues[3]
            ).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(estadoOperacion = result.state.message.ifBlank { "TX OK: $normalized" })
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(estadoOperacion = "ERROR IR: ${error.message ?: "comando rechazado"}")
                    }
                }
            )
        }
    }

    fun captureIr() {
        viewModelScope.launch {
            if (detectedProfile.value != FirmwareProfile.XIBALBA) {
                _uiState.update { it.copy(estadoOperacion = "Captura IR solo vía TEH-Link (Xibalba).") }
                return@launch
            }
            if (!connectionManager.hasXibalbaCapability("ir_rx") &&
                !connectionManager.hasXibalbaCapability("ir")
            ) {
                _uiState.update { it.copy(estadoOperacion = "IR RX no reportado por el dispositivo.") }
                return@launch
            }
            _uiState.update { it.copy(estadoOperacion = "Capturando IR (10s)...") }
            connectionManager.tehLinkRunIrRx(10).fold(
                onSuccess = { result ->
                    val raw = result.state.ir?.raw.orEmpty()
                    val msg = result.state.message.ifBlank { result.state.state }
                    _uiState.update {
                        it.copy(
                            estadoOperacion = if (raw.isNotBlank()) "IR capturado: $raw" else msg
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(estadoOperacion = "IR RX error: ${err.message}") }
                }
            )
        }
    }

    fun saveIrButton(name: String, command: String) {
        viewModelScope.launch {
            irRepository.save(
                IrButtonEntity(buttonName = name, irPayload = TehLinkIrUtils.normalizeIrCommand(command))
            )
        }
    }
}
