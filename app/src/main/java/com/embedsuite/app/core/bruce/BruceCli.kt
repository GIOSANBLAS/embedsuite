package com.embedsuite.app.core.bruce

import com.embedsuite.app.connection.TEmbedTransport
import com.embedsuite.app.connection.TransportType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Utilidades CLI alineadas con BruceDevices/firmware wiki Serial. */
object BruceCli {

    /** Bruce `subghz scan` espera frecuencias en Hz (ej. 433920000). */
    fun mhzToHz(mhz: Double): Long = (mhz * 1_000_000.0).toLong()

    fun mhzStringToHz(mhz: String): Long? =
        mhz.replace(Regex("[^0-9.]"), "").toDoubleOrNull()?.let { mhzToHz(it) }

    /** Menú loader — nombres reales del main menu Bruce. */
    object LoaderApps {
        const val WIFI = "WiFi"
        const val BLE = "BLE"
        const val IR = "IR"
        const val RFID = "RFID"
        const val RF = "RF"
    }

    /** Nav remoto — firmware compara en minúsculas (`nav == "esc"`). */
    object Nav {
        const val ESC = "nav esc"
        const val UP = "nav up"
        const val DOWN = "nav down"
        const val SELECT = "nav select"
        const val NEXT = "nav next"
        const val PREV = "nav prev"
    }

    /**
     * Envía comando y recoge respuesta:
     * - WiFi WebUI: cuerpo HTTP en [TEmbedTransport.sendCommand].
     * - BLE/USB: líneas NOTIFY/serial hasta prompt `#` o timeout.
     */
    suspend fun sendAndCollect(
        transport: TEmbedTransport,
        command: String,
        timeoutMs: Long = 4_000L
    ): Result<String> {
        if (!transport.isConnected) {
            return Result.failure(Exception("Sin transporte Bruce conectado."))
        }
        val trimmed = command.trim()
        if (trimmed.isBlank()) {
            return Result.failure(Exception("Comando vacío."))
        }

        val sendResult = transport.sendCommand(trimmed)
        if (sendResult.isFailure) return sendResult

        if (transport.type == TransportType.WIFI) {
            val body = sendResult.getOrNull().orEmpty()
            return Result.success(body.trim().ifBlank { "OK" })
        }

        delay(80)
        return Result.success(collectSerialLines(transport, timeoutMs))
    }

    private suspend fun collectSerialLines(transport: TEmbedTransport, timeoutMs: Long): String {
        val lines = mutableListOf<String>()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0) break
            val line = withTimeoutOrNull(remaining.coerceAtMost(800L)) {
                transport.incomingLines().first()
            } ?: continue
            val t = line.trim()
            if (t.isBlank()) continue
            if (t == "#" || t.startsWith("# ")) break
            if (t.startsWith("COMMAND:")) continue
            lines.add(line)
        }
        return lines.joinToString("\n").trim()
    }
}
