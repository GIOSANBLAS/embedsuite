package com.embedsuite.app.ai

import android.content.Context
import android.content.SharedPreferences
import com.embedsuite.app.security.SecureStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AiPreferences(
    context: Context,
    private val secureStore: SecureStore? = null
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(loadMode())
    val mode: StateFlow<AiMode> = _mode.asStateFlow()

    private val _ollamaHost = MutableStateFlow(prefs.getString(KEY_OLLAMA_HOST, DEFAULT_OLLAMA_HOST) ?: DEFAULT_OLLAMA_HOST)
    val ollamaHost: StateFlow<String> = _ollamaHost.asStateFlow()

    private val _ollamaModel = MutableStateFlow(prefs.getString(KEY_OLLAMA_MODEL, DEFAULT_OLLAMA_MODEL) ?: DEFAULT_OLLAMA_MODEL)
    val ollamaModel: StateFlow<String> = _ollamaModel.asStateFlow()

    fun getGeminiApiKey(): String {
        val secure = secureStore?.getGeminiApiKey()?.takeIf { it.isNotBlank() }
        if (secure != null) return secure
        // Migración única desde prefs en claro (versiones antiguas) → SecureStore
        val legacy = prefs.getString(KEY_GEMINI_API, "")?.trim().orEmpty()
        if (legacy.isNotBlank()) {
            secureStore?.setGeminiApiKey(legacy)
            prefs.edit().remove(KEY_GEMINI_API).apply()
            return legacy
        }
        return ""
    }

    fun setGeminiApiKey(key: String) {
        secureStore?.setGeminiApiKey(key)
        prefs.edit().remove(KEY_GEMINI_API).apply()
    }

    fun getMode(): AiMode = _mode.value

    fun setMode(mode: AiMode) {
        prefs.edit().putString(KEY_AI_MODE, mode.name).apply()
        _mode.value = mode
    }

    fun isAutoExecute(): Boolean = prefs.getBoolean(KEY_AUTO_EXECUTE, false)

    fun setAutoExecute(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_EXECUTE, enabled).apply()
    }

    fun getOllamaHost(): String = _ollamaHost.value

    fun setOllamaHost(host: String) {
        val normalized = host.trim().ifBlank { DEFAULT_OLLAMA_HOST }
        prefs.edit().putString(KEY_OLLAMA_HOST, normalized).apply()
        _ollamaHost.value = normalized
    }

    fun getOllamaModel(): String = _ollamaModel.value

    fun setOllamaModel(model: String) {
        val normalized = model.trim().ifBlank { DEFAULT_OLLAMA_MODEL }
        prefs.edit().putString(KEY_OLLAMA_MODEL, normalized).apply()
        _ollamaModel.value = normalized
    }

    private fun loadMode(): AiMode {
        return try {
            AiMode.valueOf(prefs.getString(KEY_AI_MODE, AiMode.LOCAL.name) ?: AiMode.LOCAL.name)
        } catch (_: Exception) {
            AiMode.LOCAL
        }
    }

    companion object {
        private const val PREFS_NAME = "embed_ai_prefs"
        private const val KEY_GEMINI_API = "gemini_api_key"
        private const val KEY_AI_MODE = "ai_mode"
        private const val KEY_AUTO_EXECUTE = "auto_execute"
        private const val KEY_OLLAMA_HOST = "ollama_host"
        private const val KEY_OLLAMA_MODEL = "ollama_model"
        const val DEFAULT_OLLAMA_HOST = "http://192.168.1.100:11434"
        const val DEFAULT_OLLAMA_MODEL = "llama3.2"
    }
}
