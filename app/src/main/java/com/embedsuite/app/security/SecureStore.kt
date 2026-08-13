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

    fun getTehLinkAuthToken(): String = prefs?.getString(KEY_TEH_LINK_AUTH, "").orEmpty()

    fun setTehLinkAuthToken(token: String) {
        prefs?.edit()?.putString(KEY_TEH_LINK_AUTH, token.trim())?.apply()
    }

    fun clearTehLinkAuthToken() {
        prefs?.edit()?.remove(KEY_TEH_LINK_AUTH)?.apply()
    }

    fun getTehLinkSessionId(): String = prefs?.getString(KEY_TEH_LINK_SESSION_ID, "").orEmpty()

    fun getTehLinkSessionKey(): ByteArray? {
        val b64 = prefs?.getString(KEY_TEH_LINK_SESSION_KEY, null) ?: return null
        return runCatching {
            android.util.Base64.decode(b64, android.util.Base64.NO_WRAP)
        }.getOrNull()
    }

    fun setTehLinkSessionKey(sessionId: String, key: ByteArray) {
        val b64 = android.util.Base64.encodeToString(key, android.util.Base64.NO_WRAP)
        prefs?.edit()
            ?.putString(KEY_TEH_LINK_SESSION_ID, sessionId)
            ?.putString(KEY_TEH_LINK_SESSION_KEY, b64)
            ?.apply()
    }

    fun clearTehLinkSession() {
        prefs?.edit()
            ?.remove(KEY_TEH_LINK_SESSION_ID)
            ?.remove(KEY_TEH_LINK_SESSION_KEY)
            ?.apply()
    }

    fun getGithubToken(): String = prefs?.getString(KEY_GITHUB_TOKEN, "").orEmpty()

    fun setGithubToken(token: String) {
        prefs?.edit()?.putString(KEY_GITHUB_TOKEN, token.trim())?.apply()
    }

    /** Passphrase SQLCipher para Room; se genera una vez y persiste cifrada. */
    fun getOrCreateDatabasePassphrase(): ByteArray {
        val existing = prefs?.getString(KEY_DB_PASSPHRASE, null)
        if (!existing.isNullOrBlank()) {
            return existing.toByteArray(Charsets.UTF_8)
        }
        val generated = buildString(PASSPHRASE_LENGTH) {
            val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            val random = java.security.SecureRandom()
            repeat(PASSPHRASE_LENGTH) { append(alphabet[random.nextInt(alphabet.length)]) }
        }
        prefs?.edit()?.putString(KEY_DB_PASSPHRASE, generated)?.apply()
        return generated.toByteArray(Charsets.UTF_8)
    }

    companion object {
        private const val TAG = "SecureStore"
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_TEH_LINK_AUTH = "teh_link_auth_token"
        private const val KEY_TEH_LINK_SESSION_ID = "teh_link_session_id"
        private const val KEY_TEH_LINK_SESSION_KEY = "teh_link_session_key"
        private const val KEY_GITHUB_TOKEN = "github_pat_firmware"
        private const val KEY_DB_PASSPHRASE = "room_db_passphrase"
        private const val PASSPHRASE_LENGTH = 32
    }
}
