package com.embedsuite.app.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStore(context: Context) {

    private val prefs: SharedPreferences?
    val isAvailable: Boolean

    init {
        prefs = try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext,
                "embed_secure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences no disponible; claves API no persistirán cifradas", e)
            null
        }
        isAvailable = prefs != null
    }

    fun getGeminiApiKey(): String = prefs?.getString(KEY_GEMINI, "") ?: ""

    fun setGeminiApiKey(key: String) {
        prefs?.edit()?.putString(KEY_GEMINI, key.trim())?.apply()
    }

    companion object {
        private const val TAG = "SecureStore"
        private const val KEY_GEMINI = "gemini_api_key"
    }
}
