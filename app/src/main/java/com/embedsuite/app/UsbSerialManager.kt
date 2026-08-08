package com.embedsuite.app

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.embedsuite.app.flash.Esp32UsbIds
import com.hoho.android.usbserial.driver.ProbeTable
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

        /** Prober con tabla Espressif + CDC estándar. */
        fun createProber(): UsbSerialProber {
            val table = ProbeTable()
            table.addProduct(Esp32UsbIds.VENDOR_ESPRESSIF, Esp32UsbIds.PID_USB_JTAG, com.hoho.android.usbserial.driver.CdcAcmSerialDriver::class.java)
            table.addProduct(Esp32UsbIds.VENDOR_ESPRESSIF, Esp32UsbIds.PID_USB_OTG, com.hoho.android.usbserial.driver.CdcAcmSerialDriver::class.java)
            table.addProduct(Esp32UsbIds.VENDOR_ESPRESSIF, Esp32UsbIds.PID_BOOTLOADER, com.hoho.android.usbserial.driver.CdcAcmSerialDriver::class.java)
            table.addProduct(Esp32UsbIds.VENDOR_ESPRESSIF, Esp32UsbIds.PID_XIBALBA_CDC, com.hoho.android.usbserial.driver.CdcAcmSerialDriver::class.java)
            return UsbSerialProber(table)
        }
    }

    private val espressifProber = createProber()

    fun listarDispositivosConectados(): List<UsbDevice> {
        val devices = usbManager.deviceList.values.toList()
        val espressif = devices.filter { Esp32UsbIds.isEspressifDevice(it) }
        return if (espressif.isNotEmpty()) {
            espressif.sortedByDescending { Esp32UsbIds.deviceScore(it) }
        } else {
            devices
        }
    }

    fun mejorDispositivo(): UsbDevice? = Esp32UsbIds.pickBestDevice(listarDispositivosConectados())

    fun tienePermiso(device: UsbDevice): Boolean = usbManager.hasPermission(device)

    /**
     * Solicita permiso USB vía Activity (fiable en MIUI/Xiaomi).
     * PendingIntent único por deviceId + EXTRA_DEVICE en el Intent.
     */
    fun solicitarPermiso(device: UsbDevice) {
        val intent = android.content.Intent(ACTION_USB_PERMISSION).apply {
            setClassName(context.packageName, MainActivity::class.java.name)
            putExtra(UsbManager.EXTRA_DEVICE, device)
            addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val requestCode = device.deviceId
        val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        val permissionIntent = android.app.PendingIntent.getActivity(context, requestCode, intent, flags)
        usbManager.requestPermission(device, permissionIntent)
    }

    /** Pide permiso al mejor dispositivo TEH-Link si aún no lo tiene. */
    fun solicitarPermisoMejorDispositivo(): UsbDevice? {
        val device = mejorDispositivo() ?: return null
        if (tienePermiso(device)) return device
        solicitarPermiso(device)
        return null
    }

    private fun findDriver(device: UsbDevice): UsbSerialDriver? {
        return espressifProber.findAllDrivers(usbManager).firstOrNull { it.device == device }
            ?: UsbSerialProber.getDefaultProber().findAllDrivers(usbManager).firstOrNull { it.device == device }
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

        val driver = findDriver(device)
            ?: run {
                onError("No se encontró driver USB para ${device.deviceName} (VID=${device.vendorId} PID=${device.productId}).")
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
            try { port.close() } catch (_: IOException) {}
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
        try { serialPort?.close() } catch (_: IOException) {}
        serialPort = null
        try { rawConnection?.close() } catch (_: Exception) {}
        rawConnection = null
    }

    fun resumeAfterFlash() {
        pausedForFlash = false
    }

    /**
     * Secuencia esptool "default_reset" + entrada a download mode (GPIO0 + EN).
     * Funciona con UART bridge; en native USB puede requerir Encoder+RST manual.
     */
    fun enterDownloadMode() {
        val port = serialPort ?: return
        try {
            port.dtr = true   // IO0 LOW → bootloader
            port.rts = true   // EN LOW → reset
            Thread.sleep(100)
            port.rts = false  // EN HIGH → run
            Thread.sleep(50)
            port.dtr = false
            Thread.sleep(100)
        } catch (_: Exception) {}
    }

    fun openRawPort(device: UsbDevice, baudRate: Int): Boolean {
        closeRawPort()

        val driver = findDriver(device) ?: return false
        if (!usbManager.hasPermission(device)) return false

        rawConnection = usbManager.openDevice(device) ?: return false
        val port = driver.ports.firstOrNull() ?: return false

        serialPort = port
        return try {
            port.open(rawConnection)
            port.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            port.dtr = false
            port.rts = false
            purgeInput()
            true
        } catch (_: IOException) {
            closeRawPort()
            false
        }
    }

    fun purgeInput() {
        val port = serialPort ?: return
        val buf = ByteArray(256)
        try {
            repeat(8) {
                val n = port.read(buf, 50)
                if (n <= 0) return
            }
        } catch (_: IOException) {}
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
        try { serialPort?.close() } catch (_: IOException) {}
        serialPort = null
        try { rawConnection?.close() } catch (_: Exception) {}
        rawConnection = null
    }

    fun desconectar() {
        ioManager?.stop()
        ioManager = null
        try { serialPort?.close() } catch (_: IOException) {}
        serialPort = null
        try { rawConnection?.close() } catch (_: Exception) {}
        rawConnection = null
    }
}
