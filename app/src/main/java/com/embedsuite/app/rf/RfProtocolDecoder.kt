package com.embedsuite.app.rf

data class DecodedRfSignal(
    val protocol: String,
    val frequency: String = "433.92",
    val bitCount: Int = 0,
    val hexKey: String = "",
    val te: Int = 0,
    val rawSummary: String = "",
    val fields: Map<String, String> = emptyMap()
)

object RfProtocolDecoder {

    fun decode(line: String): DecodedRfSignal? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        decodePrinceton(trimmed)?.let { return it }
        decodeKeeloq(trimmed)?.let { return it }
        decodePt2262(trimmed)?.let { return it }
        decodeGeneric(trimmed)?.let { return it }
        decodeRaw(trimmed)?.let { return it }

        return null
    }

    private fun decodePt2262(line: String): DecodedRfSignal? {
        if (!line.contains("PT2262", ignoreCase = true) && !line.contains("EV1527", ignoreCase = true)) return null
        val bits = extractInt(line, listOf("Bit count", "Bits")) ?: 24
        val key = extractHex(line) ?: ""
        return DecodedRfSignal(
            protocol = if (line.contains("EV1527", ignoreCase = true)) "EV1527" else "PT2262",
            bitCount = bits,
            hexKey = key,
            fields = mapOf(
                "Tipo" to "Código fijo (vulnerable a replay)",
                "Bits" to bits.toString(),
                "Key" to key
            )
        )
    }

    private fun decodeKeeloq(line: String): DecodedRfSignal? {
        if (!line.contains("Keeloq", ignoreCase = true)) return null
        val key = extractHex(line) ?: ""
        return DecodedRfSignal(
            protocol = "Keeloq",
            bitCount = extractInt(line, listOf("Bit count", "Bits")) ?: 64,
            hexKey = key,
            fields = mapOf(
                "Tipo" to "Rolling code",
                "Key" to key,
                "Nota" to "Replay limitado — captura múltiples tramas"
            )
        )
    }

    private fun decodePrinceton(line: String): DecodedRfSignal? {
        if (!line.contains("Princeton", ignoreCase = true) && !line.contains("Holtek", ignoreCase = true)) return null
        val proto = when {
            line.contains("Holtek", ignoreCase = true) -> "Holtek"
            else -> "Princeton"
        }
        return DecodedRfSignal(
            protocol = proto,
            bitCount = extractInt(line, listOf("Bit count", "Bits")) ?: 24,
            hexKey = extractHex(line) ?: "",
            fields = mapOf("Fabricante" to "Holtek/Princeton compatible")
        )
    }

    private fun decodeGeneric(line: String): DecodedRfSignal? {
        val protoMatch = Regex("""(?i)Protocol[:\s]+(\S+)""").find(line) ?: return null
        val protocol = protoMatch.groupValues[1]
        if (protocol.equals("RAW", ignoreCase = true)) return null
        return DecodedRfSignal(
            protocol = protocol,
            bitCount = extractInt(line, listOf("Bit count", "Bits")) ?: 0,
            hexKey = extractHex(line) ?: "",
            te = extractInt(line, listOf("TE", "Te")) ?: 0,
            fields = buildMap {
                put("Protocolo", protocol)
                extractHex(line)?.let { put("Data", it) }
            }
        )
    }

    private fun decodeRaw(line: String): DecodedRfSignal? {
        if (!line.contains("RAW", ignoreCase = true)) return null
        val pulseCount = Regex("""\d+""").findAll(line).count()
        return DecodedRfSignal(
            protocol = "RAW",
            rawSummary = line.take(120),
            fields = mapOf(
                "Pulsos detectados" to pulseCount.toString(),
                "Modulación" to "ASK/OOK probable",
                "Acción" to "Analizar waveform en tab RF"
            )
        )
    }

    private fun extractHex(line: String): String? {
        return Regex("""(?:Key|Data|Code)[:\s]+([0-9A-Fa-fx]+)""").find(line)?.groupValues?.get(1)
            ?: Regex("""\b(0x[0-9A-Fa-f]+)\b""").find(line)?.groupValues?.get(1)
    }

    private fun extractInt(line: String, keys: List<String>): Int? {
        for (key in keys) {
            Regex("""(?i)$key[:\s]+(\d+)""").find(line)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        }
        return null
    }

    fun formatDecoded(decoded: DecodedRfSignal): String {
        return buildString {
            appendLine("PROTO: ${decoded.protocol}")
            if (decoded.bitCount > 0) appendLine("BITS: ${decoded.bitCount}")
            if (decoded.hexKey.isNotBlank()) appendLine("KEY: ${decoded.hexKey}")
            decoded.fields.forEach { (k, v) -> appendLine("$k: $v") }
        }.trim()
    }
}
