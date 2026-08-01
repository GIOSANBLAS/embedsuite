package com.embedsuite.app.connection

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

/**
 * Cliente TEH-Link — JSON NDJSON sobre USB CDC (firmware T-Embed Xibalba).
 */
class TehLinkClient(
    private val scope: CoroutineScope
) {
    private val requestId = AtomicInteger(0)
    private val linkMutex = Mutex()

    var authToken: String = ""

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

    suspend fun runWardrivingStart(transport: TEmbedTransport): Result<TehLinkActionResult> {
        return runAction(transport, pluginId = "wardriving", action = "start")
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
                transport.sendCommand(payload).getOrElse {
                    return@withTimeout Result.failure(it)
                }

                val deadline = System.currentTimeMillis() + (timeoutMs - 500L).coerceAtLeast(2_000L)
                while (System.currentTimeMillis() < deadline) {
                    val match = buffer.firstOrNull { line ->
                        runCatching { JSONObject(line).optInt("id") == id }.getOrDefault(false)
                    }
                    if (match != null) {
                        return@withTimeout Result.success(match)
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
        else -> 5_000L
    }

    private fun timeoutForAction(action: String, params: JSONObject?): Long {
        if (action == "scan_start" || action == "capture_start") {
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
        private val PUBLIC_CMDS = setOf("ping", "get_info", "pair")
    }
}
