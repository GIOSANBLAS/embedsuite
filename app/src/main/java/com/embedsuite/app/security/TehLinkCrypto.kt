package com.embedsuite.app.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM helpers for TEH-Link encrypted payloads.
 * Wire format: base64(nonce[12] || ciphertext+tag).
 */
object TehLinkCrypto {

    private const val AES_KEY_BYTES = 32
    private const val GCM_NONCE_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    data class EncryptedPayload(
        val nonceB64: String,
        val ciphertextB64: String
    )

    /** Derives a 256-bit session key from ECDH shared secret using HKDF-SHA256. */
    fun deriveSessionKey(sharedSecret: ByteArray, salt: ByteArray, info: ByteArray = "teh-link-v3".toByteArray()): ByteArray {
        val prk = hmacSha256(salt, sharedSecret)
        val okm = ByteArray(AES_KEY_BYTES)
        var t = ByteArray(0)
        var offset = 0
        var counter = 1
        while (offset < AES_KEY_BYTES) {
            val input = t + info + counter.toByte()
            t = hmacSha256(prk, input)
            val copyLen = minOf(t.size, AES_KEY_BYTES - offset)
            System.arraycopy(t, 0, okm, offset, copyLen)
            offset += copyLen
            counter++
        }
        return okm
    }

    fun encrypt(plaintext: ByteArray, key: ByteArray): EncryptedPayload {
        require(key.size == AES_KEY_BYTES) { "AES-256 key required" }
        val nonce = ByteArray(GCM_NONCE_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedPayload(
            nonceB64 = Base64.getEncoder().encodeToString(nonce),
            ciphertextB64 = Base64.getEncoder().encodeToString(ciphertext)
        )
    }

    fun decrypt(nonceB64: String, ciphertextB64: String, key: ByteArray): ByteArray {
        require(key.size == AES_KEY_BYTES) { "AES-256 key required" }
        val nonce = Base64.getDecoder().decode(nonceB64)
        val ciphertext = Base64.getDecoder().decode(ciphertextB64)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        return cipher.doFinal(ciphertext)
    }

    fun encryptJson(json: String, key: ByteArray): String {
        val enc = encrypt(json.toByteArray(Charsets.UTF_8), key)
        return """{"enc":true,"nonce":"${enc.nonceB64}","ciphertext":"${enc.ciphertextB64}"}"""
    }

    fun tryDecryptJson(line: String, key: ByteArray): String? {
        if (!line.trimStart().startsWith("{")) return null
        return runCatching {
            val obj = org.json.JSONObject(line)
            if (!obj.optBoolean("enc")) return null
            val plain = decrypt(obj.getString("nonce"), obj.getString("ciphertext"), key)
            String(plain, Charsets.UTF_8)
        }.getOrNull()
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }
}
