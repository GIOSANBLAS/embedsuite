package com.embedsuite.app.connection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Transporte simulado para pruebas UI sin hardware T-Embed.
 * Soporta respuestas Bruce CLI y TEH-Link JSON.
 */
class MockTransport(
    private val responses: Map<String, String> = defaultResponses
) : TEmbedTransport {

    override val type: TransportType = TransportType.USB
    override val isConnected: Boolean = true

    private var mockUiScreen = "Home"
    private var mockActivePlugin = ""

    private var badusbRunning = false
    private var badusbProgress = 0
    private var badusbPath = ""
    private var badusbStartMs = 0L

    private var subghzCapturing = false
    private var subghzCaptureSeconds = 15
    private var subghzCaptureStartMs = 0L
    private var subghzPackets = 0

    private var wifiScanning = false
    private var wifiScanSeconds = 10
    private var wifiScanStartMs = 0L

    private var bleScanning = false
    private var bleScanSeconds = 10
    private var bleScanStartMs = 0L

    private var wardrivingRunning = false
    private var wardrivingApCount = 0
    private var wardrivingCsvPath = ""

    private val _incoming = MutableSharedFlow<String>(extraBufferCapacity = 64)

    override fun incomingLines(): Flow<String> = _incoming.asSharedFlow()

    override suspend fun connect(): Result<String> = Result.success("Mock T-Embed Xibalba")

    override suspend fun disconnect() = Unit

    override suspend fun sendCommand(command: String): Result<String> {
        val trimmed = command.trim()
        if (trimmed.startsWith("{")) {
            return handleTehLink(trimmed)
        }

        val key = trimmed.lowercase()
        val response = responses[key]
            ?: responses.entries.firstOrNull { key.startsWith(it.key) }?.value
            ?: "OK"
        _incoming.emit(response)
        return Result.success(response)
    }

    private suspend fun handleTehLink(json: String): Result<String> {
        val root = runCatching { JSONObject(json) }.getOrElse {
            return Result.failure(it)
        }
        val cmd = root.optString("cmd")
        val id = root.optInt("id", 0)

        val data = when (cmd) {
            "ping" -> JSONObject()
                .put("pong", true)
                .put("proto", "teh-link")
                .put("proto_ver", 3)
            "get_info" -> JSONObject()
                .put("product", "T-Embed Xibalba")
                .put("version", "0.12")
                .put("codename", "Mimic")
                .put("channel", "release")
                .put("proto", "teh-link")
                .put("proto_ver", 3)
                .put("plugins", JSONArray().apply {
                    put(JSONObject().put("id", "subghz_analyzer").put("name", "Sub-GHz").put("version", "1.0.0").put("author", "Xibalba"))
                    put(JSONObject().put("id", "badusb").put("name", "BadUSB").put("version", "1.0.0").put("author", "Xibalba"))
                    put(JSONObject().put("id", "wifi").put("name", "WiFi").put("version", "1.0.0").put("author", "Xibalba"))
                })
            "get_status", "get_screen" -> JSONObject()
                .put("sd_mounted", false)
                .put("flash_mounted", true)
                .put("ui_screen", mockUiScreen)
                .put("active_plugin", mockActivePlugin)
                .put("uptime_ms", 12345)
                .put("sim", JSONObject()
                    .put("cc1101", true)
                    .put("ble", true)
                    .put("wifi", true)
                    .put("badusb", true)
                    .put("gps", true))
            "open_plugin" -> {
                val pluginId = root.optString("plugin_id")
                if (pluginId.isBlank()) {
                    return Result.success("OK").also {
                        _incoming.emit(
                            JSONObject().put("ok", false).put("id", id).put("error", "missing_plugin_id").toString()
                        )
                    }
                }
                mockActivePlugin = pluginId
                mockUiScreen = pluginId.replaceFirstChar { it.uppercase() }
                JSONObject()
                    .put("plugin_id", pluginId)
                    .put("ui_screen", mockUiScreen)
                    .put("active_plugin", mockActivePlugin)
            }
            "back_to_menu" -> {
                mockActivePlugin = ""
                mockUiScreen = "Home"
                JSONObject()
                    .put("ui_screen", mockUiScreen)
                    .put("active_plugin", mockActivePlugin)
            }
            "list_actions" -> JSONObject().put("actions", JSONArray().apply {
                put(JSONObject()
                    .put("plugin_id", "badusb")
                    .put("action", "run_script")
                    .put("params", JSONArray().put("path")))
                put(JSONObject().put("plugin_id", "badusb").put("action", "stop"))
                put(JSONObject().put("plugin_id", "badusb").put("action", "status"))
                put(JSONObject()
                    .put("plugin_id", "subghz_analyzer")
                    .put("action", "capture_start")
                    .put("params", JSONArray().put("seconds")))
                put(JSONObject().put("plugin_id", "subghz_analyzer").put("action", "capture_stop"))
                put(JSONObject().put("plugin_id", "subghz_analyzer").put("action", "status"))
                put(JSONObject()
                    .put("plugin_id", "wifi_toolkit")
                    .put("action", "scan_start")
                    .put("params", JSONArray().put("seconds")))
                put(JSONObject().put("plugin_id", "wifi_toolkit").put("action", "scan_stop"))
                put(JSONObject().put("plugin_id", "wifi_toolkit").put("action", "status"))
                put(JSONObject().put("plugin_id", "wardriving").put("action", "start"))
                put(JSONObject().put("plugin_id", "wardriving").put("action", "stop"))
                put(JSONObject().put("plugin_id", "wardriving").put("action", "status"))
                put(JSONObject()
                    .put("plugin_id", "ble_toolkit")
                    .put("action", "scan_start")
                    .put("params", JSONArray().put("seconds")))
                put(JSONObject().put("plugin_id", "ble_toolkit").put("action", "scan_stop"))
                put(JSONObject().put("plugin_id", "ble_toolkit").put("action", "status"))
            })
            "run_action" -> handleRunAction(root)
            "get_action_state" -> handleGetActionState(root)
            else -> null
        }

        val response = if (data == null) {
            JSONObject().put("ok", false).put("id", id).put("error", "unknown_cmd")
        } else {
            JSONObject().put("ok", true).put("id", id).put("data", data)
        }
        _incoming.emit(response.toString())
        return Result.success("OK")
    }

    private fun tickBadusbProgress() {
        if (!badusbRunning) return
        val elapsed = System.currentTimeMillis() - badusbStartMs
        badusbProgress = ((elapsed / 80L).toInt()).coerceIn(0, 100)
        if (badusbProgress >= 100) {
            badusbRunning = false
            badusbProgress = 100
        }
    }

    private fun tickSubghzCapture() {
        if (!subghzCapturing) return
        val elapsedSec = ((System.currentTimeMillis() - subghzCaptureStartMs) / 1000L).toInt()
        subghzPackets = (elapsedSec * 3) + 2
        if (elapsedSec >= subghzCaptureSeconds) {
            subghzCapturing = false
        }
    }

    private fun badusbStateJson(): JSONObject {
        tickBadusbProgress()
        return JSONObject()
            .put("plugin_id", "badusb")
            .put("action", "status")
            .put("state", if (badusbRunning) "running" else if (badusbProgress >= 100) "done" else "idle")
            .put("progress", badusbProgress)
            .put("message", when {
                badusbRunning -> "HID SIM: typing line ${badusbProgress / 10 + 1}"
                badusbProgress >= 100 -> "Script complete"
                else -> "Ready"
            })
            .put("loaded_path", badusbPath)
            .put("running", badusbRunning)
    }

    private fun subghzStateJson(): JSONObject {
        tickSubghzCapture()
        val elapsedSec = if (subghzCapturing) {
            ((System.currentTimeMillis() - subghzCaptureStartMs) / 1000L).toInt()
        } else 0
        val remaining = if (subghzCapturing) (subghzCaptureSeconds - elapsedSec).coerceAtLeast(0) else 0
        return JSONObject()
            .put("plugin_id", "subghz_analyzer")
            .put("action", "status")
            .put("state", if (subghzCapturing) "capturing" else "idle")
            .put("capturing", subghzCapturing)
            .put("packets", subghzPackets)
            .put("seconds_remaining", remaining)
            .put("message", if (subghzCapturing) "RX ${subghzCaptureSeconds}s @ 433.92 MHz" else "Idle")
    }

    private fun tickWifiScan() {
        if (!wifiScanning) return
        val elapsedSec = ((System.currentTimeMillis() - wifiScanStartMs) / 1000L).toInt()
        if (elapsedSec >= wifiScanSeconds) {
            wifiScanning = false
        }
    }

    private fun wifiStateJson(): JSONObject {
        tickWifiScan()
        val elapsedSec = if (wifiScanning) {
            ((System.currentTimeMillis() - wifiScanStartMs) / 1000L).toInt()
        } else 0
        val remaining = if (wifiScanning) (wifiScanSeconds - elapsedSec).coerceAtLeast(0) else 0
        val aps = JSONArray().apply {
            put(JSONObject()
                .put("ssid", "HomeWiFi_5G")
                .put("bssid", "AA:BB:CC:DD:EE:01")
                .put("channel", 6)
                .put("rssi", -42)
                .put("security", "WPA2"))
            put(JSONObject()
                .put("ssid", "Starbucks")
                .put("bssid", "AA:BB:CC:DD:EE:02")
                .put("channel", 11)
                .put("rssi", -58)
                .put("security", "WPA2"))
            put(JSONObject()
                .put("ssid", "Guest_Open")
                .put("bssid", "AA:BB:CC:DD:EE:03")
                .put("channel", 1)
                .put("rssi", -71)
                .put("security", "Open"))
        }
        return JSONObject()
            .put("plugin_id", "wifi_toolkit")
            .put("action", "status")
            .put("state", if (wifiScanning) "scanning" else "idle")
            .put("running", wifiScanning)
            .put("seconds_remaining", remaining)
            .put("message", if (wifiScanning) "WiFi scan ${wifiScanSeconds}s" else "${aps.length()} APs")
            .put("aps", aps)
    }

    private fun tickBleScan() {
        if (!bleScanning) return
        val elapsedSec = ((System.currentTimeMillis() - bleScanStartMs) / 1000L).toInt()
        if (elapsedSec >= bleScanSeconds) {
            bleScanning = false
        }
    }

    private fun bleStateJson(): JSONObject {
        tickBleScan()
        val elapsedSec = if (bleScanning) {
            ((System.currentTimeMillis() - bleScanStartMs) / 1000L).toInt()
        } else 0
        val remaining = if (bleScanning) (bleScanSeconds - elapsedSec).coerceAtLeast(0) else 0
        val devices = JSONArray().apply {
            put(JSONObject()
                .put("name", "AirTag")
                .put("address", "A1:B2:C3:D4:E5:F6")
                .put("rssi", -45)
                .put("is_tracker", true))
            put(JSONObject()
                .put("name", "Samsung SmartTag")
                .put("address", "11:22:33:44:55:66")
                .put("rssi", -62)
                .put("is_tracker", true))
            put(JSONObject()
                .put("name", "iPhone de Juan")
                .put("address", "77:88:99:AA:BB:CC")
                .put("rssi", -55)
                .put("is_tracker", false))
        }
        return JSONObject()
            .put("plugin_id", "ble_toolkit")
            .put("action", "status")
            .put("state", if (bleScanning) "scanning" else "idle")
            .put("running", bleScanning)
            .put("seconds_remaining", remaining)
            .put("message", if (bleScanning) "BLE scan ${bleScanSeconds}s" else "${devices.length()} devices")
            .put("devices", devices)
    }

    private fun wardrivingStateJson(): JSONObject {
        if (wardrivingRunning) {
            wardrivingApCount = (wardrivingApCount + 1).coerceAtMost(128)
        }
        return JSONObject()
            .put("plugin_id", "wardriving")
            .put("action", "status")
            .put("state", if (wardrivingRunning) "recording" else "idle")
            .put("running", wardrivingRunning)
            .put("ap_count", wardrivingApCount)
            .put("csv_path", wardrivingCsvPath)
            .put("message", if (wardrivingRunning) "Wardriving… $wardrivingApCount APs" else "Stopped")
    }

    private fun handleRunAction(root: JSONObject): JSONObject? {
        val pluginId = root.optString("plugin_id")
        val action = root.optString("action")
        val params = root.optJSONObject("params") ?: JSONObject()

        return when (pluginId) {
            "badusb" -> when (action) {
                "run_script" -> {
                    badusbPath = params.optString("path", "/sdcard/plugins/badusb/demo.txt")
                    badusbRunning = true
                    badusbProgress = 0
                    badusbStartMs = System.currentTimeMillis()
                    badusbStateJson()
                        .put("action", action)
                        .put("state", "started")
                }
                "stop" -> {
                    badusbRunning = false
                    badusbStateJson().put("action", action).put("state", "stopped")
                }
                "status" -> badusbStateJson()
                else -> null
            }
            "subghz_analyzer" -> when (action) {
                "capture_start" -> {
                    subghzCaptureSeconds = params.optInt("seconds", 15).coerceIn(1, 120)
                    subghzCapturing = true
                    subghzCaptureStartMs = System.currentTimeMillis()
                    subghzPackets = 0
                    subghzStateJson()
                        .put("action", action)
                        .put("state", "started")
                }
                "capture_stop" -> {
                    subghzCapturing = false
                    subghzStateJson().put("action", action).put("state", "stopped")
                }
                "status" -> subghzStateJson()
                else -> null
            }
            "wifi_toolkit" -> when (action) {
                "scan_start" -> {
                    wifiScanSeconds = params.optInt("seconds", 10).coerceIn(1, 120)
                    wifiScanning = true
                    wifiScanStartMs = System.currentTimeMillis()
                    wifiStateJson().put("action", action).put("state", "started")
                }
                "scan_stop" -> {
                    wifiScanning = false
                    wifiStateJson().put("action", action).put("state", "stopped")
                }
                "status" -> wifiStateJson()
                else -> null
            }
            "wardriving" -> when (action) {
                "start" -> {
                    wardrivingRunning = true
                    wardrivingApCount = 0
                    wardrivingCsvPath = "/sdcard/wardriving/session_${System.currentTimeMillis()}.csv"
                    wardrivingStateJson().put("action", action).put("state", "started")
                }
                "stop" -> {
                    wardrivingRunning = false
                    wardrivingStateJson().put("action", action).put("state", "stopped")
                }
                "status" -> wardrivingStateJson()
                else -> null
            }
            "ble_toolkit" -> when (action) {
                "scan_start" -> {
                    bleScanSeconds = params.optInt("seconds", 10).coerceIn(1, 120)
                    bleScanning = true
                    bleScanStartMs = System.currentTimeMillis()
                    bleStateJson().put("action", action).put("state", "started")
                }
                "scan_stop" -> {
                    bleScanning = false
                    bleStateJson().put("action", action).put("state", "stopped")
                }
                "status" -> bleStateJson()
                else -> null
            }
            else -> null
        }
    }

    private fun handleGetActionState(root: JSONObject): JSONObject? {
        val pluginId = root.optString("plugin_id")
        return when (pluginId) {
            "badusb" -> badusbStateJson()
            "subghz_analyzer" -> subghzStateJson()
            "wifi_toolkit" -> wifiStateJson()
            "wardriving" -> wardrivingStateJson()
            "ble_toolkit" -> bleStateJson()
            else -> null
        }
    }

    companion object {
        val defaultResponses = mapOf(
            "info" to "Bruce v1.8 | CC1101 | Free heap: 120000",
            "free" to "Heap: 118432 bytes",
            "uptime" to "Uptime: 01:23:45",
            "subghz" to "Sub-GHz menu",
            "storage list /" to "[F] BruceRF/demo.sub\n[F] ir/tv_power.ir",
            "i2c scan" to "0x3C OLED\n0x50 EEPROM"
        )
    }
}
