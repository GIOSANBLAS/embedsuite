package com.embedsuite.app.connection

import android.content.Context
import com.embedsuite.app.UsbSerialManager

/**
 * Factory for [TEmbedTransport] — alineado con Bruce stock (USB/BLE CLI + WiFi WebUI).
 */
object TransportFactory {

    fun create(
        type: TransportType,
        usbSerialManager: UsbSerialManager,
        context: Context,
        wifiHost: String = WifiTransport.DEFAULT_HOST,
        @Suppress("UNUSED_PARAMETER") wifiPort: Int = 0,
        useMock: Boolean = false
    ): TEmbedTransport {
        if (useMock) return MockTransport()
        return when (type) {
            TransportType.USB -> UsbTransport(usbSerialManager)
            TransportType.WIFI -> WifiTransport(wifiHost)
            TransportType.BLE -> BleTransport(context)
        }
    }

    fun supportedTypes(): List<TransportType> = TransportType.entries

    fun displayName(type: TransportType): String = when (type) {
        TransportType.USB -> "USB serial (115200)"
        TransportType.WIFI -> "WiFi WebUI (/cm)"
        TransportType.BLE -> "BLE Bruce API"
    }
}
