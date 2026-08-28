package com.embedsuite.app.nfc

object MifareParser {

    data class SectorInfo(val index: Int, val blocks: List<String>, val accessBits: String = "")

    /** Normalizes a MIFARE block to 16 bytes (32 hex chars). */
    fun normalizeBlockHex(input: String): String {
        val cleaned = input.replace(Regex("[^0-9A-Fa-f]"), "").uppercase()
        return cleaned.take(32).padEnd(32, '0')
    }

    fun isSectorTrailer(blockIndex: Int): Boolean = blockIndex == 3

    fun parseDump(dump: String): List<SectorInfo> {
        val sectors = mutableListOf<SectorInfo>()
        val lines = dump.lines().filter { it.isNotBlank() }
        var currentSector = -1
        val blocks = mutableListOf<String>()

        lines.forEach { line ->
            when {
                line.contains("Sector", ignoreCase = true) -> {
                    if (currentSector >= 0) sectors.add(SectorInfo(currentSector, blocks.toList()))
                    blocks.clear()
                    currentSector = Regex("""\d+""").find(line)?.value?.toIntOrNull() ?: currentSector + 1
                }
                line.matches(Regex("""[0-9A-Fa-f\s]+""")) && line.replace(" ", "").length >= 16 -> {
                    blocks.add(normalizeBlockHex(line))
                }
            }
        }
        if (currentSector >= 0) sectors.add(SectorInfo(currentSector, blocks.toList()))

        if (sectors.isEmpty()) {
            val hexLines = lines.filter { line ->
                line.replace(" ", "").matches(Regex("""[0-9A-Fa-f]{32}"""))
            }
            if (hexLines.isNotEmpty()) {
                hexLines.chunked(4).forEachIndexed { sectorIdx, chunk ->
                    sectors.add(SectorInfo(sectorIdx, chunk.map { normalizeBlockHex(it) }))
                }
            }
        }
        return sectors
    }

    fun serializeDump(sectors: List<SectorInfo>, uid: String = ""): String = buildString {
        if (uid.isNotBlank() && uid != "—") appendLine("UID: $uid")
        sectors.forEach { sector ->
            appendLine("Sector ${sector.index}")
            sector.blocks.forEach { block ->
                appendLine(formatBlockDisplay(block))
            }
        }
    }

    /** Formats 32 hex chars as 4-byte groups for readability. */
    fun formatBlockDisplay(hex: String): String {
        val normalized = normalizeBlockHex(hex)
        return normalized.chunked(2).joinToString(" ")
    }

    fun updateBlock(
        sectors: List<SectorInfo>,
        sectorIndex: Int,
        blockIndex: Int,
        newHex: String
    ): List<SectorInfo> {
        val normalized = normalizeBlockHex(newHex)
        return sectors.map { sector ->
            if (sector.index != sectorIndex) sector
            else sector.copy(
                blocks = sector.blocks.mapIndexed { idx, block ->
                    if (idx == blockIndex) normalized else block
                }
            )
        }
    }

    fun formatVisual(uid: String, sectors: List<SectorInfo>): String {
        return buildString {
            appendLine("UID: $uid")
            appendLine("Tipo: MIFARE Classic (estimado)")
            appendLine("Sectores: ${sectors.size}")
            sectors.forEach { s ->
                appendLine("── Sector ${s.index} ──")
                s.blocks.forEachIndexed { i, b ->
                    val tag = if (isSectorTrailer(i)) " [trailer]" else ""
                    appendLine("  Block $i: ${formatBlockDisplay(b)}$tag")
                }
            }
            appendLine()
            appendLine("[Diccionario educativo]")
            appendLine("• Default Key A: FFFFFFFFFFFF")
            appendLine("• Default Key B: FFFFFFFFFFFF")
            appendLine("• Lectura UID: sin clave")
            appendLine("• Escritura sector: requiere auth")
        }
    }

    val EDUCATIONAL_ATTACKS = listOf(
        "Default keys (FFFFFFFFFFFF)",
        "Nested attack (MIFARE Classic)",
        "Darkside attack (nonce reuse)",
        "UID clone (emulación TEH-Link)",
        "Read-only sector dump"
    )
}
