package com.embedsuite.app.engine.decoder.protocols

object CameDecoder {
    /** CAME / Nice FLO 12-bit + sync (433.92 OOK). */
    fun decode(timings: List<Int>): ProtocolHit? {
        if (timings.size < 24) return null
        val te = timings.filter { it > 0 }.sorted().getOrNull(timings.size / 8) ?: return null
        if (te !in 250..650) return null
        val bits = StringBuilder()
        var i = 0
        while (i < timings.size - 1 && bits.length < 18) {
            val a = kotlin.math.abs(timings[i])
            val b = kotlin.math.abs(timings.getOrElse(i + 1) { 0 })
            if (a in (te / 2)..(te * 3) && b in (te / 2)..(te * 3)) {
                bits.append(if (a < b) '0' else '1')
                i += 2
            } else i++
        }
        if (bits.length < 12) return null
        return ProtocolHit(
            summary = "CAME ${bits.length}b: $bits",
            fields = mapOf("Bits" to bits.length.toString(), "Frame" to bits.toString())
        )
    }
}
