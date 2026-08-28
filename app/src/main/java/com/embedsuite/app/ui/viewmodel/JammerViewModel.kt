package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.DeviceEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class JammerUiState(
    val active: Boolean = false,
    val frequencyMhz: Float = 433.92f,
    val power: Int = 10,
    val mode: String = "continuous",
    val maxSeconds: Int = 30,
    val lastStatus: String = ""
)

class JammerViewModel(
    private val connectionManager: DeviceConnectionManager
) : ViewModel() {

    private val _ui = MutableStateFlow(JammerUiState())
    val ui: StateFlow<JammerUiState> = _ui.asStateFlow()

    private val _toast = MutableSharedFlow<String>()
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    init {
        viewModelScope.launch {
            connectionManager.events.collect { ev ->
                when (ev) {
                    is DeviceEvent.RfJammerStopped -> {
                        _ui.value = _ui.value.copy(
                            active = false,
                            lastStatus = "stopped: ${ev.reason} (${ev.elapsedMs} ms)"
                        )
                        _toast.emit("Jammer detenido: ${ev.reason}")
                    }
                    else -> Unit
                }
            }
        }
    }

    fun setFrequency(mhz: Float) {
        _ui.value = _ui.value.copy(frequencyMhz = mhz)
    }

    fun setPower(power: Int) {
        _ui.value = _ui.value.copy(power = power.coerceIn(1, 12))
    }

    fun setMode(mode: String) {
        _ui.value = _ui.value.copy(mode = mode)
    }

    fun setMaxSeconds(sec: Int) {
        _ui.value = _ui.value.copy(maxSeconds = sec.coerceIn(1, 30))
    }

    fun start() {
        val s = _ui.value
        viewModelScope.launch {
            connectionManager.rfJammerStart(
                freqMhz = s.frequencyMhz.toDouble(),
                power = s.power,
                mode = s.mode,
                burstInterval = if (s.mode == "burst") 100 else null,
                maxSeconds = s.maxSeconds
            ).onSuccess {
                _ui.value = s.copy(active = true, lastStatus = "jamming")
                _toast.emit("Jammer ON ${s.frequencyMhz} MHz")
            }.onFailure { t ->
                _toast.emit("jammer: ${t.message ?: "?"}")
            }
        }
    }

    fun stop() {
        viewModelScope.launch {
            connectionManager.rfJammerStop()
                .onSuccess {
                    _ui.value = _ui.value.copy(active = false, lastStatus = "stopping")
                }
                .onFailure { t -> _toast.emit("stop: ${t.message ?: "?"}") }
        }
    }

    companion object {
        fun factory(cm: DeviceConnectionManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    JammerViewModel(cm) as T
            }
    }
}
