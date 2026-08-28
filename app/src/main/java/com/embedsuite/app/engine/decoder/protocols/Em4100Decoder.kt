package com.embedsuite.app.engine.decoder.protocols

object Em4100Decoder {
    /** EM4100 / FDX-B: 64 Manchester bits típicos en 125 kHz RFID (skill). */
    fun decode(timings: List<Int>): ProtocolHit? {
        if (timings.size < 64) return null
        val te = timings.filter { it > 0 }.minOrNull() ?: return null
        if (te !in 200..500) return null
        val bits = manchesterBits(timings, te) ?: return null
        if (bits.length < 40) return null
        val id = bits.take(40).chunked(4).joinToString("") { nibble ->
            nibble.toIntOrNull(2)?.toString(16)?.uppercase() ?: "?"
        }
        return ProtocolHit(
            summary = "EM4100 UID $id",
            fields = mapOf("UID" to id, "Bits" to bits.length.toString())
        )
    }

    private fun manchesterBits(t: List<Int>, te: Int): String? {
        val sb = StringBuilder()
        for (i in t.indices) {
            val p = kotlin.math.abs(t[i])
            if (p in (te / 2)..(te * 2)) sb.append(if (i % 2 == 0) '0' else '1')
        }
        return sb.takeIf { it.length >= 40 }?.toString()
    }
}
