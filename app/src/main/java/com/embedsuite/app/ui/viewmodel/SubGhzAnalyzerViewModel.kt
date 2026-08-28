package com.embedsuite.app.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.core.bruce.BruceCliCaptureParser
import com.embedsuite.app.core.orchestrator.AutoDiscoveryManager
import com.embedsuite.app.core.orchestrator.CapturedSignal
import com.embedsuite.app.core.orchestrator.IntentOrchestrator
import com.embedsuite.app.core.orchestrator.OrchestrationResult
import com.embedsuite.app.core.orchestrator.SubGhzIntent
import com.embedsuite.app.data.SignalRepository
import com.embedsuite.app.rf.RfProtocolDecoder
import com.embedsuite.app.scan.LocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SubGhzAnalyzerUiState(
    val freqMhz: Float = 433.92f,
    val durationSec: Int = 15,
    val busy: Boolean = false,
    val lastResult: OrchestrationResult? = null,
    val transportHint: String = "",
    val waveform: Bitmap? = null,
    val capturedSignal: CapturedSignal? = null,
    val trimThresholdUs: Long = 5_000L
)

class SubGhzAnalyzerViewModel(
    private val orchestrator: IntentOrchestrator,
    private val autoDiscovery: AutoDiscoveryManager,
    private val connectionManager: DeviceConnectionManager,
    private val signalRepository: SignalRepository,
    private val locationTracker: LocationTracker
) : ViewModel() {

    private val _state = MutableStateFlow(SubGhzAnalyzerUiState())
    val state: StateFlow<SubGhzAnalyzerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val hint = autoDiscovery.hintForFileUpload()
            _state.value = _state.value.copy(transportHint = hint.message)
        }
    }

    fun setFreqMhz(mhz: Float) {
        _state.value = _state.value.copy(freqMhz = mhz.coerceIn(300f, 928f))
    }

    fun setDuration(sec: Int) {
        _state.value = _state.value.copy(durationSec = sec.coerceIn(1, 120))
    }

    fun setTrimThresholdUs(v: Long) {
        _state.value = _state.value.copy(trimThresholdUs = v.coerceIn(100L, 50_000L))
    }

    fun replayEdited() {
        viewModelScope.launch {
            val signal = _state.value.capturedSignal ?: return@launch
            val trimmed = signal.trimSilence(_state.value.trimThresholdUs)
            _state.value = _state.value.copy(busy = true, lastResult = null)
            autoDiscovery.ensureCliReady().onFailure { err ->
                _state.value = _state.value.copy(
                    busy = false,
                    lastResult = OrchestrationResult(false, err.message ?: "Sin CLI")
                )
                return@launch
            }
            val intent = SubGhzIntent.Replay(trimmed.buildSubContent())
            val result = orchestrator.execute(intent)
            _state.value = _state.value.copy(busy = false, lastResult = result)
        }
    }

    fun capture() {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, lastResult = null, waveform = null, capturedSignal = null)
            autoDiscovery.ensureCliReady().onFailure { err ->
                _state.value = _state.value.copy(
                    busy = false,
                    lastResult = OrchestrationResult(false, err.message ?: "Sin CLI")
                )
                return@launch
            }
            connectionManager.setSubGhzFrequency("%.2f".format(_state.value.freqMhz))
            val hz = (_state.value.freqMhz * 1_000_000f).toLong()
            val intent = SubGhzIntent.Capture(hz, _state.value.durationSec, freqMhz = _state.value.freqMhz.toDouble())
            val result = orchestrator.execute(intent)

            val signal = result.artifact as? CapturedSignal
            val waveform = signal?.trimSilence(_state.value.trimThresholdUs)?.toWaveformBitmap()

            val responseText = when {
                result.cliResponse.isNotBlank() -> result.cliResponse
                signal != null -> signal.subContent
                else -> ""
            }
            if (result.success && responseText.isNotBlank()) {
                runCatching {
                    val entry = BruceCliCaptureParser.parseSubGhzResponse(
                        responseText,
                        _state.value.freqMhz.toDouble()
                    ) ?: return@runCatching
                    val (lat, lng) = locationTracker.currentLatLng()
                    val decoded = RfProtocolDecoder.decode(responseText)
                    signalRepository.saveSubGhzSignal(entry, lat, lng, decoded)
                }
            }
            _state.value = _state.value.copy(
                busy = false,
                lastResult = result,
                waveform = waveform,
                capturedSignal = signal
            )
        }
    }
}
