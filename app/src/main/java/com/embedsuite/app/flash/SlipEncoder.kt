package com.embedsuite.app.flash

object SlipEncoder {

    private const val SLIP_END: Byte = 0xC0.toByte()
    private const val SLIP_ESC: Byte = 0xDB.toByte()
    private const val SLIP_ESC_END: Byte = 0xDC.toByte()
    private const val SLIP_ESC_ESC: Byte = 0xDD.toByte()

    fun encode(packet: ByteArray): ByteArray {
        val out = ArrayList<Byte>(packet.size + 4)
        out.add(SLIP_END)
        packet.forEach { b ->
            when (b) {
                SLIP_END -> {
                    out.add(SLIP_ESC)
                    out.add(SLIP_ESC_END)
                }
                SLIP_ESC -> {
                    out.add(SLIP_ESC)
                    out.add(SLIP_ESC_ESC)
                }
                else -> out.add(b)
            }
        }
        out.add(SLIP_END)
        return out.toByteArray()
    }

    fun decode(input: ByteArray): List<ByteArray> {
        val packets = mutableListOf<ByteArray>()
        val current = ArrayList<Byte>()
        var i = 0
        while (i < input.size) {
            when (input[i]) {
                SLIP_END -> {
                    if (current.isNotEmpty()) {
                        packets.add(current.toByteArray())
                        current.clear()
                    }
                }
                SLIP_ESC -> {
                    i++
                    if (i < input.size) {
                        when (input[i]) {
                            SLIP_ESC_END -> current.add(SLIP_END)
                            SLIP_ESC_ESC -> current.add(SLIP_ESC)
                        }
                    }
                }
                else -> current.add(input[i])
            }
            i++
        }
        return packets
    }

    fun buildCommand(op: Int, data: ByteArray = byteArrayOf()): ByteArray {
        val size = data.size
        val header = byteArrayOf(
            0x00,
            op.toByte(),
            (size and 0xFF).toByte(),
            ((size shr 8) and 0xFF).toByte(),
            (size xor 0xEF).toByte()
        )
        return header + data
    }
}
