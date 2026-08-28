package com.embedsuite.app.flash

object SlipEncoder {

    private const val SLIP_END: Byte = 0xC0.toByte()
    private const val SLIP_ESC: Byte = 0xDB.toByte()
    private const val SLIP_ESC_END: Byte = 0xDC.toByte()
    private const val SLIP_ESC_ESC: Byte = 0xDD.toByte()

    const val CHECKSUM_MAGIC = 0xEF

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
        if (current.isNotEmpty()) {
            packets.add(current.toByteArray())
        }
        return packets
    }

    /** XOR ROM checksum (1 byte), esptool lo manda como uint32 LE en el header. */
    fun checksum(data: ByteArray, initial: Int = CHECKSUM_MAGIC): Int {
        var state = initial
        for (b in data) {
            state = state xor (b.toInt() and 0xFF)
        }
        return state and 0xFF
    }

    /**
     * Formato esptool v4+: [0x00][op][size_lo][size_hi][chk:4 LE][data…]
     * SYNC/FLASH_BEGIN: chk=0. FLASH_DATA: chk=checksum(bloque raw).
     */
    fun buildCommand(op: Int, data: ByteArray = byteArrayOf(), dataBlockChecksum: Int? = null): ByteArray {
        val chk = dataBlockChecksum ?: 0
        val size = data.size
        return byteArrayOf(
            0x00,
            op.toByte(),
            (size and 0xFF).toByte(),
            ((size shr 8) and 0xFF).toByte(),
            (chk and 0xFF).toByte(),
            ((chk shr 8) and 0xFF).toByte(),
            ((chk shr 16) and 0xFF).toByte(),
            ((chk shr 24) and 0xFF).toByte()
        ) + data
    }
}
