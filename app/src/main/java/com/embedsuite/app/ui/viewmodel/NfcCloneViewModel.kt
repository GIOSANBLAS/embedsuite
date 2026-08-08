package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.DeviceEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

data class NfcCloneUiState(
    val lastUid: String = "",
    val sectorsRead: Int = 0,
    val blocksWritten: Int = 0,
    val dumpHex: String = "",
    val step: String = "idle",
    val error: String? = null
)

class NfcCloneViewModel(
    private val connectionManager: DeviceConnectionManager
) : ViewModel() {

    private val _ui = MutableStateFlow(NfcCloneUiState())
    val ui: StateFlow<NfcCloneUiState> = _ui.asStateFlow()

    private val _toast = MutableSharedFlow<String>()
    val toast: SharedFlow<String> = _toast

    init {
        viewModelScope.launch {
            connectionManager.events.collect { ev ->
                when (ev) {
                    is DeviceEvent.RawLine -> parse(ev.line)
                    is DeviceEvent.NfcCloneProgress -> {
                        _ui.update { it.copy(step = ev.step, sectorsRead = ev.sectors, lastUid = ev.uid, dumpHex = ev.dumpHex) }
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
                    is DeviceEvent.SubGhzSample,
                    is DeviceEvent.SubGhzDecodedFrame -> Unit
                }
            }
        }
    }

    private fun parse(line: String) {
        val o = runCatching { JSONObject(line.trim()) }.getOrNull() ?: return
        val e = o.optString("event")
        val d = o.optJSONObject("data") ?: return
        if (e == "NfcCloneProgress") {
            val s = d.optString("step", "idle")
            _ui.update { it.copy(step = s) }
        }
    }

    fun readMifare(keysCsv: String) {
        viewModelScope.launch {
            _ui.update { it.copy(step = "reading", error = null) }
            val res = connectionManager.tehLinkRunAction("nfc_clone", "read_mifare",
                JSONObject().put("keys_csv", keysCsv))
            res.onSuccess { data ->
                val sectors = data.rawResponse?.getInt("sectors") ?: 0
                val uid = data.rawResponse?.getString("uid") ?: ""
                val hex = data.rawResponse?.getString("dump_hex") ?: ""
                _ui.update { it.copy(step = "read_done", sectorsRead = sectors, lastUid = uid, dumpHex = hex) }
                _toast.tryEmit("Read OK: $sectors/16 sectors (UID $uid)")
            }
            res.onFailure { t ->
                _ui.update { it.copy(step = "error", error = t.message ?: "Read fail") }
                _toast.tryEmit("Read fail: ${t.message ?: "?"}")
            }
        }
    }

    fun writeMifare(dumpHex: String, forceUid: Boolean = false) {
        viewModelScope.launch {
            _ui.update { it.copy(step = "writing", error = null) }
            val res = connectionManager.tehLinkRunAction("nfc_clone", "write_mifare",
                JSONObject().put("dump_hex", dumpHex).put("force_uid_write", if (forceUid) 1 else 0))
            res.onSuccess { data ->
                val bw = data.rawResponse?.getInt("blocks_written") ?: 0
                _ui.update { it.copy(step = "write_done", blocksWritten = bw) }
                _toast.tryEmit("Write OK: $bw blocks written.")
            }
            res.onFailure { t ->
                _ui.update { it.copy(step = "error", error = t.message ?: "Write fail") }
                _toast.tryEmit("Write fail: ${t.message ?: "?"}")
            }
        }
    }

    fun writeNtagUrl(url: String) {
        viewModelScope.launch {
            _ui.update { it.copy(step = "writing_url") }
            val r = connectionManager.tehLinkRunAction("nfc_clone", "write_ntag_url", JSONObject().put("url", url))
            r.onSuccess { _toast.tryEmit("NDEF URL written OK"); _ui.update { it.copy(step = "write_done") } }
            r.onFailure { t ->
                _ui.update { it.copy(step = "error", error = t.message ?: "fail") }
                _toast.tryEmit("fail: ${t.message ?: "?"}")
            }
        }
    }

    fun writeNtagWifi(ssid: String, pass: String, auth: Int) {
        viewModelScope.launch {
            _ui.update { it.copy(step = "writing_wifi") }
            val r = connectionManager.tehLinkRunAction("nfc_clone", "write_ntag_wifi",
                JSONObject().put("ssid", ssid).put("password", pass).put("auth", auth))
            r.onSuccess { _toast.tryEmit("NDEF WSC written OK"); _ui.update { it.copy(step = "write_done") } }
            r.onFailure { t ->
                _ui.update { it.copy(step = "error", error = t.message ?: "fail") }
                _toast.tryEmit("fail: ${t.message ?: "?"}")
            }
        }
    }

    companion object {
        fun factory(cm: DeviceConnectionManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = NfcCloneViewModel(cm) as T
            }
    }
}
