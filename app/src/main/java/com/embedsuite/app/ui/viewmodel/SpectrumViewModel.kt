package com.embedsuite.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.DeviceEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter

data class SpectrumSample(val freqMhz: Double, val rssi: Int, val tMs: Long = System.currentTimeMillis())
data class DecodedFrame(
    val proto: String, val decoded: String, val rssi: Int, val freqMhz: Double,
    val seen: Long, val tMs: Long = System.currentTimeMillis()
)

class SpectrumViewModel(
    app: Application,
    private val connectionManager: DeviceConnectionManager
) : AndroidViewModel(app) {

    private val _samples = MutableStateFlow<List<SpectrumSample>>(emptyList())
    val samples: StateFlow<List<SpectrumSample>> = _samples.asStateFlow()

    private val _frames = MutableStateFlow<List<DecodedFrame>>(emptyList())
    val frames: StateFlow<List<DecodedFrame>> = _frames.asStateFlow()

    private val _specRunning = MutableStateFlow(false)
    val specRunning: StateFlow<Boolean> = _specRunning.asStateFlow()

    private val _decRunning = MutableStateFlow(false)
    val decRunning: StateFlow<Boolean> = _decRunning.asStateFlow()

    private val _toast = MutableSharedFlow<String>()
    val toast: SharedFlow<String> = _toast

    private var fStart = 380.0
    private var fEnd = 450.0
    private var step = 0.025

    init {
        viewModelScope.launch {
            connectionManager.events.collect { ev ->
                when (ev) {
                    is DeviceEvent.RawLine -> parse(ev.line)
                    is DeviceEvent.SubGhzSample -> {
                        val cur = _samples.value.toMutableList()
                        if (cur.size > 4096) cur.subList(0, cur.size - 4096).clear()
                        cur += SpectrumSample(ev.freqMhz, ev.rssi)
                        _samples.value = cur
                    }
                    is DeviceEvent.SubGhzDecodedFrame -> {
                        val fr = DecodedFrame(
                            proto = ev.proto, decoded = ev.decoded, rssi = ev.rssi,
                            freqMhz = ev.freqMhz, seen = 1L
                        )
                        val cur = _frames.value.toMutableList()
                        if (!cur.any { it.decoded == fr.decoded && it.proto == fr.proto }) {
                            if (cur.size > 200) cur.removeFirst()
                            cur += fr
                            _frames.value = cur
                            _toast.tryEmit("Decode ${fr.proto} @${fr.freqMhz}MHz")
                        }
                    }
                    is DeviceEvent.SubGhzSignal,
                    is DeviceEvent.SubGhzSignalSaved,
                    is DeviceEvent.WaveformSample,
                    is DeviceEvent.SystemInfoUpdate,
                    is DeviceEvent.TehLinkNotice,
                    is DeviceEvent.OtaCompleted,
                    is DeviceEvent.BleAdSpamProgress,
                    is DeviceEvent.WifiProbe,
                    is DeviceEvent.MousejackDongle,
                    is DeviceEvent.NfcCloneProgress -> Unit
                }
            }
        }
    }

    private fun parse(line: String) {
        val o = runCatching { JSONObject(line.trim()) }.getOrNull() ?: return
        val event = o.optString("event")
        val d = o.optJSONObject("data") ?: return
        if (event == "SubGhzSample") {
            val f = d.optDouble("freq_mhz")
            val r = d.optInt("rssi")
            val cur = _samples.value.toMutableList()
            if (cur.size > 4096) cur.subList(0, cur.size - 4096).clear()
            cur += SpectrumSample(f, r)
            _samples.value = cur
        } else if (event == "SubGhzDecodedFrame") {
            val fr = DecodedFrame(
                proto = d.optString("proto", "?"),
                decoded = d.optString("decoded", ""),
                rssi = d.optInt("rssi"),
                freqMhz = d.optDouble("freq_mhz"),
                seen = d.optLong("seen", 1L)
            )
            val cur = _frames.value.toMutableList()
            if (!cur.any { it.decoded == fr.decoded && it.proto == fr.proto }) {
                if (cur.size > 200) cur.removeFirst()
                cur += fr
                _frames.value = cur
                viewModelScope.launch { _toast.tryEmit("Decode ${fr.proto} @${fr.freqMhz}MHz") }
            }
        }
    }

    fun startSpec(start: Double = 380.0, end: Double = 450.0, stepMhz: Double = 0.025, pps: Int = 100) {
        fStart = start; fEnd = end; step = stepMhz
        viewModelScope.launch {
            connectionManager.tehLinkRunAction("subghz_tools", "spectrum_start",
                JSONObject().put("f_start", start).put("f_end", end).put("step", stepMhz).put("pps", pps))
                .onSuccess { _specRunning.value = true; _samples.value = emptyList() }
                .onFailure { t -> _toast.tryEmit("spectrum_start: ${t.message ?: "?"}") }
        }
    }

    fun stopSpec() {
        viewModelScope.launch {
            connectionManager.tehLinkRunAction("subghz_tools", "spectrum_stop", JSONObject())
            _specRunning.value = false
        }
    }

    fun startDecode(freq: Double = 433.92, mod: String = "OOK", bitrateKhz: Double = 3.9) {
        viewModelScope.launch {
            connectionManager.tehLinkRunAction("subghz_tools", "rx_decode",
                JSONObject().put("freq", freq).put("mod", mod).put("bitrate_khz", bitrateKhz))
                .onSuccess { _decRunning.value = true }
                .onFailure { t -> _toast.tryEmit("rx_decode: ${t.message ?: "?"}") }
        }
    }

    fun stopDecode() {
        viewModelScope.launch {
            connectionManager.tehLinkRunAction("subghz_tools", "rx_decode_stop", JSONObject())
            _decRunning.value = false
        }
    }

    fun exportCsv(uri: Uri) {
        try {
            val app = getApplication<Application>()
            app.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { w ->
                    w.write("timestamp_ms,freq_mhz,rssi_dbm\n")
                    _samples.value.forEach { s ->
                        w.append("${s.tMs},${s.freqMhz},${s.rssi}\n")
                    }
                }
            }
            viewModelScope.launch { _toast.tryEmit("CSV export OK") }
        } catch (t: Throwable) {
            viewModelScope.launch { _toast.tryEmit("CSV export fail: ${t.message}") }
        }
    }

    fun clearFrames() { _frames.value = emptyList() }

    companion object {
        fun factory(app: Application, cm: DeviceConnectionManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SpectrumViewModel(app, cm) as T
            }
    }
}
