package com.embedsuite.app.connection

import android.hardware.usb.UsbDevice
import com.embedsuite.app.UsbSerialManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class UsbTransport(
    private val usbSerialManager: UsbSerialManager
) : TEmbedTransport {

    override val type = TransportType.USB

    private val _incoming = MutableSharedFlow<String>(extraBufferCapacity = 512)
    private var connectedDevice: UsbDevice? = null

    /** Buffer acumulador para líneas parciales (USB CDC envía chunks arbitrarios). */
    private val lineBuffer = StringBuilder(4096)

    override val isConnected: Boolean
        get() = connectedDevice != null

    override suspend fun connect(): Result<String> {
        val devices = usbSerialManager.listarDispositivosConectados()
        if (devices.isEmpty()) {
            return Result.failure(Exception("No hay dispositivo USB conectado via OTG."))
        }

        val device = devices.firstOrNull()
            ?: return Result.failure(Exception("No hay dispositivo USB conectado via OTG (lista vacía)."))
        var connectError: String? = null
        lineBuffer.setLength(0)

        val success = usbSerialManager.conectar(
            device = device,
            baudRate = 115200,
            onDataReceived = { data ->
                processIncomingData(data)
            },
            onError = { error ->
                connectError = error
                if (error.contains("Desconexión", ignoreCase = true)) {
                    connectedDevice = null
                    lineBuffer.setLength(0)
                }
            }
        )

        return if (success) {
            connectedDevice = device
            Result.success("USB: ${device.deviceName}")
        } else {
            Result.failure(Exception(connectError ?: "No se pudo conectar por USB."))
        }
    }

    override suspend fun disconnect() {
        usbSerialManager.desconectar()
        connectedDevice = null
        lineBuffer.setLength(0)
    }

    override suspend fun sendCommand(command: String): Result<String> {
        if (!isConnected) {
            return Result.failure(Exception("USB no conectado."))
        }
        val sent = usbSerialManager.enviarTexto(command.trim())
        return if (sent) {
            Result.success("OK")
        } else {
            Result.failure(Exception("Error al enviar comando por USB."))
        }
    }

    override fun incomingLines(): Flow<String> = _incoming.asSharedFlow()

    /**
     * Procesa un chunk de datos entrante y emite líneas completas por `_incoming`.
     * Las líneas parciales se quedan acumuladas en `lineBuffer` hasta que llegue un `\n`.
     */
    private fun processIncomingData(data: String) {
        if (data.isEmpty()) return
        synchronized(lineBuffer) {
            lineBuffer.append(data)
            var lastNewline: Int
            while (lineBuffer.indexOf('\n').also { lastNewline = it } >= 0) {
                val rawLine = lineBuffer.substring(0, lastNewline)
                lineBuffer.delete(0, lastNewline + 1)
                val clean = rawLine.trimEnd('\r')
                if (clean.isNotBlank()) {
                    _incoming.tryEmit(clean)
                }
            }
            // Evitamos crecimiento ilimitado si el stream no envía \n nunca (>16KB)
            if (lineBuffer.length > 16384) {
                lineBuffer.setLength(0)
            }
        }
    }
}
