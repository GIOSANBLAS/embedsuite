package com.embedsuite.app.rf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RfProtocolDecoderTest {

    @Test
    fun decodePt2262() {
        val line = "Protocol: PT2262 Bit count: 24 Key: 0xABCDEF"
        val decoded = RfProtocolDecoder.decode(line)
        assertNotNull(decoded)
        assertEquals("PT2262", decoded?.protocol)
        assertNotNull(decoded?.hexKey)
        assertTrue(decoded!!.hexKey.contains("ABCDEF", ignoreCase = true))
    }

    @Test
    fun decodeEv1527() {
        val decoded = RfProtocolDecoder.decode("EV1527 Bit count: 24 Key: 0x112233")
        assertNotNull(decoded)
        assertEquals("EV1527", decoded?.protocol)
    }

    @Test
    fun decodeKeeloq() {
        val decoded = RfProtocolDecoder.decode("Keeloq Bit count: 64 Key: 0xDEADBEEFCAFEBABE")
        assertNotNull(decoded)
        assertEquals("Keeloq", decoded?.protocol)
    }

    @Test
    fun decodePrinceton() {
        val decoded = RfProtocolDecoder.decode("Princeton Bit count: 24 Key: 0x00AABB")
        assertNotNull(decoded)
        assertEquals("Princeton", decoded?.protocol)
    }

    @Test
    fun decodeHoltek() {
        val decoded = RfProtocolDecoder.decode("Holtek Bit count: 12 Key: 0x0ABC")
        assertNotNull(decoded)
        assertEquals("Holtek", decoded?.protocol)
    }

    @Test
    fun decodeGenericProtocol() {
        val decoded = RfProtocolDecoder.decode("Protocol: CAME Bit count: 12 TE: 320 Key: 0x55AA")
        assertNotNull(decoded)
        assertEquals("CAME", decoded?.protocol)
    }

    @Test
    fun decodeRaw() {
        val line = "RAW_Data: 1000 -500 1000 -500"
        val decoded = RfProtocolDecoder.decode(line)
        assertNotNull(decoded)
        assertEquals("RAW", decoded?.protocol)
    }

    @Test
    fun decodeEmptyOrGarbage_returnsNull() {
        assertNull(RfProtocolDecoder.decode(""))
        assertNull(RfProtocolDecoder.decode("   "))
        assertNull(RfProtocolDecoder.decode("hello world no rf"))
    }
}
