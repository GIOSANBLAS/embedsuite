package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.data.BleProfileEntity
import com.embedsuite.app.data.BleProfileRepository
import com.embedsuite.app.scan.BleGattClient
import com.embedsuite.app.scan.GattCharacteristicInfo
import com.embedsuite.app.scan.GattServiceInfo
import com.embedsuite.app.scan.WirelessDevice
import com.embedsuite.app.scan.WirelessScanner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WirelessUiState(
    val filtro: String = "TODOS",
    val warDriving: Boolean = false,
    val scanStatus: String = "Listo para escanear.",
    val gattConnecting: String? = null,
    val gattServices: List<GattServiceInfo> = emptyList(),
    val gattReadResult: String = "",
    val expandedGattAddress: String? = null,
    val writeTarget: Pair<String, String>? = null,
    val writeHexInput: String = "",
    val subscribedChar: String? = null
)

class WirelessViewModel(
    private val wirelessScanner: WirelessScanner,
    private val bleProfileRepository: BleProfileRepository,
    private val bleGattClient: BleGattClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(WirelessUiState())
    val uiState: StateFlow<WirelessUiState> = _uiState.asStateFlow()

    val devices: StateFlow<List<WirelessDevice>> = wirelessScanner.devices
    val isBleScanning: StateFlow<Boolean> = wirelessScanner.isScanning

    init {
        viewModelScope.launch {
            bleGattClient.notifications.collect { notification ->
                val hex = notification.data.joinToString(" ") { "%02X".format(it) }
                _uiState.update {
                    it.copy(gattReadResult = "NOTIFY ${notification.characteristicUuid.take(8)}… HEX: $hex")
                }
            }
        }
    }

    fun setFiltro(f: String) { _uiState.update { it.copy(filtro = f) } }
    fun setWarDriving(v: Boolean) { _uiState.update { it.copy(warDriving = v) } }

    fun scanAll(onSaveDevice: suspend (WirelessDevice) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(scanStatus = "Escaneando WiFi + BLE...") }
            wirelessScanner.scanAll().fold(
                onSuccess = { found ->
                    val bleCount = devices.value.count { it.type == "BLE" }
                    _uiState.update { it.copy(scanStatus = "${found.size} WiFi + $bleCount BLE detectados.") }
                    if (_uiState.value.warDriving) devices.value.forEach { onSaveDevice(it) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(scanStatus = e.message ?: "Error de escaneo.") }
                }
            )
        }
    }

    fun saveBleProfile(device: WirelessDevice) {
        viewModelScope.launch {
            bleProfileRepository.save(
                BleProfileEntity(
                    name = device.name,
                    address = device.mac,
                    services = device.gattDetail.lines().firstOrNull() ?: "",
                    notes = device.detail
                )
            )
        }
    }

    fun connectGatt(address: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(gattConnecting = address, gattServices = emptyList(), gattReadResult = "") }
            bleGattClient.connect(address).fold(
                onSuccess = { services ->
                    _uiState.update {
                        it.copy(gattConnecting = null, gattServices = services, expandedGattAddress = address)
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(gattConnecting = null, gattReadResult = "Error: ${e.message}") }
                }
            )
        }
    }

    fun readCharacteristic(serviceUuid: String, charUuid: String) {
        viewModelScope.launch {
            bleGattClient.readCharacteristic(serviceUuid, charUuid).fold(
                onSuccess = { bytes ->
                    val hex = bytes.joinToString(" ") { "%02X".format(it) }
                    val text = bytes.toString(Charsets.UTF_8).takeIf { it.all { c -> c.isLetterOrDigit() || c.isWhitespace() } } ?: ""
                    _uiState.update { it.copy(gattReadResult = "HEX: $hex${if (text.isNotBlank()) "\nTXT: $text" else ""}") }
                },
                onFailure = { e -> _uiState.update { it.copy(gattReadResult = "Read error: ${e.message}") } }
            )
        }
    }

    fun writeCharacteristic(serviceUuid: String, charUuid: String, hex: String) {
        viewModelScope.launch {
            val bytes = hex.split(Regex("\\s+")).mapNotNull { it.toIntOrNull(16)?.toByte() }.toByteArray()
            bleGattClient.writeCharacteristic(serviceUuid, charUuid, bytes).fold(
                onSuccess = { _uiState.update { it.copy(gattReadResult = "Write OK") } },
                onFailure = { e -> _uiState.update { it.copy(gattReadResult = "Write error: ${e.message}") } }
            )
        }
    }

    fun subscribeCharacteristic(serviceUuid: String, charUuid: String) {
        viewModelScope.launch {
            bleGattClient.subscribeCharacteristic(serviceUuid, charUuid).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            subscribedChar = charUuid,
                            gattReadResult = "Subscribe OK — escuchando $charUuid"
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(gattReadResult = "Subscribe error: ${e.message}") }
                }
            )
        }
    }

    fun showWriteDialog(serviceUuid: String, charUuid: String) {
        _uiState.update { it.copy(writeTarget = serviceUuid to charUuid, writeHexInput = "00 FF") }
    }

    fun setWriteHex(hex: String) {
        _uiState.update { it.copy(writeHexInput = hex) }
    }

    fun dismissWriteDialog() {
        _uiState.update { it.copy(writeTarget = null) }
    }

    fun confirmWrite() {
        val target = _uiState.value.writeTarget ?: return
        writeCharacteristic(target.first, target.second, _uiState.value.writeHexInput)
        dismissWriteDialog()
    }

    fun disconnectGatt() {
        bleGattClient.disconnect()
        _uiState.update { it.copy(gattServices = emptyList(), expandedGattAddress = null, gattReadResult = "") }
    }

    fun toggleGattExpand(address: String) {
        _uiState.update {
            it.copy(expandedGattAddress = if (it.expandedGattAddress == address) null else address)
        }
    }

    override fun onCleared() {
        super.onCleared()
        wirelessScanner.stopBleScan()
        bleGattClient.disconnect()
    }
}
