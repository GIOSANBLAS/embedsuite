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

    private val _incoming = MutableSharedFlow<String>(extraBufferCapacity = 128)
    private var connectedDevice: UsbDevice? = null

    override val isConnected: Boolean
        get() = connectedDevice != null

    override suspend fun connect(): Result<String> {
        val devices = usbSerialManager.listarDispositivosConectados()
        if (devices.isEmpty()) {
            return Result.failure(Exception("No hay dispositivo USB conectado via OTG."))
        }

        val device = devices.first()
        var connectError: String? = null

        val success = usbSerialManager.conectar(
            device = device,
            baudRate = 115200,
            onDataReceived = { data ->
                data.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        _incoming.tryEmit(line)
                    }
                }
            },
            onError = { error ->
                connectError = error
                if (error.contains("Desconexión", ignoreCase = true)) {
                    connectedDevice = null
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
}
