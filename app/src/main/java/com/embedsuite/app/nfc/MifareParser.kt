package com.embedsuite.app.nfc

object MifareParser {

    data class SectorInfo(val index: Int, val blocks: List<String>, val accessBits: String = "")

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
                line.matches(Regex("""[0-9A-Fa-f\s]+""")) && line.length >= 16 -> {
                    blocks.add(line.trim())
                }
            }
        }
        if (currentSector >= 0) sectors.add(SectorInfo(currentSector, blocks.toList()))
        return sectors
    }

    fun formatVisual(uid: String, sectors: List<SectorInfo>): String {
        return buildString {
            appendLine("UID: $uid")
            appendLine("Tipo: MIFARE Classic (estimado)")
            appendLine("Sectores: ${sectors.size}")
            sectors.forEach { s ->
                appendLine("── Sector ${s.index} ──")
                s.blocks.forEachIndexed { i, b -> appendLine("  Block $i: ${b.take(32)}") }
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
        "UID clone (emulación Bruce)",
        "Read-only sector dump"
    )
}
