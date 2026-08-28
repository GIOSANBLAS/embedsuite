package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.core.bruce.BruceLimits
import com.embedsuite.app.core.orchestrator.DirectCliIntent
import com.embedsuite.app.core.orchestrator.IntentOrchestrator
import com.embedsuite.app.core.orchestrator.SpamIntent
import com.embedsuite.app.core.connection.TransportTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SpamGeneratorUiState(
    val ssid: String = "Free_WiFi",
    val channel: Int = 6,
    val note: String = "",
    val bleProfile: SpamIntent.BleSpamProfile = SpamIntent.BleSpamProfile.GENERIC,
    val wifiProfile: SpamIntent.WifiSpamProfile = SpamIntent.WifiSpamProfile.BEACON,
    val packetCount: Int = 50,
    val exportedPath: String? = null,
    val disclaimer: String = BruceLimits.NO_CLI,
    val status: String = ""
)

/** Generador local — Evil Portal / BLE spam no tienen CLI Bruce remota. */
class SpamGeneratorViewModel(
    private val orchestrator: IntentOrchestrator
) : ViewModel() {

    private val _state = MutableStateFlow(SpamGeneratorUiState())
    val state: StateFlow<SpamGeneratorUiState> = _state.asStateFlow()

    fun setSsid(v: String) { _state.value = _state.value.copy(ssid = v) }
    fun setChannel(v: Int) { _state.value = _state.value.copy(channel = v.coerceIn(1, 13)) }
    fun setNote(v: String) { _state.value = _state.value.copy(note = v) }
    fun setBleProfile(p: SpamIntent.BleSpamProfile) { _state.value = _state.value.copy(bleProfile = p) }
    fun setWifiProfile(p: SpamIntent.WifiSpamProfile) { _state.value = _state.value.copy(wifiProfile = p) }

    fun setPacketCount(v: Int) { _state.value = _state.value.copy(packetCount = v.coerceIn(1, 200)) }

    fun exportLocalConfig() {
        viewModelScope.launch {
            val config = SpamIntent.Config(
                bleProfile = _state.value.bleProfile,
                wifiProfile = _state.value.wifiProfile,
                ssid = _state.value.ssid,
                channel = _state.value.channel,
                note = _state.value.note
            )
            val result = orchestrator.execute(SpamIntent.buildLocalExport(config))
            _state.value = _state.value.copy(exportedPath = result.localFile?.absolutePath)
        }
    }

    fun openBleSpamOnDevice() {
        viewModelScope.launch {
            val result = orchestrator.executeDirectCli(TransportTask.CLI_TRIGGER, "loader open BLE")
            _state.value = _state.value.copy(
                status = if (result.success) "Abre BLE Spam en el T-Embed" else result.message
            )
        }
    }

    fun openWifiMenuOnDevice() {
        viewModelScope.launch {
            val result = orchestrator.executeDirectCli(TransportTask.CLI_TRIGGER, "loader open WiFi")
            _state.value = _state.value.copy(status = result.message)
        }
    }
}
