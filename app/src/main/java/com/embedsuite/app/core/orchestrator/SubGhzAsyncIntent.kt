package com.embedsuite.app.core.orchestrator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.embedsuite.app.core.connection.TransportTask
import java.io.File
import kotlin.math.abs

/** MÓDULO B — Sub-GHz async: capturar → transferir → analizar / replay .sub */
sealed class SubGhzIntent : Intent {

    data class Capture(
        val freqHz: Long,
        val seconds: Int,
        val freqMhz: Double = freqHz / 1_000_000.0,
        val downloadPath: String = "/bruce/subghz/embed_capture.sub"
    ) : SubGhzIntent(), AsyncCaptureIntent {
        override val label = "Sub-GHz Capture"
        override val uploadTask = TransportTask.CAPTURE_SUBGHZ
        override val triggerTask = TransportTask.CAPTURE_SUBGHZ
        override val triggerCommand: String =
            "subghz rx ${freqHz.coerceIn(300_000_000L, 928_000_000L)} ${seconds.coerceIn(1, 120)}"
        override val waitAfterTriggerMs: Long = (seconds.coerceIn(1, 120) + 3) * 1000L
        override val preferredDownloadPath: String? = downloadPath

        override fun resolveDownloadPath(cliResponse: String): String? {
            Regex("""(?i)(/[\w./-]+\.sub)""").find(cliResponse)?.groupValues?.get(1)?.let { return it }
            if (cliResponse.contains("Filetype:", ignoreCase = true)) return downloadPath
            return downloadPath
        }
    }

    data class Replay(
        val subContent: String,
        override val remotePath: String = "/bruce/subghz/embed_replay.sub",
        val repeat: Boolean = false
    ) : SubGhzIntent(), FileRunIntent {
        override val label = "Sub-GHz Replay"
        override val uploadTask = TransportTask.FILE_UPLOAD
        override val triggerTask = TransportTask.REPLAY_SUBGHZ

        override suspend fun prepare(context: Context): Result<PrepareResult> = runCatching {
            val f = File.createTempFile("embed_replay_", ".sub", context.cacheDir)
            f.writeText(subContent)
            PrepareResult(file = f)
        }

        override fun triggerCommand(remotePath: String): String =
            "subghz tx_from_file $remotePath $repeat"
    }
}

/** Señal capturada con forma de onda derivada del RAW Flipper .sub */
data class CapturedSignal(
    val subContent: String,
    val freqMhz: Double,
    val pulses: List<Long>,
    val localFile: File? = null
) {
    fun buildSubContent(): String = subContent

    fun trimSilence(thresholdUs: Long = 5_000L): CapturedSignal {
        val trimmed = pulses.filter { abs(it) > thresholdUs }
        return copy(pulses = trimmed.ifEmpty { pulses })
    }

    fun toWaveformBitmap(width: Int = 512, height: Int = 128): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(0xFF0A0A0A.toInt())
        if (pulses.isEmpty()) return bmp
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF00FF66.toInt()
            strokeWidth = 2f
        }
        val total = pulses.sumOf { abs(it).coerceAtLeast(1L) }.coerceAtLeast(1L)
        var x = 0f
        var high = false
        val mid = height / 2f
        pulses.forEach { dur ->
            val w = (abs(dur).toFloat() / total.toFloat()) * width
            val y = if (high) mid - height * 0.35f else mid + height * 0.35f
            canvas.drawLine(x, mid, x + w, y, paint)
            x += w
            high = !high
        }
        return bmp
    }

    companion object {
        fun fromSubContent(content: String, freqMhz: Double): CapturedSignal {
            val pulses = mutableListOf<Long>()
            Regex("""RAW_Data:\s*([0-9\s-]+)""", RegexOption.IGNORE_CASE).find(content)?.let { m ->
                m.groupValues[1].trim().split(Regex("\\s+")).mapNotNull { it.toLongOrNull() }.forEach { pulses += it }
            }
            if (pulses.isEmpty()) {
                Regex("""(?i)RAW:\s*([0-9\s-]+)""").find(content)?.let { m ->
                    m.groupValues[1].trim().split(Regex("\\s+")).mapNotNull { it.toLongOrNull() }.forEach { pulses += it }
                }
            }
            return CapturedSignal(content, freqMhz, pulses)
        }
    }
}

/** @deprecated Usar [SubGhzIntent.Capture] */
object SubGhzCaptureIntent {
    fun capture(freqHz: Long, seconds: Int): SubGhzIntent.Capture =
        SubGhzIntent.Capture(freqHz, seconds)

    fun scan(startHz: Long = 300_000_000L, endHz: Long = 928_000_000L): DirectCliIntent =
        DirectCliIntent("Sub-GHz Scan", "subghz scan $startHz $endHz", TransportTask.CLI_TRIGGER)
}
