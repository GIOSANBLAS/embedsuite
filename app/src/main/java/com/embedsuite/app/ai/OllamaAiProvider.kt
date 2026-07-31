package com.embedsuite.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OllamaAiProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun chat(
        baseUrl: String,
        model: String,
        userMessage: String,
        context: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val host = baseUrl.trim().trimEnd('/')
        if (host.isBlank()) {
            return@withContext Result.failure(Exception("URL de Ollama no configurada."))
        }
        if (model.isBlank()) {
            return@withContext Result.failure(Exception("Modelo Ollama no configurado."))
        }

        try {
            val systemPrompt = """
                Eres EMBED AI, asistente técnico para LilyGO T-Embed CC1101 con firmware Bruce.
                Respondes en español, estilo hacker/conciso.
                Comandos Serial documentados: subghz rx/tx/tx_from_file, ir rx/tx, storage list/read, info, free, uptime, webui, i2c scan, settings.
                NO inventes nfc, ble scan, subghz reset ni setfrequency.
                Si el usuario pide una acción, sugiere el comando Bruce exacto al final en formato: CMD: comando_aqui
                Contexto del dispositivo: $context
            """.trimIndent()

            val body = JSONObject().apply {
                put("model", model)
                put("stream", false)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    })
                })
            }

            val request = Request.Builder()
                .url("$host/api/chat")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Ollama error ${response.code}: $responseBody"))
                }

                val json = JSONObject(responseBody)
                val text = json.optJSONObject("message")?.optString("content")
                    ?: json.optString("response")
                    ?: "Sin respuesta de Ollama."

                Result.success(text)
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red Ollama: ${e.message}"))
        } catch (e: JSONException) {
            Result.failure(Exception("Respuesta Ollama inválida: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun extractCommand(response: String): String? {
        val cmdLine = response.lines().lastOrNull { it.trim().startsWith("CMD:", ignoreCase = true) }
            ?: response.lines().firstOrNull { it.contains("CMD:", ignoreCase = true) }
        return cmdLine?.substringAfter("CMD:")?.substringAfter("cmd:")?.trim()?.takeIf { it.isNotBlank() }
    }
}
