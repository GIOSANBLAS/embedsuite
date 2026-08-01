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
import kotlinx.coroutines.delay
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
    val lastActionState: TehLinkActionState? = null,
    val tehLinkNotice: String? = null
)

class DashboardViewModel(
    private val connectionManager: DeviceConnectionManager,
    private val signalRepository: SignalRepository,
    private val txHistoryRepository: TxHistoryRepository,
    private val sessionStats: SessionStatsTracker,
    private val firmwareRepository: FirmwareRepository,
    private val otaUpdateChecker: OtaUpdateChecker,
    private val locationTracker: com.embedsuite.app.scan.LocationTracker? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var lastOtaCheckMs = 0L
    private var lastOtaFirmware = ""
    private var wardrivingGpsJob: kotlinx.coroutines.Job? = null

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
        viewModelScope.launch {
            connectionManager.events.collect { event ->
                if (event is com.embedsuite.app.connection.DeviceEvent.TehLinkNotice) {
                    _uiState.update { it.copy(tehLinkNotice = event.message) }
                }
            }
        }
    }

    fun clearTehLinkNotice() {
        _uiState.update { it.copy(tehLinkNotice = null) }
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
            ).onFailure { err ->
                _uiState.update {
                    it.copy(
                        tehLinkNotice = err.message ?: "BadUSB demo bloqueado por política de seguridad."
                    )
                }
                refreshActionState("badusb")
            }
        }
    }

    fun runWifiScan(seconds: Int = 10) {
        viewModelScope.launch {
            connectionManager.tehLinkRunWifiScan(seconds).onSuccess { result ->
                _uiState.update { it.copy(lastActionState = result.state) }
            }.onFailure {
                refreshActionState("wifi_toolkit")
            }
        }
    }

    fun runWardrivingStart() {
        viewModelScope.launch {
            locationTracker?.startTracking()
            connectionManager.tehLinkRunWardrivingStart().onSuccess { result ->
                _uiState.update { it.copy(lastActionState = result.state) }
                startWardrivingGpsUpdates()
                refreshActionState("wardriving")
            }.onFailure {
                refreshActionState("wardriving")
            }
        }
    }

    fun runWardrivingStop() {
        viewModelScope.launch {
            wardrivingGpsJob?.cancel()
            wardrivingGpsJob = null
            connectionManager.tehLinkRunWardrivingStop().onSuccess { result ->
                _uiState.update { it.copy(lastActionState = result.state) }
                refreshActionState("wardriving")
            }.onFailure {
                refreshActionState("wardriving")
            }
        }
    }

    private fun startWardrivingGpsUpdates() {
        wardrivingGpsJob?.cancel()
        wardrivingGpsJob = viewModelScope.launch {
            while (true) {
                delay(5_000)
                connectionManager.tehLinkRunWardrivingGpsUpdate()
            }
        }
    }

    fun runBleScan(seconds: Int = 10) {
        viewModelScope.launch {
            connectionManager.tehLinkRunBleScan(seconds).onSuccess { result ->
                _uiState.update { it.copy(lastActionState = result.state) }
            }.onFailure {
                refreshActionState("ble_toolkit")
            }
        }
    }

    fun runCryptoHashTest() {
        viewModelScope.launch {
            connectionManager.tehLinkRunCryptoHash("EmbedSuite", "sha256").onSuccess { result ->
                _uiState.update { it.copy(lastActionState = result.state) }
            }.onFailure {
                refreshActionState("crypto_toolkit", "hash")
            }
        }
    }

    fun runGenPassword(length: Int = 16) {
        viewModelScope.launch {
            connectionManager.tehLinkRunGenPassword(length).onSuccess { result ->
                _uiState.update { it.copy(lastActionState = result.state) }
            }.onFailure {
                refreshActionState("crypto_toolkit", "gen_password")
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
        val now = System.currentTimeMillis()
        if (now - lastOtaCheckMs < OTA_CHECK_INTERVAL_MS && deviceFirmware == lastOtaFirmware) {
            return
        }
        lastOtaCheckMs = now
        lastOtaFirmware = deviceFirmware
        viewModelScope.launch {
            val profile = connectionManager.detectedProfile.value
            val status = otaUpdateChecker.check(deviceFirmware, profile)
            _uiState.update { it.copy(otaStatus = status) }
        }
    }

    companion object {
        private const val OTA_CHECK_INTERVAL_MS = 60_000L
    }
}
