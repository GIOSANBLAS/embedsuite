package com.embedsuite.app.connection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class WifiTransport(
    private var host: String = DEFAULT_HOST
) : TEmbedTransport {

    override val type = TransportType.WIFI

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val _incoming = MutableSharedFlow<String>(extraBufferCapacity = 64)
    private var connected = false

    override val isConnected: Boolean
        get() = connected

    fun updateHost(newHost: String) {
        val cleaned = newHost.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')
        require(isValidHost(cleaned)) {
            "Host WiFi inválido: use hostname, IPv4 o IPv6 sin esquema"
        }
        host = cleaned
    }

    override suspend fun connect(): Result<String> = withContext(Dispatchers.IO) {
        val candidates = listOf(host, DEFAULT_HOST).distinct()
        for (candidate in candidates) {
            try {
                val request = Request.Builder()
                    .url("http://$candidate/")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        host = candidate
                        connected = true
                        _incoming.tryEmit("[WIFI] Conectado a T-Embed en $candidate")
                        return@withContext Result.success("WiFi: $candidate")
                    }
                }
            } catch (_: Exception) {
                // try next host
            }
        }
        Result.failure(Exception("No se alcanzó el dispositivo por WiFi. Verifica IP/host."))
    }

    override suspend fun disconnect() {
        connected = false
    }

    override suspend fun sendCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        if (!connected) {
            return@withContext Result.failure(Exception("WiFi no conectado."))
        }

        val payload = command.trim()
        if (payload.isBlank()) {
            return@withContext Result.failure(Exception("Comando vacío."))
        }

        try {
            val body = FormBody.Builder()
                .add("cmnd", payload)
                .build()

            val request = Request.Builder()
                .url("http://$host/cm")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    responseBody.lines().filter { it.isNotBlank() }.forEach { _incoming.tryEmit(it) }
                    Result.success(responseBody.ifBlank { "OK" })
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $responseBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun incomingLines(): Flow<String> = _incoming.asSharedFlow()

    companion object {
        const val DEFAULT_HOST = "192.168.4.1"

        private val HOST_PATTERN = Regex(
            """^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*|(?:\d{1,3}\.){3}\d{1,3}|\[[0-9a-fA-F:]+\])$"""
        )

        fun isValidHost(host: String): Boolean {
            if (host.isBlank() || host.length > 253) return false
            if (host.contains("..") || host.contains('/') || host.contains('\\')) return false
            return HOST_PATTERN.matches(host)
        }
    }
}
