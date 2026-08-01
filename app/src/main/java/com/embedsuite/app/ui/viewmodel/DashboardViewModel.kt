package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.FirmwareRepository
import com.embedsuite.app.connection.OtaUpdateChecker
import com.embedsuite.app.connection.OtaUpdateStatus
import com.embedsuite.app.connection.TehLinkActionState
import com.embedsuite.app.core.SessionStatsTracker
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.data.SignalRepository
import com.embedsuite.app.data.TxHistoryEntity
import com.embedsuite.app.data.TxHistoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

data class DashboardUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val systemInfo: com.embedsuite.app.connection.SystemInfo = com.embedsuite.app.connection.SystemInfo(),
    val lastSignal: CapturedSignalEntity? = null,
    val signalsToday: Int = 0,
    val apsToday: Int = 0,
    val macrosToday: Int = 0,
    val txHistory: List<TxHistoryEntity> = emptyList(),
    val favoriteRf: List<CapturedSignalEntity> = emptyList(),
    val otaStatus: OtaUpdateStatus = OtaUpdateStatus.Unknown,
    val lastActionState: TehLinkActionState? = null
)

class DashboardViewModel(
    private val connectionManager: DeviceConnectionManager,
    private val signalRepository: SignalRepository,
    private val txHistoryRepository: TxHistoryRepository,
    private val sessionStats: SessionStatsTracker,
    private val firmwareRepository: FirmwareRepository,
    private val otaUpdateChecker: OtaUpdateChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                connectionManager.connectionState,
                connectionManager.systemInfo,
                txHistoryRepository.observeRecent(5)
            ) { conn, sys, tx ->
                Triple(conn, sys, tx)
            }.collect { (conn, sys, tx) ->
                _uiState.update {
                    it.copy(connectionState = conn, systemInfo = sys, txHistory = tx)
                }
                if (conn is ConnectionState.Connected && sys.firmware.isNotBlank()) {
                    checkOta(sys.firmware)
                }
            }
        }
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    lastSignal = signalRepository.getLatest(),
                    signalsToday = sessionStats.signalsToday(),
                    apsToday = sessionStats.apsToday(),
                    macrosToday = sessionStats.macrosToday(),
                    favoriteRf = signalRepository.getFavoriteRf(8)
                )
            }
        }
    }

    fun refreshSystemInfo() {
        viewModelScope.launch { connectionManager.refreshSystemInfo() }
    }

    fun openXibalbaPlugin(pluginId: String) {
        viewModelScope.launch {
            connectionManager.tehLinkOpenPlugin(pluginId).onSuccess {
                refreshSystemInfo()
            }
        }
    }

    fun backToXibalbaMenu() {
        viewModelScope.launch {
            connectionManager.tehLinkBackToMenu().onSuccess {
                refreshSystemInfo()
            }
        }
    }

    fun runSubGhzCapture(seconds: Int = 15) {
        viewModelScope.launch {
            connectionManager.tehLinkRunAction(
                pluginId = "subghz_analyzer",
                action = "capture_start",
                params = JSONObject().put("seconds", seconds)
            ).onSuccess { result ->
                _uiState.update { it.copy(lastActionState = result.state) }
            }.onFailure {
                refreshActionState("subghz_analyzer")
            }
        }
    }

    fun runBadUsbDemoScript() {
        viewModelScope.launch {
            connectionManager.tehLinkRunAction(
                pluginId = "badusb",
                action = "run_script",
                params = JSONObject().put("path", "/sdcard/plugins/badusb/demo.txt")
            ).onSuccess { result ->
                _uiState.update { it.copy(lastActionState = result.state) }
            }.onFailure {
                refreshActionState("badusb")
            }
        }
    }

    fun refreshActionState(pluginId: String, action: String? = null) {
        viewModelScope.launch {
            connectionManager.tehLinkGetActionState(pluginId, action).onSuccess { state ->
                _uiState.update { it.copy(lastActionState = state) }
            }
        }
    }

    private fun checkOta(deviceFirmware: String) {
        viewModelScope.launch {
            val status = otaUpdateChecker.check(deviceFirmware)
            _uiState.update { it.copy(otaStatus = status) }
        }
    }
}
