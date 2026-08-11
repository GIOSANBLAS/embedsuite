package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.JammerMode
import com.embedsuite.app.connection.JammerParams
import com.embedsuite.app.connection.XibalbaAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * JammerViewModel — control del jammer RF headless (TEH-Link rf_jammer).
 *
 * El firmware aplica un cutoff de seguridad (máx. 300 s) y emite
 * `rf.jammer.stopped` al cortar; el ViewModel escucha ese evento para que la
 * UI refleje el estado real aunque el corte venga del dispositivo.
 */
class JammerViewModel(
    private val connectionManager: DeviceConnectionManager,
    private val xibalba: XibalbaAdapter
) : ViewModel() {

    data class JammerUiState(
        val running: Boolean = false,
        val freqMhz: Double = 433.92,
        val powerDbm: Int = 12,
        val mode: JammerMode = JammerMode.CONTINUOUS,
        val burstIntervalMs: Int = 100,
        val maxSeconds: Int = 30,
        val elapsedSeconds: Int = 0,
        val statusMessage: String = ""
    )

    private val _uiState = MutableStateFlow(JammerUiState())
    val uiState: StateFlow<JammerUiState> = _uiState.asStateFlow()

    val connectionState = connectionManager.connectionState
    val detectedProfile = connectionManager.detectedProfile

    init {
        viewModelScope.launch {
            xibalba.observeJammerState().collect { event ->
                if (!event.running) {
                    _uiState.update {
                        it.copy(
                            running = false,
                            elapsedSeconds = 0,
                            statusMessage = if (it.running) "Jammer detenido (cutoff o stop)" else it.statusMessage
                        )
                    }
                }
            }
        }
    }

    fun setFreq(mhz: Double) = _uiState.update { it.copy(freqMhz = mhz.coerceIn(300.0, 928.0)) }
    fun setPower(dbm: Int) = _uiState.update { it.copy(powerDbm = dbm.coerceIn(-30, 12)) }
    fun setMode(mode: JammerMode) = _uiState.update { it.copy(mode = mode) }
    fun setBurstInterval(ms: Int) = _uiState.update { it.copy(burstIntervalMs = ms.coerceIn(1, 5000)) }
    fun setMaxSeconds(sec: Int) = _uiState.update { it.copy(maxSeconds = sec.coerceIn(1, 300)) }

    fun startJammer() {
        val st = _uiState.value
        val params = JammerParams(
            freqMhz = st.freqMhz,
            powerDbm = st.powerDbm,
            mode = st.mode,
            burstIntervalMs = st.burstIntervalMs,
            maxSeconds = st.maxSeconds
        )
        if (!params.isValid()) {
            _uiState.update { it.copy(statusMessage = "Parámetros inválidos (300–928 MHz)") }
            return
        }
        viewModelScope.launch {
            xibalba.startJammer(params).fold(
                onSuccess = {
                    _uiState.update { it.copy(running = true, elapsedSeconds = 0, statusMessage = "") }
                    tickElapsed()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(statusMessage = err.message ?: "jammer_start_failed") }
                }
            )
        }
    }

    fun stopJammer() {
        viewModelScope.launch {
            xibalba.stopJammer()
            _uiState.update { it.copy(running = false, elapsedSeconds = 0) }
        }
    }

    private fun tickElapsed() {
        viewModelScope.launch {
            while (isActive && _uiState.value.running) {
                delay(1000)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    override fun onCleared() {
        // Igual que el scanner: el stop debe llegar al firmware aunque la
        // pantalla se destruya (el jammer tiene cutoff propio como red de seguridad).
        if (_uiState.value.running) {
            @Suppress("OPT_IN_USAGE")
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                xibalba.stopJammer()
            }
        }
    }
}
