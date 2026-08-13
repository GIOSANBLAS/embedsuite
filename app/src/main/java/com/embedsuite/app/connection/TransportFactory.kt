package com.embedsuite.app.connection

import android.content.Context
import com.embedsuite.app.UsbSerialManager

/**
 * Central factory for [TEmbedTransport] implementations (USB, WiFi/TCP, BLE, Mock).
 */
object TransportFactory {

    fun create(
        type: TransportType,
        usbSerialManager: UsbSerialManager,
        context: Context,
        wifiHost: String = TcpTransport.DEFAULT_HOST,
        wifiPort: Int = TcpTransport.DEFAULT_PORT,
        useMock: Boolean = false
    ): TEmbedTransport {
        if (useMock) return MockTransport()
        return when (type) {
            TransportType.USB -> UsbTransport(usbSerialManager)
            TransportType.WIFI -> TcpTransport(host = wifiHost, port = wifiPort)
            TransportType.BLE -> BleTransport(context)
        }
    }

    fun supportedTypes(): List<TransportType> = TransportType.entries

    fun displayName(type: TransportType): String = when (type) {
        TransportType.USB -> "USB OTG"
        TransportType.WIFI -> "WiFi / TCP"
        TransportType.BLE -> "BLE UART"
    }
}
