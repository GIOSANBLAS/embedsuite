package com.embedsuite.app.core.bruce

import org.junit.Assert.assertEquals
import org.junit.Test

class BruceSerialFramerTest {

    @Test
    fun feed_emitsTextLines_andSkipsBinaryPackets() {
        val lines = mutableListOf<String>()
        val packets = mutableListOf<ByteArray>()
        val framer = BruceSerialFramer(onLine = { lines += it }, onBinaryPacket = { packets += it })

        framer.feed("hi\r\n".toByteArray() + byteArrayOf(0xAA.toByte(), 6, 0, 0x07, 0xE0.toByte(), 0x01))
        framer.feed("ok\n".toByteArray())

        assertEquals(listOf("hi", "ok"), lines)
        assertEquals(1, packets.size)
    }
}
