package com.embedsuite.app.connection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
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
        val candidates = listOf(host, "bruce.local", DEFAULT_HOST).distinct()
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
                        _incoming.tryEmit("[WIFI] Conectado a Bruce WebUI en $candidate")
                        return@withContext Result.success("WiFi: $candidate")
                    }
                }
            } catch (_: Exception) {
                // try next host
            }
        }
        Result.failure(Exception("No se alcanzó el WebUI de Bruce. Conéctate a BruceNet (192.168.4.1)."))
    }

    override suspend fun disconnect() {
        connected = false
    }

    override suspend fun sendCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        if (!connected) {
            return@withContext Result.failure(Exception("WiFi no conectado."))
        }

        val validated = BruceCommandValidator.validate(command).getOrElse {
            return@withContext Result.failure(it)
        }

        try {
            val body = FormBody.Builder()
                .add("cmnd", validated)
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

    suspend fun uploadFirmware(binFile: File, onProgress: (Int) -> Unit): Result<String> =
        withContext(Dispatchers.IO) {
            if (!connected) {
                return@withContext Result.failure(Exception("WiFi no conectado."))
            }

            try {
                onProgress(10)
                val endpoints = listOf("/update", "/ota", "/upload")
                var lastError = "Sin respuesta del servidor OTA."

                for (endpoint in endpoints) {
                    try {
                        onProgress(30)
                        val multipart = MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart(
                                "update",
                                binFile.name,
                                binFile.asRequestBody("application/octet-stream".toMediaType())
                            )
                            .build()
                        val request = Request.Builder()
                            .url("http://$host$endpoint")
                            .post(multipart)
                            .build()

                        client.newCall(request).execute().use { response ->
                            val body = response.body?.string().orEmpty()
                            if (response.isSuccessful) {
                                onProgress(100)
                                _incoming.tryEmit("[OTA] Firmware enviado correctamente. Reiniciando T-Embed...")
                                return@withContext Result.success(body.ifBlank { "OTA OK" })
                            }
                            lastError = "HTTP ${response.code}: $body"
                        }
                    } catch (e: Exception) {
                        lastError = e.message ?: lastError
                    }
                }

                Result.failure(Exception(lastError))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    companion object {
        const val DEFAULT_HOST = "192.168.4.1"
        /** SSID/contraseña por defecto del AP Bruce — ver strings.xml `bruce_net_*`. */
        const val BRUCE_NET_SSID = "BruceNet"
        const val BRUCE_NET_PASSWORD = "bruce32"

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
