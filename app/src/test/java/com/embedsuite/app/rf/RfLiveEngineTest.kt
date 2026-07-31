package com.embedsuite.app.rf

import org.junit.Assert.*
import org.junit.Test

class RfLiveEngineTest {

    @Test
    fun feed_rssiUpdatesSpectrum() {
        val next = RfLiveEngine.feed(RfLiveSnapshot(), "RSSI: -72 dBm", "433.92")
        assertEquals(128, next.spectrumBins.size)
        assertEquals(-72f, next.lastRssiDbm)
        assertTrue(next.waterfall.isNotEmpty())
    }

    @Test
    fun feed_rawPulseUpdatesWaveform() {
        val line = "RAW_Data: 500 200 500 300"
        val next = RfLiveEngine.feed(RfLiveSnapshot(), line, "433.92")
        assertTrue(next.waveform.isNotEmpty())
        assertTrue(next.totalPulseUs > 0)
    }
}
