package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.DeviceEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

data class WifiProbeEntry(
    val ssid: String,
    val mac: String,
    val rssi: Int,
    val channel: Int,
    val vendor: String,
    val count: Int = 1,
    val lastSeenMs: Long = System.currentTimeMillis()
)

class ProbeSnifferViewModel(
    private val connectionManager: DeviceConnectionManager
) : ViewModel() {

    private val _probes = MutableStateFlow<List<WifiProbeEntry>>(emptyList())
    val probes: StateFlow<List<WifiProbeEntry>> = _probes.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _toast = MutableSharedFlow<String>()
    val toast: SharedFlow<String> = _toast

    private val oui by lazy {
        mapOf(
            "00:1A:2B" to "Intel",
            "00:1B:44" to "Apple",
            "00:1C:42" to "Apple",
            "00:1E:52" to "Broadcom",
            "00:1F:A9" to "Apple",
            "00:21:5D" to "Apple",
            "00:23:12" to "Apple",
            "00:24:36" to "Apple",
            "00:25:00" to "Apple",
            "00:25:4B" to "Apple",
            "00:26:08" to "Apple",
            "00:26:4A" to "Apple",
            "00:26:B0" to "Apple",
            "00:30:65" to "Apple",
            "08:00:28" to "Raspberry",
            "08:00:27" to "Raspberry",
            "10:40:F3" to "Espressif",
            "18:5E:0F" to "Apple",
            "20:AA:4B" to "Espressif",
            "24:0A:C4" to "Espressif",
            "24:18:1D" to "Espressif",
            "24:62:AB" to "Espressif",
            "24:6F:28" to "Espressif",
            "28:6C:07" to "Espressif",
            "28:87:BA" to "Espressif",
            "2C:3A:E8" to "Espressif",
            "2C:F4:32" to "Espressif",
            "30:AE:A4" to "Espressif",
            "34:AB:95" to "Espressif",
            "3C:71:BF" to "Espressif",
            "40:F5:20" to "Espressif",
            "44:67:55" to "Espressif",
            "50:02:91" to "Espressif",
            "54:10:EC" to "Qualcomm",
            "5C:CF:7F" to "Espressif",
            "60:01:94" to "Espressif",
            "68:3E:34" to "Apple",
            "80:35:C1" to "Espressif",
            "84:0D:8E" to "Espressif",
            "84:CC:A8" to "Espressif",
            "8C:85:90" to "Apple",
            "90:97:D5" to "Espressif",
            "94:3C:C6" to "Espressif",
            "9C:9C:1F" to "Espressif",
            "A0:20:A6" to "Espressif",
            "A4:CF:12" to "Espressif",
            "A8:03:2A" to "Apple",
            "AC:67:B2" to "Espressif",
            "B4:E6:2D" to "Espressif",
            "C0:4F:73" to "Apple",
            "C4:4F:33" to "Espressif",
            "C4:DE:E2" to "Apple",
            "CC:50:E3" to "Espressif",
            "CD:43:1B" to "Apple",
            "D4:8A:FC" to "Espressif",
            "DC:4F:22" to "Espressif",
            "DC:9F:DB" to "Apple",
            "E0:98:06" to "Espressif",
            "EC:1F:72" to "Apple",
            "EC:64:9F" to "Espressif",
            "F0:08:D1" to "Apple",
            "F0:9F:C2" to "Espressif",
            "F4:63:1F" to "Espressif",
            "FC:58:FA" to "Espressif",
            "FC:64:3C" to "Apple",
            "FC:DB:B3" to "Espressif"
        )
    }

    private fun vendor(mac: String): String {
        if (mac.length < 8) return "unknown"
        val k = mac.uppercase().take(8)
        return oui.entries.firstOrNull { k.startsWith(it.key, ignoreCase = true) }?.value ?: "unknown"
    }

    init {
        viewModelScope.launch {
            connectionManager.events.collect { ev ->
                when (ev) {
                    is DeviceEvent.RawLine -> parseEvent(ev.line)
                    is DeviceEvent.WifiProbe -> addOrUpdate(WifiProbeEntry(
                        ssid = ev.ssid.ifBlank { "<hidden>" }, mac = ev.mac, rssi = ev.rssi,
                        channel = ev.channel, vendor = ev.vendor.ifBlank { vendor(ev.mac) }
                    ))
                    is DeviceEvent.SubGhzSignal,
                    is DeviceEvent.SubGhzSignalSaved,
                    is DeviceEvent.WaveformSample,
                    is DeviceEvent.SystemInfoUpdate,
                    is DeviceEvent.TehLinkNotice,
                    is DeviceEvent.OtaCompleted,
                    is DeviceEvent.BleAdSpamProgress,
                    is DeviceEvent.MousejackDongle,
                    is DeviceEvent.SubGhzSample,
                    is DeviceEvent.SubGhzDecodedFrame,
                    is DeviceEvent.NfcCloneProgress -> Unit
                }
            }
        }
    }

    private fun parseEvent(line: String) {
        val o = runCatching { JSONObject(line.trim()) }.getOrNull() ?: return
        val e = o.optString("event")
        val d = o.optJSONObject("data") ?: return
        if (e == "WifiProbe") {
            val ssid = d.optString("ssid", "<hidden>").ifBlank { "<hidden>" }
            val mac = d.optString("mac", "00:00:00:00:00:00")
            val rssi = d.optInt("rssi", -100)
            val ch = d.optInt("channel", 0)
            addOrUpdate(WifiProbeEntry(ssid, mac, rssi, ch, vendor(mac)))
        }
    }

    private fun addOrUpdate(e: WifiProbeEntry) {
        val current = _probes.value.toMutableList()
        val i = current.indexOfFirst { it.mac.equals(e.mac, ignoreCase = true) && it.ssid == e.ssid }
        if (i >= 0) {
            current[i] = current[i].copy(rssi = e.rssi, channel = e.channel,
                count = current[i].count + 1, lastSeenMs = System.currentTimeMillis())
        } else {
            current.add(e)
        }
        _probes.value = current.sortedWith(compareByDescending<WifiProbeEntry> { it.count }.thenBy { it.ssid })
    }

    fun start(channels: String = "1,6,11") {
        viewModelScope.launch {
            val r = connectionManager.tehLinkRunAction("wifi_offensive", "probe_start",
                JSONObject().put("channels", channels))
            r.onSuccess { _running.value = true }
            r.onFailure { t -> _toast.tryEmit("Start failed: ${t.message ?: "?"}") }
        }
    }

    fun stop() {
        viewModelScope.launch {
            connectionManager.tehLinkRunAction("wifi_offensive", "probe_stop", JSONObject())
            _running.value = false
        }
    }

    fun flush() {
        viewModelScope.launch {
            val r = connectionManager.tehLinkRunAction("wifi_offensive", "probe_flush", JSONObject())
            r.onSuccess { data ->
                val arr = data.rawResponse?.getJSONArray("probes") ?: return@onSuccess
                val out = mutableListOf<WifiProbeEntry>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    out += WifiProbeEntry(
                        ssid = o.getString("ssid").ifBlank { "<hidden>" },
                        mac = o.getString("mac"),
                        rssi = o.getInt("rssi"),
                        channel = o.getInt("channel"),
                        vendor = vendor(o.getString("mac")),
                        count = o.getInt("count")
                    )
                }
                _probes.value = (_probes.value + out).distinctBy { it.mac + it.ssid }.sortedByDescending { it.count }
                _toast.tryEmit("Flushed ${out.size} probes")
            }
        }
    }

    fun clear() { _probes.value = emptyList() }

    companion object {
        fun factory(connectionManager: DeviceConnectionManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProbeSnifferViewModel(connectionManager) as T
            }
    }
}
