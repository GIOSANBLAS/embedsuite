package com.embedsuite.app.services

import com.embedsuite.app.connection.XibalbaAdapter

/**
 * AudioService — feedback auditivo del speaker NS4168 vía TEH-Link.
 *
 * Beeps semánticos con las mismas frecuencias que el firmware
 * (src/core/audio_feedback.cpp) para que la UX sonora sea consistente
 * entre UI del dispositivo y acciones disparadas desde la app.
 */
class AudioService(
    private val xibalba: XibalbaAdapter
) {
    suspend fun beep(freqHz: Int = 1000, durationMs: Int = 100) {
        xibalba.audioBeep(freqHz, durationMs)
    }

    suspend fun feedbackOk() = beep(1200, 80)

    suspend fun feedbackError() {
        beep(300, 120)
        beep(300, 120)
    }

    suspend fun feedbackNfcDetected() = beep(1000, 100)

    suspend fun feedbackPacketCaptured() = beep(800, 50)

    suspend fun feedbackScanComplete() = beep(1200, 200)
}
