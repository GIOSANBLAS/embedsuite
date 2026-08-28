package com.embedsuite.app.core.ble

import android.bluetooth.BluetoothDevice
import com.embedsuite.app.connection.BleTransport
import com.embedsuite.app.connection.TEmbedTransport

/** Gestor GATT Nordic UART — envuelve BleTransport para Módulo A. */
class BleGattManager(
    private val transport: BleTransport
) {
    val isConnected: Boolean get() = transport.isConnected

    suspend fun connect(device: BluetoothDevice) = transport.connectToDevice(device)

    suspend fun connectAuto() = transport.connect()

    suspend fun disconnect() = transport.disconnect()

    suspend fun sendLine(line: String) = transport.sendCommand(line)

    fun transport(): TEmbedTransport = transport
}
