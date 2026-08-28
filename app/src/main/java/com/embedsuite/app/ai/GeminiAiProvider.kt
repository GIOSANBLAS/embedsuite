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
                Eres EMBED AI, asistente técnico para LilyGO T-Embed CC1101 Plus con firmware Bruce (TEH-Link).
                Respondes en español, estilo hacker/conciso.
                Acciones vía TEH-Link JSON: run_action en plugins (subghz_analyzer, ir_toolkit, nfc_toolkit, wifi_toolkit, wardriving).
                Comandos base: ping, get_info, get_status, run_action, open_plugin, back_to_menu.
                Si el usuario pide una acción, sugiere JSON TEH-Link al final en formato: CMD: {"cmd":"..."}
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
