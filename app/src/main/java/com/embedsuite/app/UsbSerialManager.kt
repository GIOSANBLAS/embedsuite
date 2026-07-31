package com.embedsuite.app

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException
import java.nio.charset.Charset

class UsbSerialManager(private val context: android.content.Context) {
    private val usbManager: UsbManager = context.getSystemService(android.content.Context.USB_SERVICE) as UsbManager
    private var serialPort: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null
    private var rawConnection: android.hardware.usb.UsbDeviceConnection? = null
    private var dataCallback: ((String) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    private var pausedForFlash = false

    companion object {
        const val ACTION_USB_PERMISSION = "com.embedsuite.app.USB_PERMISSION"
    }

    fun listarDispositivosConectados(): List<UsbDevice> {
        return usbManager.deviceList.values.toList()
    }

    fun tienePermiso(device: UsbDevice): Boolean {
        return usbManager.hasPermission(device)
    }

    fun solicitarPermiso(device: UsbDevice) {
        val flags = android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        val intent = android.content.Intent(ACTION_USB_PERMISSION)
        val permissionIntent = android.app.PendingIntent.getBroadcast(context, 0, intent, flags)
        usbManager.requestPermission(device, permissionIntent)
    }

    fun conectar(
        device: UsbDevice,
        baudRate: Int = 115200,
        onDataReceived: (String) -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        if (pausedForFlash) {
            onError("Puerto USB en modo flasheo.")
            return false
        }

        dataCallback = onDataReceived
        errorCallback = onError
        desconectar()

        val availableDrivers: List<UsbSerialDriver> = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        val driver = availableDrivers.firstOrNull { it.device == device }
            ?: run {
                onError("No se encontró driver compatible para el dispositivo USB.")
                return false
            }

        if (!usbManager.hasPermission(device)) {
            solicitarPermiso(device)
            onError("Solicitando permisos USB...")
            return false
        }

        val connection = usbManager.openDevice(driver.device)
            ?: run {
                onError("No se pudo abrir la conexión USB.")
                return false
            }

        val port = driver.ports.firstOrNull()
            ?: run {
                connection.close()
                onError("El dispositivo no tiene puertos serie válidos.")
                return false
            }

        serialPort = port
        try {
            port.open(connection)
            port.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            port.dtr = true
            port.rts = true
        } catch (e: IOException) {
            try {
                port.close()
            } catch (_: IOException) {
            }
            connection.close()
            serialPort = null
            onError("Error al abrir puerto: ${e.message}")
            return false
        }

        ioManager = SerialInputOutputManager(port, object : SerialInputOutputManager.Listener {
            override fun onNewData(data: ByteArray?) {
                data?.let { onDataReceived(String(it, Charsets.UTF_8)) }
            }

            override fun onRunError(e: Exception?) {
                onError("Desconexión o error de bus: ${e?.message}")
            }
        }).apply { start() }

        return true
    }

    fun enviarTexto(texto: String, appendNewline: String = "\r\n", charset: Charset = Charsets.UTF_8): Boolean {
        val port = serialPort ?: return false
        return try {
            port.write((texto + appendNewline).toByteArray(charset), 2000)
            true
        } catch (_: IOException) {
            false
        }
    }

    fun pauseForFlash() {
        pausedForFlash = true
        ioManager?.stop()
        ioManager = null
        try {
            serialPort?.close()
        } catch (_: IOException) {}
        serialPort = null
    }

    fun resumeAfterFlash() {
        pausedForFlash = false
    }

    fun enterBootloader() {
        val port = serialPort ?: return
        try {
            port.dtr = false
            port.rts = true
            Thread.sleep(100)
            port.dtr = true
            port.rts = false
            Thread.sleep(50)
            port.dtr = false
        } catch (_: Exception) {}
    }

    fun openRawPort(device: UsbDevice, baudRate: Int): Boolean {
        closeRawPort()
        val driver = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            .firstOrNull { it.device == device } ?: return false

        if (!usbManager.hasPermission(device)) return false

        rawConnection = usbManager.openDevice(device) ?: return false
        val port = driver.ports.firstOrNull() ?: return false

        serialPort = port
        return try {
            port.open(rawConnection)
            port.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            true
        } catch (_: IOException) {
            closeRawPort()
            false
        }
    }

    fun setBaudRate(baudRate: Int): Boolean {
        val port = serialPort ?: return false
        return try {
            port.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            true
        } catch (_: IOException) {
            false
        }
    }

    fun writeRaw(data: ByteArray): Boolean {
        val port = serialPort ?: return false
        return try {
            port.write(data, 5000)
            true
        } catch (_: IOException) {
            false
        }
    }

    fun readRaw(timeoutMs: Int = 3000): ByteArray? {
        val port = serialPort ?: return null
        val buffer = ByteArray(4096)
        return try {
            val read = port.read(buffer, timeoutMs)
            if (read > 0) buffer.copyOf(read) else null
        } catch (_: IOException) {
            null
        }
    }

    fun closeRawPort() {
        try {
            serialPort?.close()
        } catch (_: IOException) {}
        serialPort = null
        rawConnection?.close()
        rawConnection = null
    }

    fun desconectar() {
        ioManager?.stop()
        ioManager = null
        try {
            serialPort?.close()
        } catch (_: IOException) {}
        serialPort = null
        rawConnection?.close()
        rawConnection = null
    }
}
