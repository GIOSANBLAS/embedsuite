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

class GeminiAiProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun chat(
        apiKey: String,
        userMessage: String,
        context: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("API key de Gemini no configurada."))
        }

        try {
            val systemPrompt = """
                Eres EMBED AI, asistente técnico para LilyGO T-Embed CC1101 con firmware Bruce.
                Respondes en español, estilo hacker/conciso.
                Comandos Serial documentados: subghz rx/tx/tx_from_file, ir rx/tx/tx_from_file, storage list/read, info, free, uptime, webui, i2c scan, settings.
                NO inventes nfc, ble scan, subghz reset, setfrequency ni rx 0 (no están en la wiki Serial).
                Si el usuario pide una acción, sugiere el comando Bruce exacto al final en formato: CMD: comando_aqui
                Contexto del dispositivo: $context
            """.trimIndent()

            val body = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().apply {
                        put("parts", JSONArray().put(
                            JSONObject().put("text", "$systemPrompt\n\nUsuario: $userMessage")
                        ))
                    }
                ))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                    put("maxOutputTokens", 512)
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent")
                .addHeader("x-goog-api-key", apiKey)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Gemini error ${response.code}: $responseBody"))
                }

                val json = JSONObject(responseBody)
                val text = json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?: "Sin respuesta de Gemini."

                Result.success(text)
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red Gemini: ${e.message}"))
        } catch (e: JSONException) {
            Result.failure(Exception("Respuesta Gemini inválida: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun extractCommand(geminiResponse: String): String? {
        val cmdLine = geminiResponse.lines().lastOrNull { it.trim().startsWith("CMD:", ignoreCase = true) }
            ?: geminiResponse.lines().firstOrNull { it.contains("CMD:", ignoreCase = true) }
        return cmdLine?.substringAfter("CMD:")?.substringAfter("cmd:")?.trim()?.takeIf { it.isNotBlank() }
    }
}
