package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.connection.RfScanParams
import com.embedsuite.app.connection.XibalbaAdapter
import com.embedsuite.app.scan.HybridLocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * RfScannerViewModel — escáner de espectro CC1101 headless (TEH-Link) con
 * cruce GPS del teléfono (HybridLocationProvider) para el mapa de calor RF.
 */
class RfScannerViewModel(
    private val connectionManager: DeviceConnectionManager,
    private val xibalba: XibalbaAdapter,
    private val hybridLocation: HybridLocationProvider
) : ViewModel() {

    enum class ViewMode { MAP, CHART }

    data class ScannerUiState(
        val scanning: Boolean = false,
        val viewMode: ViewMode = ViewMode.CHART,
        val freqStartMhz: Double = 433.0,
        val freqEndMhz: Double = 435.0,
        val stepMhz: Double = 0.1,
        val rssiThreshold: Int = -90,
        val sampleCount: Int = 0,
        val sweepCount: Int = 0,
        val maxRssi: Int = -127,
        val maxFreqMhz: Double = 0.0,
        val statusMessage: String = "",
        val timeSynced: Boolean = false
    )

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    /** Muestras RSSI crudas para la gráfica (ring buffer de las últimas N). */
    private val _samples = MutableStateFlow<List<Pair<Double, Int>>>(emptyList())
    val samples: StateFlow<List<Pair<Double, Int>>> = _samples.asStateFlow()

    /** Muestras geo-posicionadas para el mapa de calor. */
    private val _geoSamples = MutableStateFlow<List<HybridLocationProvider.GeoRfSample>>(emptyList())
    val geoSamples: StateFlow<List<HybridLocationProvider.GeoRfSample>> = _geoSamples.asStateFlow()

    val connectionState = connectionManager.connectionState
    val detectedProfile = connectionManager.detectedProfile

    init {
        viewModelScope.launch {
            xibalba.observeRfSamples().collect { sample ->
                _samples.update { list -> (list + (sample.freqMhz to sample.rssi)).takeLast(MAX_CHART_SAMPLES) }
                _uiState.update { st ->
                    val newMax = sample.rssi > st.maxRssi
                    st.copy(
                        sampleCount = st.sampleCount + 1,
                        maxRssi = if (newMax) sample.rssi else st.maxRssi,
                        maxFreqMhz = if (newMax) sample.freqMhz else st.maxFreqMhz
                    )
                }
            }
        }
        viewModelScope.launch {
            xibalba.observeScanState().collect { event ->
                _uiState.update {
                    it.copy(
                        scanning = event.running,
                        statusMessage = if (event.detail.isNotBlank()) event.detail else ""
                    )
                }
                if (!event.running) hybridLocation.stopHybridTracking()
            }
        }
        viewModelScope.launch {
            hybridLocation.geoSamples.collect { geo ->
                _geoSamples.update { list -> (list + geo).takeLast(MAX_GEO_SAMPLES) }
            }
        }
    }

    fun setViewMode(mode: ViewMode) = _uiState.update { it.copy(viewMode = mode) }

    fun updateParams(
        freqStart: Double? = null,
        freqEnd: Double? = null,
        step: Double? = null,
        threshold: Int? = null
    ) {
        _uiState.update {
            it.copy(
                freqStartMhz = freqStart ?: it.freqStartMhz,
                freqEndMhz = freqEnd ?: it.freqEndMhz,
                stepMhz = step ?: it.stepMhz,
                rssiThreshold = threshold ?: it.rssiThreshold
            )
        }
    }

    fun startScanning() {
        val st = _uiState.value
        val params = RfScanParams(
            freqStartMhz = st.freqStartMhz,
            freqEndMhz = st.freqEndMhz,
            stepMhz = st.stepMhz,
            rssiThreshold = st.rssiThreshold
        )
        if (!params.isValid()) {
            _uiState.update { it.copy(statusMessage = "Rango de frecuencia inválido (300–928 MHz)") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "") }
            xibalba.startScan(params).fold(
                onSuccess = {
                    hybridLocation.startHybridTracking()
                    _uiState.update {
                        it.copy(
                            scanning = true,
                            sampleCount = 0,
                            sweepCount = 0,
                            maxRssi = -127,
                            timeSynced = hybridLocation.lastTimeOffsetMs != 0L
                        )
                    }
                    _samples.value = emptyList()
                    _geoSamples.value = emptyList()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(statusMessage = err.message ?: "scan_start_failed") }
                }
            )
        }
    }

    fun stopScanning() {
        viewModelScope.launch {
            xibalba.stopScan()
            hybridLocation.stopHybridTracking()
            _uiState.update { it.copy(scanning = false) }
        }
    }

    /** Exporta las muestras geo a CSV (freq,rssi,lat,lon,ts). */
    fun exportCsv(): String {
        val sb = StringBuilder("freq_mhz,rssi_dbm,lat,lon,ts_ms\n")
        _geoSamples.value.forEach { s ->
            sb.append("${s.freqMhz},${s.rssi},${s.latitude ?: ""},${s.longitude ?: ""},${s.timestampMs}\n")
        }
        return sb.toString()
    }

    override fun onCleared() {
        // viewModelScope ya está cancelado aquí: el stop del barrido debe
        // sobrevivir al ViewModel para no dejar el CC1101 ocupado.
        if (_uiState.value.scanning) {
            @Suppress("OPT_IN_USAGE")
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                xibalba.stopScan()
            }
        }
        hybridLocation.stopHybridTracking()
    }

    companion object {
        private const val MAX_CHART_SAMPLES = 2000
        private const val MAX_GEO_SAMPLES = 5000
    }
}
