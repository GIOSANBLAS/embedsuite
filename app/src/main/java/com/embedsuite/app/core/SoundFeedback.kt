package com.embedsuite.app.core

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.min
import kotlin.math.sin

object SoundFeedback {

    private var enabled = false

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    /** Retro terminal beep — square-ish wave, low volume, short duration. */
    private fun playBeep(frequencyHz: Int, durationMs: Int, volume: Float = 0.07f) {
        if (!enabled) return
        Thread {
            try {
                val sampleRate = 22050
                val sampleCount = sampleRate * durationMs / 1000
                val buffer = ShortArray(sampleCount)
                for (i in buffer.indices) {
                    val attack = min(1f, i.toFloat() / (sampleCount * 0.08f))
                    val release = min(1f, (sampleCount - i).toFloat() / (sampleCount * 0.25f))
                    val envelope = attack * release
                    val sample = sin(2.0 * Math.PI * frequencyHz * i / sampleRate) * envelope * volume
                    buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep((durationMs + 30).toLong())
                track.stop()
                track.release()
            } catch (_: Exception) {
                // Silently ignore if audio unavailable
            }
        }.start()
    }

    fun init() { /* No-op — beeps generated on demand */ }

    fun playConnect() = playBeep(520, 70, 0.06f)

    fun playDisconnect() = playBeep(280, 90, 0.05f)

    fun playCapture() = playBeep(740, 45, 0.06f)

    fun playError() = playBeep(180, 120, 0.07f)

    fun playKey() = playBeep(640, 20, 0.04f)

    fun playSuccess() = playBeep(660, 55, 0.06f)

    fun release() { /* AudioTrack released per beep */ }
}
