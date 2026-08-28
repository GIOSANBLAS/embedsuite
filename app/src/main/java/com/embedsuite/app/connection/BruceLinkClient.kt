package com.embedsuite.app.connection

import com.embedsuite.app.core.bruce.BruceCli
import com.embedsuite.app.core.bruce.BruceCliSystemParser
import com.embedsuite.app.core.bruce.BruceGatt
import com.embedsuite.app.core.bruce.BruceLimits
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject

/**
 * Cliente alineado con Bruce stock — CLI serial vía BLE, USB o WebUI WiFi (/cm).
 * Solo comandos documentados en https://github.com/BruceDevices/firmware/wiki/Serial
 */
class BruceLinkClient(
    @Suppress("unused") private val scope: CoroutineScope
) {
    var authToken: String = ""

    suspend fun ping(transport: TEmbedTransport): Result<Boolean> {
        if (!transport.isConnected) return Result.failure(Exception("Sin transporte Bruce."))
        return sendCli(transport, "info", 3_000L).map { it.contains("Bruce", ignoreCase = true) }
    }

    fun activateSecureSession() = Unit

    suspend fun establishSecureSession(transport: TEmbedTransport): Result<Unit> =
        if (transport.isConnected) Result.success(Unit)
        else Result.failure(IllegalStateException("Transporte no conectado"))

    suspend fun pair(transport: TEmbedTransport): Result<String> =
        Result.success("")

    suspend fun getInfo(transport: TEmbedTransport): Result<TehLinkDeviceInfo> {
        if (!transport.isConnected) return notConnected()
        val response = sendCli(transport, "info", 5_000L).getOrElse { return Result.failure(it) }
        val parsed = BruceCliSystemParser.parseInfo(response)
        val batteryPct = readBatteryPercent(transport)
        val sdMounted = parsed.sdMentioned || !response.contains("No SD", ignoreCase = true)
        return Result.success(
            TehLinkDeviceInfo(
                product = "Bruce",
                version = parsed.version.ifBlank { parseBruceVersion(response) },
                codename = "bruce",
                channel = transport.type.name.lowercase(),
                proto = "bruce-cli",
                protoVer = 0,
                plugins = emptyList(),
                hardware = parsed.deviceName.ifBlank { parseDeviceName(response).ifBlank { "T-Embed CC1101 Plus" } },
                firmware = parsed.version.ifBlank { parseBruceVersion(response) },
                battery = batteryPct?.let { TehLinkBatteryInfo(percentage = it) },
                sdStatus = if (sdMounted) "mounted" else "unknown"
            )
        )
    }

    suspend fun getStatus(transport: TEmbedTransport): Result<TehLinkDeviceStatus> {
        if (!transport.isConnected) return notConnected()
        val batteryPct = readBatteryPercent(transport)
        val uptimeLine = sendCli(transport, "uptime", 2_500L).getOrNull().orEmpty()
        val freeLine = sendCli(transport, "free", 2_500L).getOrNull().orEmpty()
        val sdLine = sendCli(transport, "storage free sd", 2_500L).getOrNull().orEmpty()
        val freeParsed = BruceCliSystemParser.parseFree(freeLine)
        val sdParsed = BruceCliSystemParser.parseSdFree(sdLine)
        return Result.success(
            TehLinkDeviceStatus(
                sdMounted = sdParsed.mounted,
                flashMounted = true,
                uiScreen = "Bruce (local)",
                uptimeMs = parseUptimeMs(uptimeLine),
                sim = emptyMap(),
                batteryPct = batteryPct,
                heapFreeBytes = freeParsed.heapFreeBytes,
                psramFreeBytes = freeParsed.psramFreeBytes,
                sdFreeBytes = sdParsed.freeBytes
            )
        )
    }

    suspend fun getScreen(transport: TEmbedTransport): Result<TehLinkScreenInfo> =
        if (transport.isConnected) {
            Result.success(TehLinkScreenInfo(uiScreen = "Bruce (local)", activePlugin = ""))
        } else notConnected()

    suspend fun openPlugin(transport: TEmbedTransport, pluginId: String): Result<TehLinkScreenInfo> {
        if (!transport.isConnected) return notConnected()
        val app = when (pluginId) {
            "wifi_toolkit" -> BruceCli.LoaderApps.WIFI
            "ble_toolkit" -> BruceCli.LoaderApps.BLE
            "ir_toolkit" -> BruceCli.LoaderApps.IR
            "nfc_toolkit", "nfc_clone" -> BruceCli.LoaderApps.RFID
            "subghz_analyzer" -> BruceCli.LoaderApps.RF
            else -> return Result.failure(Exception("Plugin '$pluginId': abrir solo apps del menú Bruce."))
        }
        return sendCli(transport, "loader open $app").map {
            TehLinkScreenInfo(uiScreen = app, activePlugin = pluginId, openedPluginId = pluginId)
        }
    }

    suspend fun backToMenu(transport: TEmbedTransport): Result<TehLinkScreenInfo> {
        if (!transport.isConnected) return notConnected()
        return sendCli(transport, BruceCli.Nav.ESC).map {
            TehLinkScreenInfo(uiScreen = "Inicio", activePlugin = "", openedPluginId = "")
        }
    }

    suspend fun listActions(transport: TEmbedTransport): Result<List<TehLinkActionInfo>> {
        if (!transport.isConnected) return notConnected()
        return Result.success(SUPPORTED_ACTIONS)
    }

    suspend fun getActionState(
        transport: TEmbedTransport,
        pluginId: String,
        action: String? = null
    ): Result<TehLinkActionState> = Result.success(
        TehLinkActionState(
            pluginId = pluginId,
            action = action.orEmpty(),
            state = "idle",
            message = "Estado en vivo no expuesto por CLI Bruce"
        )
    )

    suspend fun runAction(
        transport: TEmbedTransport,
        pluginId: String,
        action: String,
        params: JSONObject = JSONObject()
    ): Result<TehLinkActionResult> {
        if (!transport.isConnected) return notConnected()
        TehLinkActionPolicy.validate(pluginId, action).getOrElse { return Result.failure(it) }
        val cli = buildCliCommand(pluginId, action, params)
            ?: return Result.failure(Exception("Sin CLI Bruce para $pluginId/$action"))
        return sendCli(transport, cli, timeoutFor(action)).map { response ->
            actionOk(pluginId, action, response.ifBlank { cli })
        }
    }

    suspend fun runWifiScan(transport: TEmbedTransport, seconds: Int): Result<TehLinkActionResult> =
        unsupportedAction("wifi_toolkit", "scan_start")

    suspend fun runWardrivingStart(
        transport: TEmbedTransport,
        latitude: Double?,
        longitude: Double?,
        altitudeM: Double?
    ): Result<TehLinkActionResult> = unsupportedAction("wardriving", "start")

    suspend fun runWardrivingGpsUpdate(
        transport: TEmbedTransport,
        latitude: Double,
        longitude: Double,
        altitudeM: Double?
    ): Result<TehLinkActionResult> = unsupportedAction("wardriving", "gps_update")

    suspend fun runNfcRead(transport: TEmbedTransport): Result<TehLinkActionResult> =
        runAction(transport, "nfc_toolkit", "read")

    suspend fun runIrSend(
        transport: TEmbedTransport,
        protocol: String,
        address: String,
        command: String
    ): Result<TehLinkActionResult> = runAction(
        transport, "ir_toolkit", "send",
        JSONObject().put("protocol", protocol).put("address", address).put("command", command)
    )

    suspend fun runIrSendRaw(transport: TEmbedTransport, raw: String): Result<TehLinkActionResult> =
        runAction(transport, "ir_toolkit", "send", JSONObject().put("raw", raw))

    suspend fun runBleScan(transport: TEmbedTransport, seconds: Int): Result<TehLinkActionResult> =
        unsupportedAction("ble_toolkit", "scan_start")

    suspend fun runCryptoHash(
        transport: TEmbedTransport,
        input: String,
        algo: String
    ): Result<TehLinkActionResult> = unsupportedAction("crypto_toolkit", "hash")

    suspend fun runCryptoBase64Encode(
        transport: TEmbedTransport,
        input: String
    ): Result<TehLinkActionResult> = unsupportedAction("crypto_toolkit", "base64_encode")

    suspend fun runGenPassword(
        transport: TEmbedTransport,
        length: Int
    ): Result<TehLinkActionResult> = unsupportedAction("crypto_toolkit", "gen_password")

    suspend fun runSubGhzTx(
        transport: TEmbedTransport,
        rawHex: String,
        freqMhz: Double?
    ): Result<TehLinkActionResult> {
        val params = JSONObject().put("raw", rawHex)
        if (freqMhz != null) params.put("freq_mhz", freqMhz)
        return runAction(transport, "subghz_analyzer", "subghz_tx", params)
    }

    suspend fun runSubGhzReplay(
        transport: TEmbedTransport,
        devicePath: String
    ): Result<TehLinkActionResult> = runAction(
        transport, "subghz_analyzer", "subghz_replay",
        JSONObject().put("path", devicePath)
    )

    suspend fun runIrRxStart(
        transport: TEmbedTransport,
        seconds: Int
    ): Result<TehLinkActionResult> = runAction(
        transport, "ir_toolkit", "rx_start",
        JSONObject().put("seconds", seconds.coerceIn(1, 60))
    )

    suspend fun runNfcEmulate(
        transport: TEmbedTransport,
        uid: String
    ): Result<TehLinkActionResult> = runAction(
        transport, "nfc_toolkit", "emulate", JSONObject().put("uid", uid)
    )

    suspend fun runEvilPortalStart(
        transport: TEmbedTransport,
        ssid: String,
        templateId: String,
        channel: Int
    ): Result<TehLinkActionResult> = unsupportedAction("evil_portal", "start")

    suspend fun runEvilPortalStop(transport: TEmbedTransport): Result<TehLinkActionResult> =
        unsupportedAction("evil_portal", "stop")

    suspend fun runEvilPortalCreds(transport: TEmbedTransport): Result<TehLinkActionResult> =
        unsupportedAction("evil_portal", "creds")

    suspend fun runEvilPortalClearCreds(transport: TEmbedTransport): Result<TehLinkActionResult> =
        unsupportedAction("evil_portal", "clear_creds")

    suspend fun runEvilPortalStatus(transport: TEmbedTransport): Result<TehLinkActionResult> =
        unsupportedAction("evil_portal", "status")

    suspend fun runBeaconSpamStart(
        transport: TEmbedTransport,
        spec: String,
        hz: Int,
        channel: Int
    ): Result<TehLinkActionResult> = unsupportedAction("beacon_spam", "start")

    suspend fun runBeaconSpamStop(transport: TEmbedTransport): Result<TehLinkActionResult> =
        unsupportedAction("beacon_spam", "stop")

    suspend fun runBeaconSpamStatus(transport: TEmbedTransport): Result<TehLinkActionResult> =
        unsupportedAction("beacon_spam", "status")

    suspend fun runNrf24Status(transport: TEmbedTransport): Result<TehLinkActionResult> =
        unsupportedAction("nrf24_toolkit", "status")

    suspend fun runNrf24Tx(transport: TEmbedTransport, hex: String): Result<TehLinkActionResult> =
        unsupportedAction("nrf24_toolkit", "tx")

    suspend fun getOtaStatus(transport: TEmbedTransport): Result<TehLinkOtaStatus> =
        Result.failure(Exception(BruceLimits.NO_OTA))

    suspend fun clearCoredump(transport: TEmbedTransport): Result<Boolean> =
        Result.failure(Exception(BruceLimits.NO_CLI))

    suspend fun runSoakStress(
        transport: TEmbedTransport,
        iterations: Int,
        perStepSeconds: Int
    ): Result<TehLinkSoakResult> = Result.failure(Exception(BruceLimits.NO_CLI))

    suspend fun runNfcEmulateStop(transport: TEmbedTransport): Result<TehLinkActionResult> =
        runAction(transport, "nfc_toolkit", "emulate_stop")

    suspend fun runBadUsbFromFile(transport: TEmbedTransport, path: String): Result<TehLinkActionResult> {
        if (!transport.isConnected) return notConnected()
        val normalized = path.trim().let { if (it.startsWith("/")) it else "/$it" }
        return sendCli(transport, "badusb run_from_file $normalized", 30_000L).map { response ->
            actionOk("badusb", "run_from_file", response.ifBlank { normalized })
        }
    }

    suspend fun syncTime(transport: TEmbedTransport, timestampNs: Long): Result<JSONObject> =
        Result.failure(Exception("Bruce stock no expone time.sync por CLI."))

    suspend fun getTime(transport: TEmbedTransport): Result<JSONObject> {
        if (!transport.isConnected) return notConnected()
        return sendCli(transport, "date").map { line ->
            JSONObject()
                .put("synced", !line.contains("not set", ignoreCase = true))
                .put("time", line)
        }
    }

    suspend fun audioBeep(
        transport: TEmbedTransport,
        freqHz: Int,
        durationMs: Int
    ): Result<JSONObject> = audioTone(transport, freqHz, durationMs)

    suspend fun audioTone(
        transport: TEmbedTransport,
        freqHz: Int,
        durationMs: Int
    ): Result<JSONObject> {
        if (!transport.isConnected) return notConnected()
        val freq = freqHz.coerceIn(20, 12_000)
        val dur = durationMs.coerceIn(10, 1_500)
        return sendCli(transport, "tone $freq $dur").map {
            JSONObject().put("ok", true).put("freq_hz", freq).put("duration_ms", dur)
        }
    }

    suspend fun nfcWrite(
        transport: TEmbedTransport,
        hexData: String?,
        url: String?,
        block: Int
    ): Result<JSONObject> {
        if (!transport.isConnected) return notConnected()
        return when {
            !url.isNullOrBlank() -> sendCli(transport, "rfid ndef url $url").map { jsonOk("url", url) }
            !hexData.isNullOrBlank() -> sendCli(transport, "rfid write").map { jsonOk("block", block) }
            else -> Result.failure(IllegalArgumentException("rfid write requiere data o url"))
        }
    }

    suspend fun nfcReadFlat(transport: TEmbedTransport, timeoutMs: Int): Result<JSONObject> {
        if (!transport.isConnected) return notConnected()
        val timeout = timeoutMs.coerceIn(500, 30_000)
        return sendCli(transport, "rfid read $timeout", timeout + 5_000L).map { jsonOk("raw", it) }
    }

    suspend fun sdStatus(transport: TEmbedTransport): Result<JSONObject> {
        if (!transport.isConnected) return notConnected()
        return sendCli(transport, "storage free sd").map { line ->
            val parsed = BruceCliSystemParser.parseSdFree(line)
            JSONObject()
                .put("mounted", parsed.mounted)
                .put("free_bytes", parsed.freeBytes ?: JSONObject.NULL)
                .put("used_bytes", parsed.usedBytes ?: JSONObject.NULL)
                .put("total_bytes", parsed.totalBytes ?: JSONObject.NULL)
                .put("raw", line)
        }
    }

    suspend fun sdList(transport: TEmbedTransport, path: String): Result<JSONObject> =
        listFiles(transport, path)

    suspend fun listFiles(transport: TEmbedTransport, path: String): Result<JSONObject> {
        if (!transport.isConnected) return notConnected()
        val p = path.trim().ifBlank { "/" }
        return sendCli(transport, "storage list $p", 8_000L).map { line ->
            JSONObject().put("path", p).put("raw", line)
        }
    }

    suspend fun storageRead(transport: TEmbedTransport, path: String): Result<String> {
        if (!transport.isConnected) return notConnected()
        val p = path.trim().let { if (it.startsWith("/")) it else "/$it" }
        return sendCli(transport, "storage read $p", 30_000L)
    }

    suspend fun downloadFile(transport: TEmbedTransport, path: String): Result<ByteArray> {
        if (!transport.isConnected) return notConnected()
        return storageRead(transport, path).map { raw ->
            com.embedsuite.app.core.bruce.BruceStorageParser.extractFileContent(raw).toByteArray(Charsets.UTF_8)
        }
    }

    suspend fun sdSave(
        transport: TEmbedTransport,
        filename: String,
        data: String,
        append: Boolean
    ): Result<JSONObject> = Result.failure(Exception(BruceLimits.NO_FILE_UPLOAD_BLE))

    suspend fun rfScanStart(
        transport: TEmbedTransport,
        freqStart: Double,
        freqEnd: Double,
        step: Double,
        rssiThreshold: Int,
        dwellMs: Int,
        maxHz: Int
    ): Result<JSONObject> {
        if (!transport.isConnected) return notConnected()
        val startHz = BruceCli.mhzToHz(freqStart)
        val endHz = BruceCli.mhzToHz(freqEnd)
        return sendCli(transport, "subghz scan $startHz $endHz", 15_000L).map { line ->
            JSONObject().put("ok", true).put("start_hz", startHz).put("end_hz", endHz).put("raw", line)
        }
    }

    suspend fun rfScanStop(transport: TEmbedTransport): Result<JSONObject> =
        Result.failure(Exception("Bruce stock no expone subghz scan stop por CLI."))

    suspend fun rfScanStatus(transport: TEmbedTransport): Result<JSONObject> =
        Result.failure(Exception("Bruce stock no expone rf.scan.status por CLI."))

    suspend fun rfJammerStart(
        transport: TEmbedTransport,
        freqMhz: Double,
        power: Int,
        mode: String,
        burstInterval: Int?,
        maxSeconds: Int
    ): Result<JSONObject> = Result.failure(Exception(BruceLimits.NO_CLI))

    suspend fun rfJammerStop(transport: TEmbedTransport): Result<JSONObject> =
        Result.failure(Exception(BruceLimits.NO_CLI))

    suspend fun rfJammerStatus(transport: TEmbedTransport): Result<JSONObject> =
        Result.failure(Exception(BruceLimits.NO_CLI))

    suspend fun sendRawJson(
        transport: TEmbedTransport,
        json: String,
        timeoutMs: Long = 5_000L,
        @Suppress("unused") plaintext: Boolean = false
    ): Result<String> {
        if (!transport.isConnected) return notConnected()
        val trimmed = json.trim()
        if (!trimmed.startsWith("{")) {
            return sendCli(transport, trimmed, timeoutMs)
        }
        val obj = runCatching { JSONObject(trimmed) }.getOrElse {
            return sendCli(transport, trimmed, timeoutMs)
        }
        val cmd = obj.optString("cmd")
        if (cmd.isBlank()) return sendCli(transport, trimmed, timeoutMs)
        return executeCommand(transport, cmd, obj.optJSONObject("params") ?: obj.optJSONObject("args"), timeoutMs)
            .fold(
                onSuccess = { Result.success(it.toString()) },
                onFailure = {
                    Result.failure(
                        IllegalArgumentException("${BruceLimits.JSON_REJECTED} (cmd=$cmd)")
                    )
                }
            )
    }

    suspend fun executeCommand(
        transport: TEmbedTransport,
        cmd: String,
        args: JSONObject?,
        timeoutMs: Long = 5_000L
    ): Result<JSONObject> {
        if (!transport.isConnected) return notConnected()
        return when (cmd) {
            "ping" -> Result.success(JSONObject().put("pong", true).put("proto", "bruce-cli"))
            "get_info" -> getInfo(transport).map { info ->
                JSONObject()
                    .put("product", info.product)
                    .put("version", info.version)
                    .put("firmware", info.firmware)
                    .put("hardware", info.hardware)
            }
            "get_status" -> getStatus(transport).map { status ->
                JSONObject()
                    .put("ui_screen", status.uiScreen)
                    .put("battery_pct", status.batteryPct ?: JSONObject.NULL)
                    .put("sd_mounted", status.sdMounted)
            }
            "audio.beep", "audio.tone" -> {
                val p = args ?: JSONObject()
                audioTone(
                    transport,
                    p.optInt("freq_hz", p.optInt("freq", 1000)),
                    p.optInt("duration_ms", p.optInt("duration", 100))
                )
            }
            "nfc.read" -> nfcReadFlat(transport, args?.optInt("timeout", 5_000) ?: 5_000)
            "nfc.write" -> nfcWrite(
                transport,
                hexData = args?.optString("data"),
                url = args?.optString("url"),
                block = args?.optInt("block", 1) ?: 1
            )
            "sd.status" -> sdStatus(transport)
            "sd.list" -> sdList(transport, args?.optString("path", "/") ?: "/")
            "list_files" -> listFiles(transport, args?.optString("path", "/") ?: "/")
            "time.get" -> getTime(transport)
            "run_action" -> {
                val pluginId = args?.optString("plugin_id").orEmpty()
                val action = args?.optString("action").orEmpty()
                val params = args?.optJSONObject("params") ?: JSONObject()
                runAction(transport, pluginId, action, params).map { result ->
                    JSONObject()
                        .put("plugin_id", result.pluginId)
                        .put("action", result.action)
                        .put("state", result.state.state)
                        .put("message", result.state.message)
                }
            }
            "ota_begin", "ota_chunk", "ota_finish", "ota_abort", "ota_status",
            "coredump_clear", "time.sync", "sd.save", "download_file",
            "rf.scan.start" -> {
                val start = args?.optDouble("freq_start_mhz", 300.0) ?: 300.0
                val end = args?.optDouble("freq_end_mhz", 928.0) ?: 928.0
                rfScanStart(transport, start, end, 0.25, -100, 5, 25)
            }
            "rf.scan.stop", "rf.scan.status",
            "rf.jammer.start", "rf.jammer.stop", "rf.jammer.status" ->
                Result.failure(Exception(BruceLimits.NO_CLI))
            else -> Result.failure(IllegalArgumentException("Comando '$cmd' no disponible en Bruce CLI"))
        }
    }

    // ── internals ───────────────────────────────────────────────────────────

    private suspend fun sendCli(
        transport: TEmbedTransport,
        command: String,
        timeoutMs: Long = 4_000L
    ): Result<String> = BruceCli.sendAndCollect(transport, command, timeoutMs)

    private suspend fun readBatteryPercent(transport: TEmbedTransport): Int? =
        (transport as? BruceBleCapable)?.readBatteryLevel()

    private fun buildCliCommand(pluginId: String, action: String, params: JSONObject): String? {
        return when (pluginId to action) {
            "device" to "reboot" -> BruceGatt.Commands.REBOOT
            "device" to "poweroff" -> BruceGatt.Commands.POWER_OFF

            "ir_toolkit" to "send" -> {
                val path = params.optString("path")
                if (path.isNotBlank()) {
                    val normalized = path.trim().let { if (it.startsWith("/")) it else "/$it" }
                    "ir tx_from_file $normalized"
                } else {
                    val raw = params.optString("raw")
                    if (raw.isNotBlank()) {
                        val freq = params.optInt("frequency", params.optInt("freq", 38_000))
                        "ir tx_raw $freq $raw"
                    } else {
                        val protocol = params.optString("protocol")
                        val address = params.optString("address")
                        val command = params.optString("command")
                        if (protocol.isBlank() || address.isBlank() || command.isBlank()) null
                        else "ir tx $protocol $address $command"
                    }
                }
            }
            "ir_toolkit" to "rx_start", "ir_toolkit" to "capture" -> {
                val seconds = params.optInt("seconds", 10).coerceIn(1, 60)
                "ir rx $seconds"
            }

            "nfc_toolkit" to "read", "nfc_toolkit" to "read_mifare" -> {
                val timeout = params.optInt("timeout", 5_000)
                "rfid read $timeout"
            }
            "nfc_toolkit" to "write" -> "rfid write"
            "nfc_toolkit" to "emulate" -> {
                val uid = params.optString("uid")
                if (uid.isBlank()) "rfid emulate t4t" else "rfid emulate t4t text $uid"
            }
            "nfc_toolkit" to "emulate_stop" -> "rfid reset"

            "subghz_analyzer" to "subghz_replay" -> {
                val path = params.optString("path")
                if (path.isBlank()) null else "subghz tx_from_file $path false"
            }
            "subghz_analyzer" to "subghz_tx" -> {
                val key = params.optString("key").ifBlank { params.optString("raw") }
                val freqMhz = params.optDouble("freq_mhz", Double.NaN)
                val te = params.optInt("te", 174)
                val count = params.optInt("count", 10)
                if (key.isBlank() || freqMhz.isNaN()) null
                else {
                    val freqHz = BruceCli.mhzToHz(freqMhz)
                    "subghz tx $key $freqHz $te $count"
                }
            }
            "subghz_analyzer" to "capture_start" -> {
                val freqHz = params.optLong("freq_hz", 0L).takeIf { it > 0 }
                    ?: params.optDouble("freq_mhz", Double.NaN).let { if (it.isNaN()) null else BruceCli.mhzToHz(it) }
                    ?: 433_920_000L
                val seconds = params.optInt("seconds", 15).coerceIn(1, 120)
                "subghz rx $freqHz $seconds"
            }
            "subghz_analyzer" to "capture_stop" -> null

            "subghz_tools" to "spectrum_start" -> {
                val start = params.optDouble("freq_start_mhz", params.optDouble("freq_start", 300.0))
                val end = params.optDouble("freq_end_mhz", params.optDouble("freq_end", 928.0))
                "subghz scan ${BruceCli.mhzToHz(start)} ${BruceCli.mhzToHz(end)}"
            }

            else -> null
        }
    }

    private fun parseBruceVersion(response: String): String {
        response.lineSequence().forEach { line ->
            val marker = "Bruce v"
            val idx = line.indexOf(marker, ignoreCase = true)
            if (idx >= 0) {
                return line.substring(idx + marker.length).trim().substringBefore(' ')
            }
        }
        return ""
    }

    private fun parseDeviceName(response: String): String {
        response.lineSequence().forEach { line ->
            if (line.startsWith("Device:", ignoreCase = true)) {
                return line.substringAfter(":").trim()
            }
        }
        return ""
    }

    private fun parseUptimeMs(line: String): Long {
        val match = Regex("""(\d+):(\d+):(\d+)""").find(line) ?: return 0L
        val (h, m, s) = match.destructured
        return (h.toLong() * 3600 + m.toLong() * 60 + s.toLong()) * 1000L
    }

    private fun timeoutFor(action: String): Long = when (action) {
        "capture_start", "rx_start", "read" -> 20_000L
        "subghz_replay", "subghz_tx" -> 10_000L
        else -> 5_000L
    }

    private fun actionOk(pluginId: String, action: String, message: String) = TehLinkActionResult(
        pluginId = pluginId,
        action = action,
        state = TehLinkActionState(
            pluginId = pluginId,
            action = action,
            state = "ok",
            message = message,
            running = false
        )
    )

    private fun jsonOk(key: String, value: Any) = JSONObject().put("ok", true).put(key, value)

    private fun <T> notConnected(): Result<T> =
        Result.failure(Exception("Sin transporte Bruce conectado."))

    private fun unsupportedAction(pluginId: String, action: String): Result<TehLinkActionResult> =
        Result.failure(Exception(BruceLimits.NO_CLI))

    companion object {
        private val SUPPORTED_ACTIONS = listOf(
            TehLinkActionInfo("device", "reboot"),
            TehLinkActionInfo("ir_toolkit", "send"),
            TehLinkActionInfo("ir_toolkit", "rx_start"),
            TehLinkActionInfo("nfc_toolkit", "read"),
            TehLinkActionInfo("nfc_toolkit", "emulate"),
            TehLinkActionInfo("nfc_toolkit", "emulate_stop"),
            TehLinkActionInfo("subghz_analyzer", "subghz_replay", listOf("path")),
            TehLinkActionInfo("subghz_analyzer", "subghz_tx", listOf("key", "freq_mhz", "te", "count")),
            TehLinkActionInfo("subghz_analyzer", "capture_start", listOf("freq_mhz", "seconds")),
            TehLinkActionInfo("badusb", "run_from_file", listOf("path"))
        )
    }
}

interface BruceBleCapable {
    suspend fun readBatteryLevel(): Int?
}
