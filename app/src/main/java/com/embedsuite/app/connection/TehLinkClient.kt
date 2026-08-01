package com.embedsuite.app.connection

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

    suspend fun ping(transport: TEmbedTransport): Result<Boolean> {
        return execute(transport, "ping").map { data ->
            data.optBoolean("pong") &&
                data.optString("proto") == "teh-link"
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
        return execute(transport, "run_action", args).map(TehLinkResponseParser::parseActionResult)
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
        return runAction(
            transport,
            pluginId = "wifi_toolkit",
            action = "scan_start",
            params = JSONObject().put("seconds", seconds)
        )
    }

    suspend fun runWardrivingStart(transport: TEmbedTransport): Result<TehLinkActionResult> {
        return runAction(transport, pluginId = "wardriving", action = "start")
    }

    suspend fun runBleScan(transport: TEmbedTransport, seconds: Int): Result<TehLinkActionResult> {
        return runAction(
            transport,
            pluginId = "ble_toolkit",
            action = "scan_start",
            params = JSONObject().put("seconds", seconds)
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

    /** Envía JSON TEH-Link crudo y devuelve la línea de respuesta con id coincidente. */
    suspend fun sendRawJson(transport: TEmbedTransport, json: String): Result<String> {
        val trimmed = json.trim()
        val id = TehLinkResponseParser.validateRawRequest(trimmed).getOrElse {
            return Result.failure(it)
        }

        val buffer = mutableListOf<String>()
        val job = scope.launch {
            transport.incomingLines().collect { line ->
                if (TehLinkResponseParser.isTehLinkLine(line)) {
                    buffer.add(line.trim())
                }
            }
        }

        return try {
            withTimeout(5_000L) {
                delay(80)
                buffer.clear()
                transport.sendCommand(trimmed).getOrElse {
                    return@withTimeout Result.failure(it)
                }

                val deadline = System.currentTimeMillis() + 4_000L
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
        args: JSONObject? = null
    ): Result<JSONObject> {
        val id = requestId.incrementAndGet()
        val payload = JSONObject().put("cmd", cmd).put("id", id)
        args?.keys()?.forEach { key ->
            payload.put(key, args.get(key))
        }
        return sendRawJson(transport, payload.toString()).mapCatching { line ->
            val root = JSONObject(line)
            if (!root.optBoolean("ok")) {
                throw Exception(root.optString("error", "teh_link_error"))
            }
            root.optJSONObject("data") ?: JSONObject()
        }
    }
}
