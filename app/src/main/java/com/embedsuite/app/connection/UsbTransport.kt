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

    /**
     * Protocolo Bruce `storage write`: comando → líneas de contenido → línea `EOF`.
     * Usa `\n` (sin `\r`) para que `readStringUntil('\n')` coincida con `EOF`.
     */
    suspend fun writeTextFile(relativePath: String, content: String): Result<String> {
        if (!isConnected) {
            return Result.failure(Exception("USB no conectado. Push .sub requiere OTG."))
        }
        val path = runCatching { BruceCommands.sanitizeDeviceRelativePath(relativePath) }
            .getOrElse { return Result.failure(it) }
        val normalized = BruceCommands.preparePushContent(content).getOrElse {
            return Result.failure(it)
        }
        val sizeHint = (normalized.length + 64).coerceAtLeast(256)

        // Carpeta padre (ignorar error si ya existe)
        val parent = path.substringBeforeLast('/', "")
        if (parent.isNotBlank()) {
            val mkdir = BruceCommandValidator.validate(BruceCommands.storageMkdir(parent))
                .getOrElse { return Result.failure(it) }
            usbSerialManager.enviarTexto(mkdir, appendNewline = "\n")
            kotlinx.coroutines.delay(200)
        }

        val writeCmd = BruceCommandValidator.validate(BruceCommands.storageWrite(path, sizeHint))
            .getOrElse { return Result.failure(it) }
        val cmdOk = usbSerialManager.enviarTexto(writeCmd, appendNewline = "\n")
        if (!cmdOk) return Result.failure(Exception("No se pudo iniciar storage write."))

        kotlinx.coroutines.delay(350)

        normalized.lineSequence().forEach { line ->
            if (!usbSerialManager.enviarTexto(line, appendNewline = "\n")) {
                return Result.failure(Exception("Error enviando contenido .sub"))
            }
            kotlinx.coroutines.delay(5)
        }
        if (!usbSerialManager.enviarTexto(BruceCommands.STORAGE_WRITE_EOF, appendNewline = "\n")) {
            return Result.failure(Exception("Error enviando EOF"))
        }
        kotlinx.coroutines.delay(400)
        return Result.success(path)
    }

    override fun incomingLines(): Flow<String> = _incoming.asSharedFlow()
}
