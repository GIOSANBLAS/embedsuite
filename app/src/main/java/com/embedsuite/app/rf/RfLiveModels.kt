package com.embedsuite.app.rf

data class RfLiveSnapshot(
    val spectrumBins: List<Float> = emptyList(),
    val waterfall: List<List<Float>> = emptyList(),
    val waveform: List<Pair<Float, Long>> = emptyList(),
    val lastRssiDbm: Float? = null,
    val centerFreqMhz: String = "433.92",
    val totalPulseUs: Long = 0L,
    val pulseCount: Int = 0
)
