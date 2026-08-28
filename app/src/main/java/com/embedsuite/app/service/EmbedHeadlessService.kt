package com.embedsuite.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.embedsuite.app.EmbedApplication
import com.embedsuite.app.R
import com.embedsuite.app.connection.TEmbedTransport
import com.embedsuite.app.connection.BruceLinkClient
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/**
 * REST API local (127.0.0.1:8080) para control headless del T-Embed vía Bruce BLE CLI.
 */
class EmbedHeadlessService : Service() {

    private var server: HeadlessHttpServer? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        val container = (application as EmbedApplication).container
        val connectionManager = container.connectionManager
        val bruceLinkClient = connectionManager.bruceLinkClientForHeadless()

        server = HeadlessHttpServer(connectionManager, bruceLinkClient)
        server?.start(SOCKET_READ_TIMEOUT, false)
    }

    override fun onDestroy() {
        server?.stop()
        server = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class HeadlessHttpServer(
        private val connectionManager: com.embedsuite.app.connection.DeviceConnectionManager,
        private val bruceLinkClient: BruceLinkClient
    ) : NanoHTTPD(HOST, PORT) {

        override fun serve(session: IHTTPSession): Response {
            val apiKey = session.headers.entries
                .firstOrNull { it.key.equals("x-api-key", ignoreCase = true) }
                ?.value
                ?: session.headers["X-API-Key"]
            if (apiKey != API_KEY) {
                return jsonResponse(
                    Response.Status.UNAUTHORIZED,
                    JSONObject().put("error", "unauthorized")
                )
            }
            if (session.uri != "/api/v1/command" || session.method != Method.POST) {
                return jsonResponse(
                    Response.Status.NOT_FOUND,
                    JSONObject().put("error", "not_found")
                )
            }
            val body = readBody(session)
            val request = runCatching { JSONObject(body) }.getOrElse {
                return jsonResponse(
                    Response.Status.BAD_REQUEST,
                    JSONObject().put("error", "bad_json")
                )
            }
            val transport: TEmbedTransport = connectionManager.activeTransportForHeadless()
                ?: return jsonResponse(
                    Response.Status.SERVICE_UNAVAILABLE,
                    JSONObject().put("error", "device_not_connected")
                )
            val action = request.optString("action")
            val result = runBlocking {
                when (action) {
                    "rx_raw" -> {
                        val freq = request.optLong("freq", 433_920_000L)
                        val mod = request.optString("mod", "FSK")
                        val freqMhz = freq / 1_000_000.0
                        bruceLinkClient.runAction(
                            transport,
                            pluginId = "subghz_analyzer",
                            action = "capture_start",
                            params = JSONObject()
                                .put("seconds", 10)
                                .put("freq_mhz", freqMhz)
                                .put("mod", mod)
                        )
                    }
                    else -> {
                        val cmd = request.optString("cmd", action)
                        if (cmd.isBlank()) {
                            return@runBlocking Result.failure(Exception("missing_action"))
                        }
                        bruceLinkClient.sendRawJson(transport, cmd)
                    }
                }
            }
            return result.fold(
                onSuccess = { data ->
                    val payload = when (data) {
                        is org.json.JSONObject -> data
                        is com.embedsuite.app.connection.TehLinkActionResult ->
                            JSONObject().put("state", data.state.state).put("message", data.state.message)
                        else -> JSONObject().put("response", data.toString())
                    }
                    jsonResponse(Response.Status.OK, JSONObject().put("ok", true).put("data", payload))
                },
                onFailure = { err ->
                    jsonResponse(
                        Response.Status.INTERNAL_ERROR,
                        JSONObject().put("ok", false).put("error", err.message ?: "error")
                    )
                }
            )
        }

        private fun readBody(session: IHTTPSession): String {
            val files = HashMap<String, String>()
            session.parseBody(files)
            return files["postData"] ?: ""
        }

        private fun jsonResponse(status: Response.Status, body: JSONObject): Response {
            return newFixedLengthResponse(status, "application/json", body.toString())
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "embed_headless"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Embed Headless API",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("API headless activa en $HOST:$PORT")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_START = "com.embedsuite.app.action.START_HEADLESS"
        private const val HOST = "127.0.0.1"
        private const val PORT = 8080
        private const val API_KEY = "1234"
        private const val NOTIFICATION_ID = 8080
        private const val SOCKET_READ_TIMEOUT = 5000
    }
}
