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

    private var cryptoLastDigest = ""
    private var cryptoLastResult = ""
    private var cryptoLastAlgo = "sha256"

    /** Simulated auth token after pair; empty until paired. */
    private var mockAuthToken = "mock-teh-link-token"
    private var pairingWindowOpen = false
    private var badusbRemoteArmedUntilMs = 0L

    /** Abre ventana de pairing (simula long-press lateral en firmware). */
    fun openPairingWindow(durationSec: Int = 120) {
        pairingWindowOpen = true
    }

    /** Simula arm remoto BadUSB (long-press en plugin BadUSB). */
    fun armBadusbRemote(durationSec: Int = 120) {
        badusbRemoteArmedUntilMs = System.currentTimeMillis() + durationSec * 1000L
    }

    private fun isBadusbRemoteArmed(): Boolean =
        System.currentTimeMillis() < badusbRemoteArmedUntilMs

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
        val auth = root.optString("auth")

        if (cmd !in PUBLIC_CMDS && auth != mockAuthToken) {
            val err = JSONObject().put("ok", false).put("id", id).put("error", "auth_required")
            _incoming.emit(err.toString())
            return Result.success("OK")
        }

        val data = when (cmd) {
            "ping" -> JSONObject()
                .put("pong", true)
                .put("proto", "teh-link")
                .put("proto_ver", 3)
            "pair" -> {
                if (!pairingWindowOpen) {
                    return Result.success("OK").also {
                        _incoming.emit(
                            JSONObject().put("ok", false).put("id", id).put("error", "pair_window_closed").toString()
                        )
                    }
                }
                pairingWindowOpen = false
                JSONObject()
                    .put("token", mockAuthToken)
                    .put("proto", "teh-link")
                    .put("proto_ver", 3)
            }
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
                put(JSONObject()
                    .put("plugin_id", "crypto_toolkit")
                    .put("action", "hash")
                    .put("params", JSONArray().put("input").put("algo")))
                put(JSONObject()
                    .put("plugin_id", "crypto_toolkit")
                    .put("action", "base64_encode")
                    .put("params", JSONArray().put("input")))
                put(JSONObject()
                    .put("plugin_id", "crypto_toolkit")
                    .put("action", "base64_decode")
                    .put("params", JSONArray().put("input")))
                put(JSONObject()
                    .put("plugin_id", "crypto_toolkit")
                    .put("action", "gen_password")
                    .put("params", JSONArray().put("length")))
                put(JSONObject()
                    .put("plugin_id", "crypto_toolkit")
                    .put("action", "gen_passphrase")
                    .put("params", JSONArray().put("words")))
                put(JSONObject().put("plugin_id", "crypto_toolkit").put("action", "status"))
            })
            "run_action" -> handleRunAction(root)
            "get_action_state" -> handleGetActionState(root)
            "ota_begin" -> try {
                handleOtaBegin(root)
            } catch (e: Exception) {
                emitTehLinkError(id, e.message ?: "ota_error")
                return Result.success("OK")
            }
            "ota_chunk" -> try {
                handleOtaChunk(root)
            } catch (e: Exception) {
                emitTehLinkError(id, e.message ?: "ota_error")
                return Result.success("OK")
            }
            "ota_finish" -> try {
                handleOtaFinish()
            } catch (e: Exception) {
                emitTehLinkError(id, e.message ?: "ota_error")
                return Result.success("OK")
            }
            "ota_abort" -> {
                otaActive = false
                otaBuffer.reset()
                otaWritten = 0L
                otaNextSeq = 0
                JSONObject().put("state", "aborted")
            }
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

    private suspend fun emitTehLinkError(id: Int, error: String) {
        _incoming.emit(
            JSONObject()
                .put("ok", false)
                .put("id", id)
                .put("error", error)
                .toString()
        )
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

    private fun mockSha256Hex(input: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun mockBase64Encode(input: String): String {
        return android.util.Base64.encodeToString(
            input.toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP
        )
    }

    private fun mockBase64Decode(input: String): String {
        return String(
            android.util.Base64.decode(input, android.util.Base64.DEFAULT),
            Charsets.UTF_8
        )
    }

    private fun mockGenPassword(length: Int): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#\$%"
        val seed = (System.currentTimeMillis() % 1000).toInt()
        return (1..length.coerceIn(8, 64))
            .map { chars[(seed + it * 17) % chars.length] }
            .joinToString("")
    }

    private var mockPassphraseCounter = 0

    private var otaActive = false
    private var otaTotalSize = 0L
    private var otaWritten = 0L
    private var otaExpectedSha256 = ""
    private var otaNextSeq = 0
    private val otaBuffer = java.io.ByteArrayOutputStream()

    private fun mockGenPassphrase(words: Int): String {
        val wordList = listOf("alpha", "bravo", "cascade", "delta", "ember", "flux", "glyph", "helix")
        mockPassphraseCounter++
        return (0 until words.coerceIn(3, 8))
            .map { wordList[(mockPassphraseCounter + it) % wordList.size] }
            .joinToString("-")
    }

    private fun cryptoStateJson(action: String = "status"): JSONObject {
        return JSONObject()
            .put("plugin_id", "crypto_toolkit")
            .put("action", action)
            .put("state", if (cryptoLastDigest.isNotBlank() || cryptoLastResult.isNotBlank()) "done" else "idle")
            .put("message", cryptoLastResult.ifBlank { cryptoLastDigest.ifBlank { "Ready" } })
            .put("digest", cryptoLastDigest)
            .put("result", cryptoLastResult)
            .put("algo", cryptoLastAlgo)
            .put("last_result", cryptoLastResult.ifBlank { cryptoLastDigest })
    }

    private fun handleRunAction(root: JSONObject): JSONObject? {
        val pluginId = root.optString("plugin_id")
        val action = root.optString("action")
        val params = root.optJSONObject("params") ?: JSONObject()

        return when (pluginId) {
            "badusb" -> when (action) {
                "run_script" -> {
                    if (!isBadusbRemoteArmed()) {
                        return badusbStateJson()
                            .put("action", action)
                            .put("state", "error")
                            .put("message", "badusb_not_armed")
                    }
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
            "crypto_toolkit" -> when (action) {
                "hash" -> {
                    val input = params.optString("input")
                    cryptoLastAlgo = params.optString("algo", "sha256").ifBlank { "sha256" }
                    cryptoLastDigest = when (cryptoLastAlgo.lowercase()) {
                        "md5", "sha1", "sha512" -> mockSha256Hex(input).take(
                            when (cryptoLastAlgo.lowercase()) {
                                "md5" -> 32
                                "sha1" -> 40
                                else -> 128
                            }
                        )
                        else -> mockSha256Hex(input)
                    }
                    cryptoLastResult = cryptoLastDigest
                    cryptoStateJson(action).put("state", "done")
                }
                "base64_encode" -> {
                    val input = params.optString("input")
                    cryptoLastDigest = ""
                    cryptoLastResult = mockBase64Encode(input)
                    cryptoStateJson(action).put("state", "done")
                }
                "base64_decode" -> {
                    val input = params.optString("input")
                    cryptoLastDigest = ""
                    cryptoLastResult = mockBase64Decode(input)
                    cryptoStateJson(action).put("state", "done")
                }
                "gen_password" -> {
                    val length = params.optInt("length", 16)
                    cryptoLastDigest = ""
                    cryptoLastResult = mockGenPassword(length)
                    cryptoStateJson(action).put("state", "done")
                }
                "gen_passphrase" -> {
                    val words = params.optInt("words", 4)
                    cryptoLastDigest = ""
                    cryptoLastResult = mockGenPassphrase(words)
                    cryptoStateJson(action).put("state", "done")
                }
                "status" -> cryptoStateJson(action)
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
            "crypto_toolkit" -> cryptoStateJson()
            else -> null
        }
    }

    private fun handleOtaBegin(root: JSONObject): JSONObject {
        if (otaActive) {
            throw IllegalStateException("ota_already_active")
        }
        otaTotalSize = root.optLong("size", 0L)
        otaExpectedSha256 = root.optString("sha256", "").lowercase()
        if (otaTotalSize <= 0 || otaExpectedSha256.length != 64) {
            throw IllegalArgumentException("invalid_params")
        }
        otaActive = true
        otaWritten = 0L
        otaNextSeq = 0
        otaBuffer.reset()
        return JSONObject()
            .put("bytes_written", 0)
            .put("total_size", otaTotalSize)
            .put("state", "in_progress")
    }

    private fun handleOtaChunk(root: JSONObject): JSONObject {
        if (!otaActive) {
            throw IllegalStateException("ota_not_active")
        }
        val seq = root.optInt("seq", -1)
        if (seq != otaNextSeq) {
            throw IllegalStateException("unexpected_seq")
        }
        val b64 = root.optString("data", "")
        val chunk = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
        if (chunk.isEmpty()) {
            throw IllegalArgumentException("base64_decode_failed")
        }
        otaBuffer.write(chunk)
        otaWritten += chunk.size
        otaNextSeq++
        return JSONObject()
            .put("bytes_written", otaWritten)
            .put("total_size", otaTotalSize)
            .put("state", "in_progress")
            .put("chunk_bytes", chunk.size)
    }

    private fun handleOtaFinish(): JSONObject {
        if (!otaActive) {
            throw IllegalStateException("ota_not_active")
        }
        if (otaWritten < otaTotalSize) {
            throw IllegalStateException("ota_incomplete")
        }
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(otaBuffer.toByteArray())
            .joinToString("") { "%02x".format(it) }
        if (!digest.equals(otaExpectedSha256, ignoreCase = true)) {
            otaActive = false
            otaBuffer.reset()
            throw IllegalStateException("sha256_mismatch")
        }
        otaActive = false
        otaBuffer.reset()
        return JSONObject()
            .put("bytes_written", otaWritten)
            .put("total_size", otaTotalSize)
            .put("state", "complete")
            .put("rebooting", true)
    }

    companion object {
        private val PUBLIC_CMDS = setOf("ping", "get_info", "pair")

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
