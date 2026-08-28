package com.embedsuite.app.engine.decoder.protocols

data class ProtocolHit(val summary: String, val fields: Map<String, String>)

object PrincetonDecoder {
    /** Heurística OOK 24-bit Princeton/Holtek sobre timings RAW. */
    fun decode(timings: List<Int>): ProtocolHit? {
        if (timings.size < 48) return null
        val te = estimateTe(timings) ?: return null
        val bits = ookBits(timings, te) ?: return null
        if (bits.length !in 20..32) return null
        return ProtocolHit(
            summary = "Princeton ${bits.length}b: $bits",
            fields = mapOf(
                "Bits" to bits.length.toString(),
                "Codigo" to bits,
                "TE_us" to te.toString()
            )
        )
    }

    private fun estimateTe(t: List<Int>): Int? {
        val pos = t.filter { it > 0 }.sorted()
        if (pos.isEmpty()) return null
        return pos[pos.size / 4].coerceIn(200, 800)
    }

    private fun ookBits(t: List<Int>, te: Int): String? {
        val sb = StringBuilder()
        var i = 0
        while (i < t.size - 1 && sb.length < 32) {
            val hi = kotlin.math.abs(t[i])
            val lo = kotlin.math.abs(t.getOrElse(i + 1) { 0 })
            if (hi in (te / 2)..(te * 3) && lo in (te / 2)..(te * 3)) {
                sb.append(if (hi > lo) '1' else '0')
                i += 2
            } else i++
        }
        return sb.takeIf { it.length >= 20 }?.toString()
    }
}
