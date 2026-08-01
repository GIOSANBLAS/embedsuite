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

    private suspend fun execute(transport: TEmbedTransport, cmd: String): Result<JSONObject> {
        val id = requestId.incrementAndGet()
        val payload = JSONObject().put("cmd", cmd).put("id", id).toString()
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
                transport.sendCommand(payload).getOrElse {
                    return@withTimeout Result.failure(it)
                }

                val deadline = System.currentTimeMillis() + 4_000L
                while (System.currentTimeMillis() < deadline) {
                    val match = buffer.firstOrNull { line ->
                        runCatching { JSONObject(line).optInt("id") == id }.getOrDefault(false)
                    }
                    if (match != null) {
                        val root = JSONObject(match)
                        if (!root.optBoolean("ok")) {
                            return@withTimeout Result.failure(
                                Exception(root.optString("error", "teh_link_error"))
                            )
                        }
                        return@withTimeout Result.success(root.optJSONObject("data") ?: JSONObject())
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
}
