package com.embedsuite.app.security

import android.util.Base64
import com.embedsuite.app.connection.TEmbedTransport
import com.embedsuite.app.connection.TehLinkClient
import org.json.JSONObject
import java.security.KeyPairGenerator
import java.security.KeyFactory
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement

/**
 * ECDH P-256 handshake for TEH-Link v3 optional payload encryption.
 *
 * Flow:
 * 1. App generates ephemeral EC key pair.
 * 2. `secure_handshake` with client public key (SPKI, base64).
 * 3. Device returns server public key + salt.
 * 4. ECDH → HKDF → AES-256-GCM session key.
 */
class TehLinkSecureSession(
    private val secureStore: SecureStore
) {
    data class SessionState(
        val sessionId: String,
        val sessionKey: ByteArray,
        val establishedAtMs: Long = System.currentTimeMillis()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SessionState) return false
            return sessionId == other.sessionId && sessionKey.contentEquals(other.sessionKey)
        }

        override fun hashCode(): Int {
            var result = sessionId.hashCode()
            result = 31 * result + sessionKey.contentHashCode()
            return result
        }
    }

    @Volatile
    var activeSession: SessionState? = null
        private set

    val isEstablished: Boolean get() = activeSession != null

    suspend fun handshake(client: TehLinkClient, transport: TEmbedTransport): Result<SessionState> {
        return runCatching {
            val kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(ECGenParameterSpec("secp256r1"))
            val keyPair = kpg.generateKeyPair()
            val clientPubB64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)

            val response = client.executeCommand(
                transport,
                "secure_handshake",
                JSONObject().put("client_pubkey", clientPubB64),
                timeoutMs = 10_000L
            ).getOrThrow()

            val serverPubB64 = response.getString("server_pubkey")
            val saltB64 = response.optString("salt", "")
            val sessionId = response.optString("session_id", "default")

            val serverPub = KeyFactory.getInstance("EC")
                .generatePublic(X509EncodedKeySpec(Base64.decode(serverPubB64, Base64.NO_WRAP)))

            val ka = KeyAgreement.getInstance("ECDH")
            ka.init(keyPair.private)
            ka.doPhase(serverPub, true)
            val shared = ka.generateSecret()

            val salt = if (saltB64.isNotBlank()) {
                Base64.decode(saltB64, Base64.NO_WRAP)
            } else {
                sessionId.toByteArray(Charsets.UTF_8)
            }
            val sessionKey = TehLinkCrypto.deriveSessionKey(shared, salt)

            SessionState(sessionId = sessionId, sessionKey = sessionKey).also { state ->
                activeSession = state
                secureStore.setTehLinkSessionKey(sessionId, sessionKey)
            }
        }
    }

    fun restoreFromStore(): Boolean {
        val sessionId = secureStore.getTehLinkSessionId()
        val key = secureStore.getTehLinkSessionKey()
        if (sessionId.isBlank() || key == null) return false
        activeSession = SessionState(sessionId, key)
        return true
    }

    fun clear() {
        activeSession = null
        secureStore.clearTehLinkSession()
    }

    fun encryptOutbound(json: String): String {
        val key = activeSession?.sessionKey ?: return json
        return TehLinkCrypto.encryptJson(json, key)
    }

    fun decryptInbound(line: String): String {
        val key = activeSession?.sessionKey ?: return line
        return TehLinkCrypto.tryDecryptJson(line, key) ?: line
    }
}
