package com.embedsuite.app.engine.decoder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SubGhzDecoderTest {

    @Test
    fun parseSubFile_extractsRawProtocol() {
        val sub = """
            Filetype: Flipper SubGhz RAW File
            Frequency: 433920000
            Protocol: RAW
            RAW_Data: 500 -500 500 -1500 500 -500
        """.trimIndent()
        val result = SubGhzDecoder.decodeSubFile(sub, "test.sub").getOrThrow()
        assertEquals("RAW", result.protocol)
        assertNotNull(result.summary)
    }

    @Test
    fun decodeLine_princetonFromConsole() {
        val line = "Protocol: Princeton Bit count: 24 Key: A1B2C3"
        val hit = SubGhzDecoder.decodeLine(line)
        assertNotNull(hit)
        assertEquals("Princeton", hit?.protocol)
    }
}
