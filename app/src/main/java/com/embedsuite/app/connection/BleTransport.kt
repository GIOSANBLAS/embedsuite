package com.embedsuite.app.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import kotlin.coroutines.resume

class BleTransport(private val context: Context) : TEmbedTransport {

    override val type = TransportType.BLE

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _incoming = MutableSharedFlow<String>(extraBufferCapacity = 128)
    private var gatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var connected = false
    private var connectDeferred: CompletableDeferred<Result<String>>? = null

    override val isConnected: Boolean get() = connected

    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(device: BluetoothDevice): Result<String> {
        disconnect()
        connectDeferred = CompletableDeferred()

        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)

        return try {
            connectDeferred?.await() ?: Result.failure(Exception("BLE connect timeout"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(): Result<String> {
        val btAdapter = adapter ?: return Result.failure(Exception("Bluetooth no disponible."))
        if (!btAdapter.isEnabled) return Result.failure(Exception("Activa Bluetooth en el Xiaomi."))

        val candidates = btAdapter.bondedDevices.filter {
            val name = it.name?.lowercase() ?: ""
            name.contains("t-embed") || name.contains("bruce") || name.contains("lilygo") || name.contains("esp32")
        }

        val device = candidates.firstOrNull()
            ?: return Result.failure(Exception("No se encontró T-Embed/Bruce emparejado. Empareja el dispositivo en Ajustes Bluetooth."))

        return connectToDevice(device)
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        connected = false
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        txCharacteristic = null
    }

    @SuppressLint("MissingPermission")
    override suspend fun sendCommand(command: String): Result<String> {
        val char = txCharacteristic
            ?: return Result.failure(Exception("BLE no conectado."))

        val data = (command.trim() + "\n").toByteArray()
        char.value = data
        val ok = gatt?.writeCharacteristic(char) == true
        return if (ok) Result.success("OK") else Result.failure(Exception("Error escribiendo por BLE."))
    }

    override fun incomingLines() = _incoming.asSharedFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connected = false
                    connectDeferred?.complete(Result.failure(Exception("BLE desconectado.")))
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connectDeferred?.complete(Result.failure(Exception("BLE service discovery failed")))
                return
            }

            val service = g.getService(NORDIC_UART_SERVICE)
            if (service == null) {
                connectDeferred?.complete(Result.failure(Exception("Nordic UART no encontrado en el dispositivo.")))
                return
            }

            txCharacteristic = service.getCharacteristic(NORDIC_UART_TX)
            val rxCharacteristic = service.getCharacteristic(NORDIC_UART_RX)

            if (rxCharacteristic != null) {
                g.setCharacteristicNotification(rxCharacteristic, true)
                val descriptor = rxCharacteristic.getDescriptor(CCCD)
                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                g.writeDescriptor(descriptor)
            }

            connected = true
            connectDeferred?.complete(Result.success("BLE: ${g.device.name ?: g.device.address}"))
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val text = String(value)
            text.lines().filter { it.isNotBlank() }.forEach { _incoming.tryEmit(it) }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            val value = characteristic.value ?: return
            val text = String(value)
            text.lines().filter { it.isNotBlank() }.forEach { _incoming.tryEmit(it) }
        }
    }

    companion object {
        val NORDIC_UART_SERVICE: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val NORDIC_UART_RX: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        val NORDIC_UART_TX: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
