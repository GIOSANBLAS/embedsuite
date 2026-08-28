package com.embedsuite.app.engine.decoder

/** Parser básico de archivos .sub (formato Flipper RAW). */
data class SubCapture(
    val frequencyHz: Long,
    val preset: String,
    val protocol: String,
    val rawTimings: List<Int>,
    val fileName: String = ""
)

object SubFileParser {

    fun parse(content: String, fileName: String = ""): Result<SubCapture> = runCatching {
        var freq = 433920000L
        var preset = "FuriHalSubGhzPresetOok650Async"
        var protocol = "RAW"
        val timings = mutableListOf<Int>()

        content.lineSequence().forEach { line ->
            val t = line.trim()
            when {
                t.startsWith("Frequency:") -> {
                    val mhz = t.substringAfter(":").trim().removeSuffix("MHz").trim().toDoubleOrNull()
                    if (mhz != null) freq = (mhz * 1_000_000).toLong()
                }
                t.startsWith("Preset:") -> preset = t.substringAfter(":").trim()
                t.startsWith("Protocol:") -> protocol = t.substringAfter(":").trim()
                t.startsWith("RAW:") || t.startsWith("RAW_Data:") -> {
                    val nums = t.substringAfter(":").trim().split(Regex("\\s+"))
                    nums.mapNotNull { it.toIntOrNull() }.forEach { timings += it }
                }
                t.matches(Regex("^-?\\d+(\\s+-?\\d+)*$")) -> {
                    t.split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }.forEach { timings += it }
                }
            }
        }
        if (timings.isEmpty()) error("Sin timings RAW en .sub")
        SubCapture(freq, preset, protocol, timings, fileName)
    }
}
