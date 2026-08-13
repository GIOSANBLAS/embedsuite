package com.embedsuite.app.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TehLinkCryptoTest {

    @Test
    fun encryptDecryptRoundTrip() {
        val key = ByteArray(32) { it.toByte() }
        val plain = """{"cmd":"ping","id":1}""".toByteArray()
        val enc = TehLinkCrypto.encrypt(plain, key)
        val dec = TehLinkCrypto.decrypt(enc.nonceB64, enc.ciphertextB64, key)
        assertArrayEquals(plain, dec)
    }

    @Test
    fun encryptJsonProducesEncWrapper() {
        val key = ByteArray(32) { (it * 3).toByte() }
        val wrapped = TehLinkCrypto.encryptJson("hello", key)
        assert(wrapped.contains("\"enc\":true"))
        val plain = TehLinkCrypto.tryDecryptJson(wrapped, key)
        assertEquals("hello", plain)
    }

    @Test
    fun deriveSessionKeyIsDeterministic() {
        val secret = "shared-secret".toByteArray()
        val salt = "salt".toByteArray()
        val k1 = TehLinkCrypto.deriveSessionKey(secret, salt)
        val k2 = TehLinkCrypto.deriveSessionKey(secret, salt)
        assertArrayEquals(k1, k2)
        assertEquals(32, k1.size)
    }

    @Test
    fun differentSaltsProduceDifferentKeys() {
        val secret = "shared-secret".toByteArray()
        val k1 = TehLinkCrypto.deriveSessionKey(secret, "a".toByteArray())
        val k2 = TehLinkCrypto.deriveSessionKey(secret, "b".toByteArray())
        assertNotEquals(k1.toList(), k2.toList())
    }
}
