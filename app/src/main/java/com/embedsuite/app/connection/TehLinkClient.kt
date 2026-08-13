package com.embedsuite.app.connection

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger

/**
 * Cliente TEH-Link — JSON NDJSON sobre USB CDC (firmware T-Embed Xibalba).
 */
class TehLinkClient(
    private val scope: CoroutineScope,
    private val secureSession: com.embedsuite.app.security.TehLinkSecureSession? = null
) {
    private val requestId = AtomicInteger(0)
    private val linkMutex = Mutex()

    var authToken: String = ""

    /** Optional ECDH + AES-GCM session after pairing. */
    suspend fun establishSecureSession(transport: TEmbedTransport): Result<Unit> {
        val session = secureSession
            ?: return Result.failure(IllegalStateException("secure_session_unavailable"))
        return session.handshake(this, transport).map { }
    }

    suspend fun ping(transport: TEmbedTransport): Result<Boolean> {
        return execute(transport, "ping").map { data ->
            data.optBoolean("pong") &&
                data.optString("proto") == "teh-link"
        }
    }

    /** Requiere long-press en el dispositivo para abrir ventana de pairing. */
    suspend fun pair(transport: TEmbedTransport): Result<String> {
        return execute(transport, "pair", timeoutMs = 8_000L).mapCatching { data ->
            data.optString("token").also { token ->
                if (token.isBlank()) throw Exception("pair_sin_token")
            }
        }
    }

    suspend fun getInfo(transport: TEmbedTransport): Result<TehLinkDeviceInfo> {
        return execute(transport, "get_info").map(TehLinkResponseParser::parseDeviceInfo)
    }

    suspend fun getStatus(transport: TEmbedTransport): Result<TehLinkDeviceStatus> {
        return execute(transport, "get_status").map(TehLinkResponseParser::parseDeviceStatus)
    }

    suspend fun getScreen(transport: TEmbedTransport): Result<TehLinkScreenInfo> {
        return execute(transport, "get_screen").map(TehLinkResponseParser::parseScreenInfo)
    }

    suspend fun openPlugin(transport: TEmbedTransport, pluginId: String): Result<TehLinkScreenInfo> {
        val args = JSONObject().put("plugin_id", pluginId)
        return execute(transport, "open_plugin", args).map(TehLinkResponseParser::parseScreenInfo)
    }

    suspend fun backToMenu(transport: TEmbedTransport): Result<TehLinkScreenInfo> {
        return execute(transport, "back_to_menu").map(TehLinkResponseParser::parseScreenInfo)
    }

    suspend fun listActions(transport: TEmbedTransport): Result<List<TehLinkActionInfo>> {
        return execute(transport, "list_actions").map(TehLinkResponseParser::parseActionList)
    }

    suspend fun runAction(
        transport: TEmbedTransport,
        pluginId: String,
        action: String,
        params: JSONObject = JSONObject()
    ): Result<TehLinkActionResult> {
        val args = JSONObject()
            .put("plugin_id", pluginId)
            .put("action", action)
            .put("params", params)
        return execute(transport, "run_action", args, timeoutMs = timeoutForAction(action, params))
            .map(TehLinkResponseParser::parseActionResult)
    }

    suspend fun getActionState(
        transport: TEmbedTransport,
        pluginId: String,
        action: String? = null
    ): Result<TehLinkActionState> {
        val args = JSONObject().put("plugin_id", pluginId)
        if (!action.isNullOrBlank()) {
            args.put("action", action)
        }
        return execute(transport, "get_action_state", args).map(TehLinkResponseParser::parseActionState)
    }

    suspend fun runWifiScan(transport: TEmbedTransport, seconds: Int): Result<TehLinkActionResult> {
        val sec = seconds.coerceIn(1, 120)
        return runAction(
            transport,
            pluginId = "wifi_toolkit",
            action = "scan_start",
            params = JSONObject().put("seconds", sec)
        )
    }

    suspend fun runWardrivingStart(
        transport: TEmbedTransport,
        latitude: Double? = null,
        longitude: Double? = null,
        altitudeM: Double? = null
    ): Result<TehLinkActionResult> {
        val params = JSONObject()
        if (latitude != null && longitude != null) {
            params.put("lat", latitude)
            params.put("lon", longitude)
            if (altitudeM != null) {
                params.put("alt", altitudeM)
            }
        }
        return runAction(transport, pluginId = "wardriving", action = "start", params = params)
    }

    suspend fun runWardrivingGpsUpdate(
        transport: TEmbedTransport,
        latitude: Double,
        longitude: Double,
        altitudeM: Double? = null
    ): Result<TehLinkActionResult> {
        val params = JSONObject()
            .put("lat", latitude)
            .put("lon", longitude)
        if (altitudeM != null) {
            params.put("alt", altitudeM)
        }
        return runAction(transport, pluginId = "wardriving", action = "gps_update", params = params)
    }

    suspend fun runNfcRead(transport: TEmbedTransport): Result<TehLinkActionResult> {
        return runAction(transport, pluginId = "nfc_toolkit", action = "read")
    }

    suspend fun runIrSend(
        transport: TEmbedTransport,
        protocol: String,
        address: String,
        command: String
    ): Result<TehLinkActionResult> {
        return runAction(
            transport,
            pluginId = "ir_toolkit",
            action = "send",
            params = JSONObject()
                .put("protocol", protocol)
                .put("address", address)
                .put("command", command)
        )
    }

    suspend fun runIrSendRaw(transport: TEmbedTransport, raw: String): Result<TehLinkActionResult> {
        return runAction(
            transport,
            pluginId = "ir_toolkit",
            action = "send",
            params = JSONObject().put("raw", raw)
        )
    }

    suspend fun runBleScan(transport: TEmbedTransport, seconds: Int): Result<TehLinkActionResult> {
        val sec = seconds.coerceIn(1, 120)
        return runAction(
            transport,
            pluginId = "ble_toolkit",
            action = "scan_start",
            params = JSONObject().put("seconds", sec)
        )
    }

    suspend fun runCryptoHash(
        transport: TEmbedTransport,
        input: String,
        algo: String = "sha256"
    ): Result<TehLinkActionResult> {
        return runAction(
            transport,
            pluginId = "crypto_toolkit",
            action = "hash",
            params = JSONObject()
                .put("input", input)
                .put("algo", algo)
        )
    }

    suspend fun runCryptoBase64Encode(
        transport: TEmbedTransport,
        input: String
    ): Result<TehLinkActionResult> {
        return runAction(
            transport,
            pluginId = "crypto_toolkit",
            action = "base64_encode",
            params = JSONObject().put("input", input)
        )
    }

    suspend fun runGenPassword(
        transport: TEmbedTransport,
        length: Int = 16
    ): Result<TehLinkActionResult> {
        return runAction(
            transport,
            pluginId = "crypto_toolkit",
            action = "gen_password",
            params = JSONObject().put("length", length)
        )
    }

    suspend fun runSubGhzTx(
        transport: TEmbedTransport,
        rawHex: String,
        freqMhz: Double? = null
    ): Result<TehLinkActionResult> {
        val params = JSONObject()
            .put("raw", rawHex)
            .put("confirm", true)
        if (freqMhz != null) {
            params.put("freq_mhz", freqMhz)
        }
        return runAction(
            transport,
            pluginId = "subghz_analyzer",
            action = "subghz_tx",
            params = params
        )
    }

    suspend fun runSubGhzReplay(
        transport: TEmbedTransport,
        devicePath: String
    ): Result<TehLinkActionResult> {
        return runAction(
            transport,
            pluginId = "subghz_analyzer",
            action = "subghz_replay",
            params = JSONObject()
                .put("path", devicePath)
                .put("confirm", true)
        )
    }

    suspend fun runIrRxStart(
        transport: TEmbedTransport,
        seconds: Int = 10
    ): Result<TehLinkActionResult> {
        return runAction(
            transport,
            pluginId = "ir_toolkit",
            action = "rx_start",
            params = JSONObject().put("seconds", seconds.coerceIn(1, 60))
        )
    }

    suspend fun runNfcEmulate(
        transport: TEmbedTransport,
        uid: String
    ): Result<TehLinkActionResult> {
        return runAction(
            transport,
            pluginId = "nfc_toolkit",
            action = "emulate",
            params = JSONObject().put("uid", uid)
        )
    }

    // ===== XIBALBA v0.19+ (Evil Portal API) =====
    suspend fun runEvilPortalStart(
        transport: TEmbedTransport,
        ssid: String,
        templateId: String = "generic",
        channel: Int = 6
    ): Result<TehLinkActionResult> {
        val params = JSONObject()
            .put("ssid", ssid)
            .put("template_id", templateId)
            .put("channel", channel.coerceIn(1, 11))
        return runAction(transport, "evil_portal", "start", params)
    }

    suspend fun runEvilPortalStop(transport: TEmbedTransport): Result<TehLinkActionResult> {
        return runAction(transport, "evil_portal", "stop")
    }

    suspend fun runEvilPortalCreds(transport: TEmbedTransport): Result<TehLinkActionResult> {
        return runAction(transport, "evil_portal", "creds")
    }

    suspend fun runEvilPortalClearCreds(transport: TEmbedTransport): Result<TehLinkActionResult> {
        return runAction(transport, "evil_portal", "clear_creds")
    }

    suspend fun runEvilPortalStatus(transport: TEmbedTransport): Result<TehLinkActionResult> {
        return runAction(transport, "evil_portal", "status")
    }

    // ===== XIBALBA v0.19+ (Beacon Spam API) =====
    suspend fun runBeaconSpamStart(
        transport: TEmbedTransport,
        spec: String = "random:50",
        hz: Int = 10,
        channel: Int = 0
    ): Result<TehLinkActionResult> {
        val params = JSONObject()
            .put("spec", spec)
            .put("hz", hz.coerceIn(1, 100))
            .put("channel", channel.coerceIn(0, 11))
        return runAction(transport, "beacon_spam", "start", params)
    }

    suspend fun runBeaconSpamStop(transport: TEmbedTransport): Result<TehLinkActionResult> {
        return runAction(transport, "beacon_spam", "stop")
    }

    suspend fun runBeaconSpamStatus(transport: TEmbedTransport): Result<TehLinkActionResult> {
        return runAction(transport, "beacon_spam", "status")
    }

    suspend fun runNrf24Status(transport: TEmbedTransport): Result<TehLinkActionResult> {
        return runAction(transport, pluginId = "nrf24_toolkit", action = "status")
    }

    suspend fun runNrf24Tx(
        transport: TEmbedTransport,
        hex: String
    ): Result<TehLinkActionResult> {
        return runAction(
            transport,
            pluginId = "nrf24_toolkit",
            action = "tx",
            params = JSONObject().put("hex", hex)
        )
    }

    suspend fun getOtaStatus(transport: TEmbedTransport): Result<TehLinkOtaStatus> {
        return execute(transport, "ota_status", timeoutMs = 5_000L)
            .map(TehLinkResponseParser::parseOtaStatus)
    }

    /** Borra el coredump ELF guardado en flash tras haberlo volcado a Android. */
    suspend fun clearCoredump(transport: TEmbedTransport): Result<Boolean> {
        return execute(transport, "coredump_clear", timeoutMs = 8_000L)
            .mapCatching { data -> data.optBoolean("ok") }
    }

    /** Stress test integrado: loop N x [wifi_scan, ble_scan, subghz_capture, crypto_hash]
     *  y devuelve diferencia de heap libre (para detectar memory leaks). */
    suspend fun runSoakStress(
        transport: TEmbedTransport,
        iterations: Int = 5,
        perStepSeconds: Int = 3
    ): Result<TehLinkSoakResult> {
        val params = JSONObject()
            .put("iterations", iterations.coerceIn(1, 20))
            .put("seconds", perStepSeconds.coerceIn(1, 30))
        return runAction(transport, "diagnostic_tools", "soak_test", params)
            .mapCatching { result ->
                result.state.soak
                    ?: TehLinkResponseParser.parseSoakResult(JSONObject())
            }
    }

    suspend fun runNfcEmulateStop(transport: TEmbedTransport): Result<TehLinkActionResult> {
        return runAction(transport, pluginId = "nfc_toolkit", action = "emulate_stop")
    }

    // ===== HW bridge (flat cmds, Xibalba teh_hw) =====

    suspend fun syncTime(
        transport: TEmbedTransport,
        timestampNs: Long = System.nanoTime()
    ): Result<JSONObject> {
        val params = JSONObject().put("timestamp_ns", timestampNs)
        return execute(transport, "time.sync", JSONObject().put("params", params))
    }

    suspend fun getTime(transport: TEmbedTransport): Result<JSONObject> {
        return execute(transport, "time.get")
    }

    suspend fun audioBeep(
        transport: TEmbedTransport,
        freqHz: Int = 1000,
        durationMs: Int = 100
    ): Result<JSONObject> {
        val params = JSONObject()
            .put("freq", freqHz.coerceIn(20, 12_000))
            .put("duration", durationMs.coerceIn(10, 1_500))
        return execute(transport, "audio.beep", JSONObject().put("params", params))
    }

    /** Alias TEH `audio.tone` (freq_hz / duration_ms) — mismo efecto que [audioBeep]. */
    suspend fun audioTone(
        transport: TEmbedTransport,
        freqHz: Int = 1000,
        durationMs: Int = 100
    ): Result<JSONObject> {
        val params = JSONObject()
            .put("freq_hz", freqHz.coerceIn(20, 12_000))
            .put("duration_ms", durationMs.coerceIn(10, 1_500))
        return execute(transport, "audio.tone", JSONObject().put("params", params))
    }

    suspend fun nfcWrite(
        transport: TEmbedTransport,
        hexData: String? = null,
        url: String? = null,
        block: Int = 1
    ): Result<JSONObject> {
        val params = JSONObject().put("block", block)
        if (!hexData.isNullOrBlank()) params.put("data", hexData)
        if (!url.isNullOrBlank()) params.put("url", url)
        return execute(transport, "nfc.write", JSONObject().put("params", params), timeoutMs = 15_000L)
    }

    suspend fun nfcReadFlat(
        transport: TEmbedTransport,
        timeoutMs: Int = 5_000
    ): Result<JSONObject> {
        val params = JSONObject().put("timeout", timeoutMs.coerceIn(500, 30_000))
        return execute(transport, "nfc.read", JSONObject().put("params", params), timeoutMs = timeoutMs + 3_000L)
    }

    suspend fun sdStatus(transport: TEmbedTransport): Result<JSONObject> {
        return execute(transport, "sd.status")
    }

    suspend fun sdList(
        transport: TEmbedTransport,
        path: String = "/xibalba"
    ): Result<JSONObject> {
        val params = JSONObject().put("path", path)
        return execute(transport, "sd.list", JSONObject().put("params", params))
    }

    suspend fun listFiles(
        transport: TEmbedTransport,
        path: String = "/"
    ): Result<JSONObject> {
        val params = JSONObject().put("path", path)
        return execute(transport, "list_files", JSONObject().put("params", params), timeoutMs = 12_000L)
    }

    /**
     * Descarga un archivo de `/xibalba` en chunks Base64 (`event: file_chunk`).
     * Reensambla en orden y devuelve los bytes.
     */
    suspend fun downloadFile(
        transport: TEmbedTransport,
        path: String
    ): Result<ByteArray> = linkMutex.withLock {
        val id = requestId.incrementAndGet()
        val payload = JSONObject()
            .put("cmd", "download_file")
            .put("id", id)
            .put("params", JSONObject().put("path", path))
        if (authToken.isNotBlank()) payload.put("auth", authToken)
        val outboundJson = payload.toString()

        val chunks = sortedMapOf<Int, ByteArray>()
        val replies = mutableListOf<String>()
        val job = scope.launch {
            transport.incomingLines().collect { line ->
                val trimmed = line.trim()
                if (!trimmed.startsWith("{")) return@collect
                val obj = runCatching { JSONObject(trimmed) }.getOrNull() ?: return@collect
                when {
                    obj.optString("event") == "file_chunk" -> {
                        val d = obj.optJSONObject("data") ?: return@collect
                        val b64 = d.optString("data")
                        if (b64.isNotBlank()) {
                            runCatching { Base64.getDecoder().decode(b64) }.onSuccess { bytes ->
                                chunks[d.optInt("index")] = bytes
                            }
                        }
                    }
                    TehLinkResponseParser.isTehLinkLine(trimmed) -> replies.add(trimmed)
                }
            }
        }

        try {
            withTimeout(90_000L) {
                delay(80)
                replies.clear()
                chunks.clear()
                val outbound = secureSession?.encryptOutbound(outboundJson) ?: outboundJson
                transport.sendCommand(outbound).getOrElse {
                    return@withTimeout Result.failure(it)
                }
                val deadline = System.currentTimeMillis() + 85_000L
                while (System.currentTimeMillis() < deadline) {
                    val match = replies.firstOrNull { line ->
                        val plain = secureSession?.decryptInbound(line) ?: line
                        runCatching { JSONObject(plain).optInt("id") == id }.getOrDefault(false)
                    }
                    if (match != null) {
                        val plain = secureSession?.decryptInbound(match) ?: match
                        val root = JSONObject(plain)
                        if (!root.optBoolean("ok")) {
                            return@withTimeout Result.failure(
                                Exception(root.optString("error", "download_failed"))
                            )
                        }
                        val ordered = chunks.toSortedMap()
                        val out = ordered.values.fold(ByteArray(0)) { acc, part -> acc + part }
                        return@withTimeout Result.success(out)
                    }
                    delay(30)
                }
                Result.failure(Exception("TEH-Link timeout"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("TEH-Link: ${e.message}"))
        } finally {
            job.cancel()
        }
    }

    /**
     * Guarda texto en microSD del T-Embed (`/xibalba/logs/<filename>`).
     * El firmware limita ~3500 bytes por llamada; usa [append] para chunking.
     */
    suspend fun sdSave(
        transport: TEmbedTransport,
        filename: String,
        data: String,
        append: Boolean = false
    ): Result<JSONObject> {
        val params = JSONObject()
            .put("filename", filename)
            .put("data", data)
            .put("append", append)
        return execute(transport, "sd.save", JSONObject().put("params", params), timeoutMs = 12_000L)
    }

    suspend fun rfScanStart(
        transport: TEmbedTransport,
        freqStart: Double,
        freqEnd: Double,
        step: Double = 0.25,
        rssiThreshold: Int = -100,
        dwellMs: Int = 5,
        maxHz: Int = 25
    ): Result<JSONObject> {
        val params = JSONObject()
            .put("freq_start", freqStart)
            .put("freq_end", freqEnd)
            .put("step", step)
            .put("rssi_threshold", rssiThreshold)
            .put("dwell_ms", dwellMs)
            .put("max_hz", maxHz)
        return execute(transport, "rf.scan.start", JSONObject().put("params", params))
    }

    suspend fun rfScanStop(transport: TEmbedTransport): Result<JSONObject> {
        return execute(transport, "rf.scan.stop")
    }

    suspend fun rfScanStatus(transport: TEmbedTransport): Result<JSONObject> {
        return execute(transport, "rf.scan.status")
    }

    suspend fun rfJammerStart(
        transport: TEmbedTransport,
        freqMhz: Double,
        power: Int = 10,
        mode: String = "continuous",
        burstInterval: Int? = null,
        maxSeconds: Int = 30
    ): Result<JSONObject> {
        val params = JSONObject()
            .put("freq", freqMhz)
            .put("power", power.coerceIn(1, 12))
            .put("mode", mode)
            .put("max_s", maxSeconds.coerceIn(1, 30))
        if (burstInterval != null) {
            params.put("burst_interval", burstInterval.coerceIn(5, 2000))
        }
        return execute(transport, "rf.jammer.start", JSONObject().put("params", params))
    }

    suspend fun rfJammerStop(transport: TEmbedTransport): Result<JSONObject> {
        return execute(transport, "rf.jammer.stop")
    }

    suspend fun rfJammerStatus(transport: TEmbedTransport): Result<JSONObject> {
        return execute(transport, "rf.jammer.status")
    }

    suspend fun sendRawJson(
        transport: TEmbedTransport,
        json: String,
        timeoutMs: Long = 5_000L
    ): Result<String> = linkMutex.withLock {
        val trimmed = json.trim()
        val id = TehLinkResponseParser.validateRawRequest(trimmed).getOrElse {
            return@withLock Result.failure(it)
        }
        val payload = injectAuthIfNeeded(trimmed)

        val buffer = mutableListOf<String>()
        val job = scope.launch {
            transport.incomingLines().collect { line ->
                if (TehLinkResponseParser.isTehLinkLine(line)) {
                    buffer.add(line.trim())
                }
            }
        }

        try {
            withTimeout(timeoutMs) {
                delay(80)
                buffer.clear()
                val outbound = secureSession?.encryptOutbound(payload) ?: payload
                transport.sendCommand(outbound).getOrElse {
                    return@withTimeout Result.failure(it)
                }

                val deadline = System.currentTimeMillis() + (timeoutMs - 500L).coerceAtLeast(2_000L)
                while (System.currentTimeMillis() < deadline) {
                    val match = buffer.firstOrNull { line ->
                        val plain = secureSession?.decryptInbound(line) ?: line
                        runCatching { JSONObject(plain).optInt("id") == id }.getOrDefault(false)
                    }
                    if (match != null) {
                        val plain = secureSession?.decryptInbound(match) ?: match
                        return@withTimeout Result.success(plain)
                    }
                    delay(30)
                }
                Result.failure(Exception("TEH-Link timeout"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("TEH-Link: ${e.message}"))
        } finally {
            job.cancel()
        }
    }

    /** Used by [TehLinkOtaUploader] and other trusted internal callers. */
    suspend fun executeCommand(
        transport: TEmbedTransport,
        cmd: String,
        args: JSONObject? = null,
        timeoutMs: Long = 5_000L
    ): Result<JSONObject> = execute(transport, cmd, args, timeoutMs)

    private suspend fun execute(
        transport: TEmbedTransport,
        cmd: String,
        args: JSONObject? = null,
        timeoutMs: Long = timeoutForCommand(cmd, args)
    ): Result<JSONObject> {
        val id = requestId.incrementAndGet()
        val payload = JSONObject().put("cmd", cmd).put("id", id)
        if (authToken.isNotBlank() && cmd !in PUBLIC_CMDS) {
            payload.put("auth", authToken)
        }
        args?.keys()?.forEach { key ->
            payload.put(key, args.get(key))
        }
        return sendRawJson(transport, payload.toString(), timeoutMs).mapCatching { line ->
            val root = JSONObject(line)
            if (!root.optBoolean("ok")) {
                throw Exception(root.optString("error", "teh_link_error"))
            }
            root.optJSONObject("data") ?: JSONObject()
        }
    }

    private fun timeoutForCommand(cmd: String, args: JSONObject?): Long = when (cmd) {
        "run_action" -> timeoutForAction(args?.optString("action").orEmpty(), args?.optJSONObject("params"))
        "ota_begin" -> 15_000L
        "ota_chunk" -> 30_000L
        "ota_finish" -> 60_000L
        "sd.save" -> 12_000L
        "list_files" -> 12_000L
        "download_file" -> 90_000L
        "rf.scan.start", "rf.jammer.start" -> 8_000L
        else -> 5_000L
    }

    private fun timeoutForAction(action: String, params: JSONObject?): Long {
        if (action == "scan_start" || action == "capture_start" || action == "rx_start") {
            val seconds = (params?.optInt("seconds", 10) ?: 10).coerceIn(1, 120)
            return ((seconds + 15).coerceAtMost(320).coerceAtLeast(15)) * 1000L
        }
        return 5_000L
    }

    private fun injectAuthIfNeeded(json: String): String {
        return runCatching {
            val obj = JSONObject(json.trim())
            val cmd = obj.optString("cmd")
            if (authToken.isNotBlank() && cmd !in PUBLIC_CMDS && !obj.has("auth")) {
                obj.put("auth", authToken)
            }
            obj.toString()
        }.getOrDefault(json)
    }

    companion object {
        private val PUBLIC_CMDS = setOf("ping", "get_info", "pair", "secure_handshake")
    }
}
