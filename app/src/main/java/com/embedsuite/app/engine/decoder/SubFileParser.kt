package com.embedsuite.app.engine.decoder

/** Parsed Flipper Zero `.sub` file (RAW or Key protocol). */
data class FlipperSubFile(
    val filetype: String = "Flipper SubGhz RAW File",
    val version: Int = 1,
    val frequencyHz: Long = 433_920_000L,
    val preset: String = "FuriHalSubGhzPresetOok650Async",
    val protocol: String = "RAW",
    val bit: Int? = null,
    val key: String? = null,
    val te: Int? = null,
    val rawTimings: List<Int> = emptyList(),
    val extraFields: Map<String, String> = emptyMap()
) {
    val isKeyFile: Boolean
        get() = filetype.contains("Key", ignoreCase = true) ||
            (!protocol.equals("RAW", ignoreCase = true) && key != null)

    fun frequencyMhz(): Double = frequencyHz / 1_000_000.0

    fun toSubContent(): String = buildString {
        appendLine("Filetype: $filetype")
        appendLine("Version: $version")
        appendLine("Frequency: $frequencyHz")
        appendLine("Preset: $preset")
        appendLine("Protocol: $protocol")
        bit?.let { appendLine("Bit: $it") }
        key?.takeIf { it.isNotBlank() }?.let { appendLine("Key: $it") }
        te?.let { appendLine("TE: $it") }
        extraFields.forEach { (k, v) -> appendLine("$k: $v") }
        if (rawTimings.isNotEmpty()) {
            appendLine("RAW_Data: ${rawTimings.joinToString(" ")}")
        }
    }.trimEnd() + "\n"
}

/** @deprecated Use [FlipperSubFile] */
data class SubCapture(
    val frequencyHz: Long,
    val preset: String,
    val protocol: String,
    val rawTimings: List<Int>,
    val fileName: String = ""
)

object SubFileParser {

    private val KNOWN_KEYS = setOf(
        "Filetype", "Version", "Frequency", "Preset", "Protocol",
        "Bit", "Key", "TE", "RAW", "RAW_Data"
    )

    fun parse(content: String, fileName: String = ""): Result<FlipperSubFile> = runCatching {
        parseFlipperSub(content)
    }

    fun parseFlipperSub(content: String): FlipperSubFile {
        var filetype = "Flipper SubGhz RAW File"
        var version = 1
        var freqHz = 433_920_000L
        var preset = "FuriHalSubGhzPresetOok650Async"
        var protocol = "RAW"
        var bit: Int? = null
        var key: String? = null
        var te: Int? = null
        val timings = mutableListOf<Int>()
        val extra = linkedMapOf<String, String>()

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val colon = line.indexOf(':')
            if (colon <= 0) {
                if (line.matches(Regex("^-?\\d+(\\s+-?\\d+)*$"))) {
                    line.split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }.forEach { timings += it }
                }
                return@forEach
            }
            val field = line.substring(0, colon).trim()
            val value = line.substring(colon + 1).trim()
            when (field) {
                "Filetype" -> filetype = value
                "Version" -> version = value.toIntOrNull() ?: 1
                "Frequency" -> freqHz = parseFrequencyHz(value)
                "Preset" -> preset = value
                "Protocol" -> protocol = value
                "Bit" -> bit = value.toIntOrNull()
                "Key" -> key = value
                "TE" -> te = value.toIntOrNull()
                "RAW", "RAW_Data" -> {
                    value.split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }.forEach { timings += it }
                }
                else -> if (field !in KNOWN_KEYS) extra[field] = value
            }
        }

        if (timings.isEmpty() && key == null && !content.contains("Flipper SubGhz", ignoreCase = true)) {
            error("Archivo .sub inválido: falta Flipper header o datos")
        }
        if (timings.isEmpty() && key == null && protocol.equals("RAW", ignoreCase = true)) {
            error("Sin timings RAW ni Key en .sub")
        }

        return FlipperSubFile(
            filetype = filetype,
            version = version,
            frequencyHz = freqHz,
            preset = preset,
            protocol = protocol,
            bit = bit,
            key = key,
            te = te,
            rawTimings = timings,
            extraFields = extra
        )
    }

    /** Legacy API for [SubGhzDecoder]. */
    fun parseLegacy(content: String, fileName: String = ""): Result<SubCapture> = runCatching {
        val f = parseFlipperSub(content)
        SubCapture(f.frequencyHz, f.preset, f.protocol, f.rawTimings, fileName)
    }

    fun parseRawTimings(raw: String): List<Int> =
        raw.trim().split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }

    private fun parseFrequencyHz(value: String): Long {
        val cleaned = value.removeSuffix("MHz").removeSuffix("mhz").trim()
        cleaned.toLongOrNull()?.let { hz ->
            return if (hz > 10_000) hz else (hz * 1_000_000L)
        }
        cleaned.toDoubleOrNull()?.let { mhz ->
            return (mhz * 1_000_000.0).toLong()
        }
        return 433_920_000L
    }
}
