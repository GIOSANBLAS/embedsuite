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
            else -> null
        }
    }

    private fun handleGetActionState(root: JSONObject): JSONObject? {
        val pluginId = root.optString("plugin_id")
        return when (pluginId) {
            "badusb" -> badusbStateJson()
            "subghz_analyzer" -> subghzStateJson()
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
