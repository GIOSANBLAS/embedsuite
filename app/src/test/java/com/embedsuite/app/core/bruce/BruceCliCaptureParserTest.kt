package com.embedsuite.app.core.bruce

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BruceCliCaptureParserTest {

    @Test
    fun parseSubGhzResponse_findsRawBlock() {
        val response = """
            Protocol: Princeton
            Bit count: 24
            Key: 0xAABBCC
        """.trimIndent()
        val entry = BruceCliCaptureParser.parseSubGhzResponse(response, 433.92)
        assertNotNull(entry)
        assertEquals("433.92", entry?.frequency)
        assertEquals("Princeton", entry?.protocol)
    }

    @Test
    fun parseIrCapture_buildsCli() {
        val response = "Captured: ir tx NEC 00FF00FF 00FF00FF"
        val cap = BruceCliCaptureParser.parseIrCapture(response)
        assertNotNull(cap)
        assertTrue(cap!!.cliCommand.startsWith("ir tx"))
    }
}
