package com.embedsuite.app.flash

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SlipEncoderTest {

    @Test
    fun buildCommand_matchesEsptoolV4Header() {
        // esptool: struct.pack("<BBHI", 0, 0x08, len, 0) + data  (SYNC chk=0)
        val syncData = byteArrayOf(0x07, 0x07)
        val pkt = SlipEncoder.buildCommand(0x08, syncData)
        assertEquals(8 + syncData.size, pkt.size)
        assertEquals(0x00, pkt[0].toInt() and 0xFF)
        assertEquals(0x08, pkt[1].toInt() and 0xFF)
        assertEquals(syncData.size and 0xFF, pkt[2].toInt() and 0xFF)
        assertEquals(0, pkt[4].toInt() and 0xFF) // chk=0
    }

    @Test
    fun buildCommand_flashDataChecksumInHeader() {
        val block = ByteArray(4) { 0x55 }
        val chk = SlipEncoder.checksum(block)
        val header = byteArrayOf(0, 0, 0, 0)
        val payload = header + block
        val pkt = SlipEncoder.buildCommand(0x03, payload, chk)
        assertEquals(chk and 0xFF, pkt[4].toInt() and 0xFF)
        assertArrayEquals(payload, pkt.copyOfRange(8, pkt.size))
    }
}
