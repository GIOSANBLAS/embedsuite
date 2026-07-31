package com.embedsuite.app.rf

import com.embedsuite.app.connection.BruceResponseParser

object RfLiveEngine {
    const val SPECTRUM_BINS = 128
    const val WATERFALL_ROWS = 64
    const val MAX_WAVEFORM = 400

    fun feed(current: RfLiveSnapshot, line: String, centerMhz: String): RfLiveSnapshot {
        var next = current.copy(centerFreqMhz = centerMhz)

        BruceResponseParser.parseSpectrumRow(line)?.let { row ->
            next = next.copy(
                spectrumBins = row,
                waterfall = (next.waterfall + listOf(row.map { binToRssi(it) })).takeLast(WATERFALL_ROWS),
                lastRssiDbm = row.maxOrNull()?.let(::binToRssi)
            )
            return next
        }

        BruceResponseParser.parseRssiDbm(line)?.let { rssi ->
            val bin = rssiToBin(rssi)
            val bins = rollSpectrum(next.spectrumBins, bin)
            val row = List(SPECTRUM_BINS) { idx ->
                val dist = kotlin.math.abs(idx - SPECTRUM_BINS / 2) / (SPECTRUM_BINS / 2f)
                rssi - dist * 18f
            }
            next = next.copy(
                spectrumBins = bins,
                waterfall = (next.waterfall + listOf(row)).takeLast(WATERFALL_ROWS),
                lastRssiDbm = rssi
            )
        }

        if (line.contains("RAW", ignoreCase = true)) {
            val pulses = BruceResponseParser.parseRawPulseTrain(line)
            if (pulses.isNotEmpty()) {
                val totalUs = pulses.sumOf { it.second }
                next = next.copy(
                    waveform = (next.waveform + pulses).takeLast(MAX_WAVEFORM),
                    pulseCount = next.pulseCount + pulses.size,
                    totalPulseUs = next.totalPulseUs + totalUs
                )
            }
        }

        BruceResponseParser.parsePulseSample(line)?.let { (level, us) ->
            next = next.copy(
                waveform = (next.waveform + (level to us)).takeLast(MAX_WAVEFORM),
                pulseCount = next.pulseCount + 1,
                totalPulseUs = next.totalPulseUs + us
            )
        }

        return next
    }

    fun reset(centerMhz: String = "433.92") = RfLiveSnapshot(centerFreqMhz = centerMhz)

    private fun rollSpectrum(current: List<Float>, newBin: Float): List<Float> {
        val base = if (current.size == SPECTRUM_BINS) current.drop(1) else List(SPECTRUM_BINS - 1) { 0f }
        return base + newBin
    }

    private fun rssiToBin(rssi: Float): Float = ((rssi + 110f) / 90f).coerceIn(0f, 1f)

    private fun binToRssi(bin: Float): Float = (bin * 90f - 110f).coerceIn(-110f, -10f)
}
