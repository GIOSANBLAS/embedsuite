package com.embedsuite.app.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.embedsuite.app.core.SessionStatsTracker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SessionReportGenerator(
    private val context: Context,
    private val signalRepository: SignalRepository,
    private val txHistoryRepository: TxHistoryRepository,
    private val sessionStats: SessionStatsTracker
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    data class FieldSessionMeta(
        val name: String,
        val startedAt: Long,
        val endedAt: Long = System.currentTimeMillis(),
        val frequencyMhz: String
    )

    suspend fun generateHtmlReport(): Result<File> = runCatching {
        writeHtml(
            title = "EMBED SUITE — Session Report",
            signals = signalRepository.getRecent(200),
            txHistory = txHistoryRepository.getRecent(50),
            extraStats = listOf(
                "Signals today" to sessionStats.signalsToday().toString(),
                "APs today" to sessionStats.apsToday().toString(),
                "Macros" to sessionStats.macrosToday().toString()
            ),
            filePrefix = "embed_session"
        )
    }

    suspend fun generatePdfReport(): Result<File> = runCatching {
        writePdf(
            title = "EMBED SUITE — Session Report",
            signals = signalRepository.getRecent(100),
            txHistory = txHistoryRepository.getRecent(30),
            extraLine = "Signals today: ${sessionStats.signalsToday()} | APs: ${sessionStats.apsToday()} | Macros: ${sessionStats.macrosToday()}",
            filePrefix = "embed_session"
        )
    }

    /** Reporte acotado a una sesión de modo campo. */
    suspend fun generateFieldSessionReport(meta: FieldSessionMeta): Result<File> = runCatching {
        val signals = signalRepository.getSince(meta.startedAt)
        val txHistory = txHistoryRepository.getSince(meta.startedAt)
        val durationMin = TimeUnit.MILLISECONDS.toMinutes((meta.endedAt - meta.startedAt).coerceAtLeast(0))
        val geoCount = signals.count { it.latitude != null && it.longitude != null }
        val rfCount = signals.count { it.signalType.equals("RF", true) }
        val wifiCount = signals.count { it.signalType.equals("WIFI", true) }
        val bleCount = signals.count { it.signalType.equals("BLE", true) }
        val favCount = signals.count { it.favorite }

        writeHtml(
            title = "EMBED SUITE — Field Session: ${meta.name.ifBlank { "Campo" }}",
            signals = signals,
            txHistory = txHistory,
            extraStats = listOf(
                "Sesión" to meta.name.ifBlank { "Campo" },
                "Freq" to "${meta.frequencyMhz} MHz",
                "Inicio" to dateFormat.format(Date(meta.startedAt)),
                "Fin" to dateFormat.format(Date(meta.endedAt)),
                "Duración" to "${durationMin} min",
                "Señales" to signals.size.toString(),
                "RF / WiFi / BLE" to "$rfCount / $wifiCount / $bleCount",
                "Con GPS" to geoCount.toString(),
                "Favoritos en sesión" to favCount.toString()
            ),
            mapPoints = signals.filter { it.latitude != null && it.longitude != null },
            filePrefix = "embed_field_${sanitizeFilePart(meta.name)}"
        )
    }

    private fun writeHtml(
        title: String,
        signals: List<CapturedSignalEntity>,
        txHistory: List<TxHistoryEntity>,
        extraStats: List<Pair<String, String>>,
        mapPoints: List<CapturedSignalEntity> = emptyList(),
        filePrefix: String
    ): File {
        val now = dateFormat.format(Date())
        val html = buildString {
            appendLine("<!DOCTYPE html><html><head><meta charset='utf-8'>")
            appendLine("<title>$title</title>")
            appendLine("<style>body{background:#0c1210;color:#00ff41;font-family:monospace;padding:20px}")
            appendLine("h1,h2{color:#00ffff}table{border-collapse:collapse;width:100%;margin-bottom:16px}")
            appendLine("td,th{border:1px solid #333;padding:6px;font-size:12px}")
            appendLine(".stat{display:inline-block;margin:6px;padding:10px;border:1px solid #00ff41}")
            appendLine(".fav{color:#ffaa00}.map{font-size:11px;color:#88ffcc}</style></head><body>")
            appendLine("<h1>$title</h1>")
            appendLine("<p>Generated: $now</p>")
            extraStats.forEach { (k, v) ->
                appendLine("<div class='stat'><b>$k</b><br>$v</div>")
            }
            if (mapPoints.isNotEmpty()) {
                appendLine("<h2>Geopuntos (${mapPoints.size})</h2><div class='map'>")
                mapPoints.take(80).forEach { s ->
                    appendLine(
                        "${s.signalType} ${s.label.ifBlank { s.protocol }} — " +
                            "%.5f, %.5f<br>".format(s.latitude, s.longitude)
                    )
                }
                appendLine("</div>")
            }
            appendLine("<h2>TX History</h2><table><tr><th>Time</th><th>Label</th><th>Protocol</th><th>OK</th></tr>")
            txHistory.forEach { tx ->
                appendLine(
                    "<tr><td>${dateFormat.format(Date(tx.timestamp))}</td><td>${esc(tx.label)}</td>" +
                        "<td>${esc(tx.protocol)}</td><td>${if (tx.success) "✓" else "✗"}</td></tr>"
                )
            }
            appendLine("</table><h2>Captured Signals (${signals.size})</h2><table>")
            appendLine("<tr><th>Type</th><th>Label</th><th>Protocol</th><th>Freq</th><th>RSSI</th><th>GPS</th><th>★</th></tr>")
            signals.forEach { s ->
                val gps = if (s.latitude != null && s.longitude != null) {
                    "%.4f,%.4f".format(s.latitude, s.longitude)
                } else "—"
                appendLine(
                    "<tr><td>${esc(s.signalType)}</td><td>${esc(s.label.ifBlank { s.name })}</td>" +
                        "<td>${esc(s.protocol)}</td><td>${esc(s.frequency)}</td><td>${s.rssi}</td>" +
                        "<td>$gps</td><td class='fav'>${if (s.favorite) "★" else ""}</td></tr>"
                )
            }
            appendLine("</table></body></html>")
        }

        val file = File(context.getExternalFilesDir(null), "${filePrefix}_${fileDateFormat.format(Date())}.html")
        file.writeText(html)
        return file
    }

    private fun writePdf(
        title: String,
        signals: List<CapturedSignalEntity>,
        txHistory: List<TxHistoryEntity>,
        extraLine: String,
        filePrefix: String
    ): File {
        val now = dateFormat.format(Date())
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 10f }
        var y = 40f

        canvas.drawText(title.take(60), 40f, y, titlePaint)
        y += 22f
        canvas.drawText("Generated: $now", 40f, y, bodyPaint)
        y += 16f
        canvas.drawText(extraLine.take(90), 40f, y, bodyPaint)
        y += 22f
        canvas.drawText("--- TX History ---", 40f, y, bodyPaint)
        y += 14f
        txHistory.take(15).forEach { tx ->
            canvas.drawText("${if (tx.success) "OK" else "FAIL"} ${tx.label} // ${tx.protocol}".take(70), 40f, y, bodyPaint)
            y += 12f
            if (y > 780f) return@forEach
        }
        y += 10f
        canvas.drawText("--- Signals (${signals.size}) ---", 40f, y, bodyPaint)
        y += 14f
        signals.take(25).forEach { s ->
            val star = if (s.favorite) "*" else " "
            canvas.drawText(
                "$star ${s.signalType}: ${s.label.ifBlank { s.protocol }} @ ${s.frequency}".take(70),
                40f, y, bodyPaint
            )
            y += 12f
            if (y > 800f) return@forEach
        }

        doc.finishPage(page)
        val file = File(context.getExternalFilesDir(null), "${filePrefix}_${fileDateFormat.format(Date())}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun sanitizeFilePart(name: String): String =
        name.trim().replace(Regex("""[^\w\-]+"""), "_").take(24).ifBlank { "campo" }

    private fun esc(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
