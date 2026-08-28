package com.embedsuite.app.engine.decoder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubFileParserTest {

    @Test
    fun parseFlipperSub_rawFile() {
        val content = """
            Filetype: Flipper SubGhz RAW File
            Version: 1
            Frequency: 433920000
            Preset: FuriHalSubGhzPresetOok650Async
            Protocol: RAW
            RAW_Data: 500 -500 500 -1500
        """.trimIndent()
        val parsed = SubFileParser.parseFlipperSub(content)
        assertEquals(433_920_000L, parsed.frequencyHz)
        assertEquals("RAW", parsed.protocol)
        assertEquals(4, parsed.rawTimings.size)
        assertTrue(parsed.toSubContent().contains("RAW_Data: 500 -500 500 -1500"))
    }

    @Test
    fun parseFlipperSub_keyFile() {
        val content = """
            Filetype: Flipper SubGhz Key File
            Version: 1
            Frequency: 433920000
            Preset: FuriHalSubGhzPresetOok650Async
            Protocol: Princeton
            Bit: 24
            Key: 00 00 00 00 01
            TE: 359
        """.trimIndent()
        val parsed = SubFileParser.parseFlipperSub(content)
        assertEquals("Princeton", parsed.protocol)
        assertEquals(24, parsed.bit)
        assertEquals("00 00 00 00 01", parsed.key)
        assertEquals(359, parsed.te)
        assertTrue(parsed.isKeyFile)
        assertTrue(parsed.toSubContent().contains("Key: 00 00 00 00 01"))
    }

    @Test
    fun parseFlipperSub_roundTripPreservesFields() {
        val original = FlipperSubFile(
            filetype = "Flipper SubGhz Key File",
            protocol = "CAME",
            bit = 12,
            key = "A1B2",
            te = 320,
            rawTimings = listOf(100, -100)
        )
        val reparsed = SubFileParser.parseFlipperSub(original.toSubContent())
        assertEquals(original.protocol, reparsed.protocol)
        assertEquals(original.bit, reparsed.bit)
        assertEquals(original.key, reparsed.key)
        assertEquals(original.te, reparsed.te)
    }

    @Test
    fun parseRawTimings_spaceSeparated() {
        assertEquals(listOf(500, -500, 1500), SubFileParser.parseRawTimings("500 -500  1500"))
    }
}
