package com.embedsuite.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportHelper(
    private val context: Context,
    private val repository: SignalRepository
) {
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    suspend fun exportJson(): Result<File> = runCatching {
        val signals = repository.getRecent()
        val array = JSONArray()
        signals.forEach { signal ->
            array.put(
                JSONObject().apply {
                    put("id", signal.id)
                    put("timestamp", signal.timestamp)
                    put("type", signal.signalType)
                    put("name", signal.name)
                    put("frequency", signal.frequency)
                    put("protocol", signal.protocol)
                    put("deviceId", signal.deviceId)
                    put("mac", signal.macAddress)
                    put("rssi", signal.rssi)
                    put("latitude", signal.latitude ?: JSONObject.NULL)
                    put("longitude", signal.longitude ?: JSONObject.NULL)
                    put("rawData", signal.rawData)
                    put("detail", signal.detail)
                    put("label", signal.label)
                    put("tags", signal.tags)
                    put("favorite", signal.favorite)
                }
            )
        }

        val file = File(exportDir(), "embedsuite_${dateFormat.format(Date())}.json")
        file.writeText(
            JSONObject()
                .put("exportedAt", System.currentTimeMillis())
                .put("count", signals.size)
                .put("signals", array)
                .toString(2)
        )
        file
    }

    suspend fun exportCsv(): Result<File> = runCatching {
        val signals = repository.getRecent()
        val header = "id,timestamp,type,name,frequency,protocol,deviceId,mac,rssi,latitude,longitude,detail"
        val rows = signals.joinToString("\n") { s ->
            listOf(
                s.id,
                s.timestamp,
                s.signalType,
                csvEscape(s.name),
                csvEscape(s.frequency),
                csvEscape(s.protocol),
                csvEscape(s.deviceId),
                csvEscape(s.macAddress),
                s.rssi,
                s.latitude ?: "",
                s.longitude ?: "",
                csvEscape(s.detail)
            ).joinToString(",")
        }

        val file = File(exportDir(), "embedsuite_${dateFormat.format(Date())}.csv")
        file.writeText("$header\n$rows")
        file
    }

    suspend fun exportKml(): Result<File> = runCatching {
        val signals = repository.getRecent().filter { it.latitude != null && it.longitude != null }
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<kml xmlns="http://www.opengis.net/kml/2.2">""")
        sb.appendLine("<Document><name>EMBED SUITE War-Driving</name>")
        signals.forEach { s ->
            sb.appendLine("<Placemark>")
            sb.appendLine("<name>${s.label.ifBlank { s.protocol }} [${s.signalType}]</name>")
            sb.appendLine("<description>RSSI:${s.rssi} ${s.detail}</description>")
            sb.appendLine("<Point><coordinates>${s.longitude},${s.latitude},0</coordinates></Point>")
            sb.appendLine("</Placemark>")
        }
        sb.appendLine("</Document></kml>")
        val file = File(exportDir(), "embedsuite_${dateFormat.format(Date())}.kml")
        file.writeText(sb.toString())
        file
    }

    private fun exportDir(): File {
        val dir = File(context.getExternalFilesDir(null), "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun csvEscape(value: String): String {
        return if (value.contains(',') || value.contains('"')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
