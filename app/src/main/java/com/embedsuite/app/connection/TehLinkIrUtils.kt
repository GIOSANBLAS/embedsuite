package com.embedsuite.app.connection

/** IR payload helpers for TEH-Link ir_toolkit/send. */
object TehLinkIrUtils {

    fun irTx(protocol: String, addressWord: String, commandWord: String): String {
        val proto = protocol.trim().ifBlank { "NEC" }
        return "ir tx $proto ${normalizeIrWord(addressWord)} ${normalizeIrWord(commandWord)}"
    }

    fun normalizeIrCommand(command: String): String {
        val trimmed = command.trim()
        val match = Regex(
            """(?i)^ir\s+tx\s+(\w+)\s+(0x)?([0-9a-f]+)\s+(0x)?([0-9a-f]+)$"""
        ).matchEntire(trimmed) ?: return trimmed
        return irTx(match.groupValues[1], match.groupValues[3], match.groupValues[5])
    }

    private fun normalizeIrWord(raw: String): String {
        val hex = raw.removePrefix("0x").removePrefix("0X")
            .filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
            .uppercase()
        if (hex.isBlank()) return "00000000"
        if (hex.length >= 8) return hex.take(8)
        val value = hex.toLongOrNull(16) ?: 0L
        val b0 = (value and 0xFF).toInt()
        val b1 = ((value shr 8) and 0xFF).toInt()
        val b2 = ((value shr 16) and 0xFF).toInt()
        val b3 = ((value shr 24) and 0xFF).toInt()
        return "%02X%02X%02X%02X".format(b0, b1, b2, b3)
    }
}
