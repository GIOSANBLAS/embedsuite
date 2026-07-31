package com.embedsuite.app.connection

import com.embedsuite.app.rf.RfProtocolDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BruceResponseParser {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun parseLine(line: String): BruceEvent {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return BruceEvent.RawLine(line)

        parseSubGhzSignal(trimmed)?.let { return BruceEvent.SubGhzSignal(it) }
        parseWaveformSample(trimmed)?.let { return BruceEvent.WaveformSample(it.first, it.second) }
        parseSystemInfo(trimmed)?.let { return BruceEvent.SystemInfoUpdate(it) }

        return BruceEvent.RawLine(line)
    }

    fun parseRssiDbm(line: String): Float? {
        return Regex("""(-?\d+)\s*dBm""", RegexOption.IGNORE_CASE).find(line)?.groupValues?.get(1)
            ?.toFloatOrNull()
    }

    /** Multi-bin scan row: "300:-90 433:-72 868:-80" or CSV RSSI values */
    fun parseSpectrumRow(line: String): List<Float>? {
        if (!line.contains("dBm", ignoreCase = true) &&
            !line.contains("scan", ignoreCase = true) &&
            !line.contains("rssi", ignoreCase = true)
        ) {
            return null
        }
        val pairs = Regex("""(\d{3}(?:\.\d+)?)\s*[:=]\s*(-?\d+)""").findAll(line).mapNotNull { m ->
            val rssi = m.groupValues[2].toFloatOrNull() ?: return@mapNotNull null
            ((rssi + 110f) / 90f).coerceIn(0f, 1f)
        }.toList()
        if (pairs.size >= 8) {
            return pairs.take(SPECTRUM_BINS).let { bins ->
                if (bins.size == SPECTRUM_BINS) bins
                else bins + List(SPECTRUM_BINS - bins.size) { bins.lastOrNull() ?: 0f }
            }
        }
        val csv = Regex("""(-?\d+)""").findAll(line).mapNotNull { it.value.toFloatOrNull() }.toList()
        if (csv.size >= SPECTRUM_BINS && line.contains("scan", ignoreCase = true)) {
            return csv.take(SPECTRUM_BINS).map { rssi ->
                ((rssi + 110f) / 90f).coerceIn(0f, 1f)
            }
        }
        return null
    }

    fun parsePulseSample(line: String): Pair<Float, Long>? = parseWaveformSample(line)

    private const val SPECTRUM_BINS = 128

    private fun parseSubGhzSignal(line: String): SignalEntry? {
        RfProtocolDecoder.decode(line)?.let { decoded ->
            return SignalEntry(
                timestamp = now(),
                frequency = decoded.frequency,
                deviceId = decoded.hexKey.take(16).ifBlank { "DECODED" },
                protocol = decoded.protocol,
                power = extractRssi(line) ?: "-50 dBm",
                rawData = line
            )
        }

        val decoded = Regex(
            """(?i)(?:Protocol|Proto):\s*(\S+).*?(?:Bit\s*count|Bits):\s*(\d+).*?(?:Key|Data):\s*([0-9A-Fa-fx]+)""",
            RegexOption.DOT_MATCHES_ALL
        ).find(line)

        if (decoded != null) {
            return SignalEntry(
                timestamp = now(),
                frequency = extractFrequency(line) ?: "433.92",
                deviceId = decoded.groupValues[3].take(10),
                protocol = decoded.groupValues[1],
                power = extractRssi(line) ?: "-50 dBm",
                rawData = line
            )
        }

        if (line.contains("RAW_Data", ignoreCase = true) || line.contains("RAW:", ignoreCase = true)) {
            return SignalEntry(
                timestamp = now(),
                frequency = extractFrequency(line) ?: "433.92",
                deviceId = "RAW",
                protocol = "RAW",
                power = extractRssi(line) ?: "-50 dBm",
                rawData = line
            )
        }

        return null
    }

    private fun parseWaveformSample(line: String): Pair<Float, Long>? {
        val pulse = Regex("""(?i)(?:Pulse|Level|Bit)\s*[:=]\s*(\d+)\s*(?:us|µs)?""").find(line)
            ?: Regex("""(?i)^(\d+)\s+(\d+)$""").find(line)
            ?: return null

        return when (pulse.groupValues.size) {
            2 -> {
                val duration = pulse.groupValues[1].toLongOrNull() ?: return null
                Pair(1f, duration)
            }
            else -> {
                val duration = pulse.groupValues[1].toLongOrNull() ?: return null
                Pair(1f, duration)
            }
        }
    }

    private fun parseSystemInfo(line: String): SystemInfo? {
        return when {
            line.contains("Uptime", ignoreCase = true) -> {
                SystemInfo(uptime = line.substringAfter(":").trim())
            }
            line.contains("Free heap", ignoreCase = true) || line.contains("Free:", ignoreCase = true) -> {
                SystemInfo(freeHeap = line.substringAfter(":").trim())
            }
            line.contains("Battery", ignoreCase = true) -> {
                SystemInfo(battery = line.substringAfter(":").trim())
            }
            line.contains("Firmware", ignoreCase = true) || line.contains("Version", ignoreCase = true) -> {
                SystemInfo(firmware = line.substringAfter(":").trim())
            }
            else -> null
        }
    }

    fun parseRawPulseTrain(rawLine: String): List<Pair<Float, Long>> {
        val numbers = Regex("""\d+""").findAll(rawLine).map { it.value.toLongOrNull() ?: 0L }.toList()
        if (numbers.isEmpty()) return emptyList()

        return numbers.mapIndexed { index, duration ->
            val level = if (index % 2 == 0) 1f else 0f
            level to duration
        }
    }

    private fun extractFrequency(line: String): String? {
        return Regex("""(\d{3}\.\d{2})\s*MHz""").find(line)?.groupValues?.get(1)
            ?: Regex("""Freq(?:uency)?[:=]\s*(\d+)""", RegexOption.IGNORE_CASE).find(line)?.groupValues?.get(1)?.let {
                val mhz = it.toLongOrNull() ?: return@let null
                String.format(Locale.US, "%.2f", mhz / 1_000_000.0)
            }
    }

    private fun extractRssi(line: String): String? {
        return Regex("""(-?\d+)\s*dBm""", RegexOption.IGNORE_CASE).find(line)?.let {
            "${it.groupValues[1]} dBm"
        }
    }

    private fun now(): String = timeFormat.format(Date())
}
