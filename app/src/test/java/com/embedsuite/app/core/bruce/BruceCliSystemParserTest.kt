package com.embedsuite.app.core.bruce

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BruceCliSystemParserTest {

    @Test
    fun parseInfo_extractsVersionAndDevice() {
        val response = """
            Bruce v1.14.0
            a1b2c3d4
            SDK: v4.4.7
            MAC addr: 24:58:7c:5b:24:5c
            Wifi: not connected
            Device: LilyGo T-Embed CC1101
        """.trimIndent()
        val parsed = BruceCliSystemParser.parseInfo(response)
        assertEquals("1.14.0", parsed.version)
        assertEquals("LilyGo T-Embed CC1101", parsed.deviceName)
        assertEquals(false, parsed.wifiConnected)
    }

    @Test
    fun parseFree_extractsHeapAndPsram() {
        val response = """
            Total heap: 327680
            Free heap: 198432
            Total PSRAM: 8388608
            Free PSRAM: 8123456
        """.trimIndent()
        val parsed = BruceCliSystemParser.parseFree(response)
        assertEquals(198_432L, parsed.heapFreeBytes)
        assertEquals(327_680L, parsed.heapTotalBytes)
        assertEquals(8_123_456L, parsed.psramFreeBytes)
    }

    @Test
    fun parseSdFree_extractsFreeBytes() {
        val response = """
            SD Total space: 31914983424 Bytes
            SD Used space: 134217728 Bytes
            SD Free space: 31780765696 Bytes
        """.trimIndent()
        val parsed = BruceCliSystemParser.parseSdFree(response)
        assertTrue(parsed.mounted)
        assertEquals(31_780_765_696L, parsed.freeBytes)
        assertEquals("29.6 GB", BruceCliSystemParser.formatBytes(parsed.freeBytes!!))
    }

    @Test
    fun parseSdFree_noCard() {
        val parsed = BruceCliSystemParser.parseSdFree("No SD card installed")
        assertFalse(parsed.mounted)
        assertNull(parsed.freeBytes)
    }

    @Test
    fun formatBattery_percentOnly() {
        assertEquals("78%", BruceCliSystemParser.formatBattery(78))
    }
}
