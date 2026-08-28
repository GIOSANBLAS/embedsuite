package com.embedsuite.app.core.bruce

/**
 * Separa líneas de texto CLI de paquetes binarios 0xAA (display/log del firmware Bruce).
 * Inspirado en la lógica de transporte serial del proyecto oficial Bruce App — sin espejo TFT.
 */
class BruceSerialFramer(
    private val onLine: (String) -> Unit,
    private val onBinaryPacket: ((ByteArray) -> Unit)? = null
) {
    private val text = StringBuilder()
    private val packet = ByteArray(256)
    private var packetPos = 0
    private var expectedSize = 0

    fun feed(buf: ByteArray, length: Int = buf.size) {
        for (i in 0 until length) {
            val b = buf[i]
            val ub = b.toInt() and 0xFF

            if (packetPos > 0) {
                packet[packetPos++] = b
                if (packetPos == 2) expectedSize = ub
                if (expectedSize > 0 && packetPos >= expectedSize) {
                    onBinaryPacket?.invoke(packet.copyOf(packetPos))
                    packetPos = 0
                    expectedSize = 0
                } else if (packetPos >= packet.size) {
                    packetPos = 0
                    expectedSize = 0
                }
            } else if (ub == PACKET_HEADER) {
                flushText()
                packet[0] = b
                packetPos = 1
            } else if (b == '\n'.code.toByte()) {
                emitTextLine()
            } else if (b != '\r'.code.toByte()) {
                text.append(ub.toChar())
            }
        }
    }

    fun flush() {
        flushText()
    }

    private fun emitTextLine() {
        if (text.isEmpty()) return
        onLine(text.toString())
        text.clear()
    }

    private fun flushText() {
        if (text.isNotEmpty()) emitTextLine()
    }

    companion object {
        const val PACKET_HEADER = 0xAA
    }
}
