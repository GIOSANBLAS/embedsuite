package com.embedsuite.app.flipper

import org.junit.Assert.*
import org.junit.Test

class FlipperFileManagerTest {

    @Test
    fun parseSubFile_validContent() {
        val content = """
            Filetype: Flipper SubGhz RAW File
            Version: 1
            Frequency: 433920000
            Protocol: RAW
            RAW_Data: 1000 -1000 1000
        """.trimIndent()
        val result = FlipperFileManager.parseSubFile(content)
        assertNotNull(result)
        assertEquals("433.92", result!!.frequency)
        assertEquals("RAW", result.protocol)
    }

    @Test
    fun parseIrFile_validContent() {
        val content = """
            Filetype: IR signals file
            Version: 1
            name: Power
            protocol: NEC
            command: 0x00FF
        """.trimIndent()
        val result = FlipperFileManager.parseIrFile(content)
        assertNotNull(result)
        assertEquals("Power", result!!.buttonName)
        assertEquals("NEC", result.protocol)
    }

    @Test
    fun parseNfcFile_validContent() {
        val content = """
            Filetype: Flipper NFC device
            UID: 04:A1:B2:C3
            Device type: MIFARE Classic 1K
        """.trimIndent()
        val result = FlipperFileManager.parseNfcFile(content)
        assertNotNull(result)
        assertEquals("04:A1:B2:C3", result!!.uid)
    }

    @Test
    fun parseSubFile_invalidReturnsNull() {
        assertNull(FlipperFileManager.parseSubFile("not a flipper file"))
    }
}
