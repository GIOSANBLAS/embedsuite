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
import com.embedsuite.app.core.bruce.BruceGatt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/**
 * Transporte BLE nativo Bruce (Serial Service + Battery Service).
 * Sin Nordic UART, sin TEH-Link JSON, sin espejo de pantalla.
 */
class BleTransport(private val context: Context) : TEmbedTransport, BruceBleCapable {

    override val type = TransportType.BLE

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _incoming = MutableSharedFlow<String>(extraBufferCapacity = 128)
    private var gatt: BluetoothGatt? = null
    private var serialCharacteristic: BluetoothGattCharacteristic? = null
    private var batteryCharacteristic: BluetoothGattCharacteristic? = null
    private var connected = false
    private var connectDeferred: CompletableDeferred<Result<String>>? = null
    private var batteryLevel: Int? = null
    private val batteryMutex = Mutex()

    override val isConnected: Boolean get() = connected

    override suspend fun readBatteryLevel(): Int? = batteryMutex.withLock { batteryLevel }

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
        if (!btAdapter.isEnabled) return Result.failure(Exception("Activa Bluetooth en el teléfono."))

        val candidates = btAdapter.bondedDevices.filter {
            val name = it.name?.lowercase() ?: ""
            name.contains("t-embed") || name.contains("bruce") || name.contains("bruc") ||
                name.contains("lilygo") || name.contains("esp32")
        }
        val device = candidates.firstOrNull()
            ?: return Result.failure(Exception("No hay Bruce emparejado. Empareja el T-Embed en Ajustes Bluetooth y activa BLE API en Bruce."))
        return connectToDevice(device)
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        connected = false
        batteryLevel = null
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        serialCharacteristic = null
        batteryCharacteristic = null
    }

    @SuppressLint("MissingPermission")
    override suspend fun sendCommand(command: String): Result<String> {
        val char = serialCharacteristic
            ?: return Result.failure(Exception("BLE Bruce no conectado (Serial Service)."))
        val data = (command.trim() + "\n").toByteArray(Charsets.UTF_8)
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        char.value = data
        val ok = gatt?.writeCharacteristic(char) == true
        return if (ok) Result.success("OK") else Result.failure(Exception("Error escribiendo por BLE."))
    }

    override fun incomingLines() = _incoming.asSharedFlow()

    @SuppressLint("MissingPermission")
    private fun enableNotifications(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        g.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(BruceGatt.CCCD)
        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        g.writeDescriptor(descriptor)
    }

    @SuppressLint("MissingPermission")
    private fun readBattery(g: BluetoothGatt) {
        val battChar = batteryCharacteristic ?: return
        g.readCharacteristic(battChar)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> g.discoverServices()
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

            val serialService = g.getService(BruceGatt.SERIAL_SERVICE)
            if (serialService == null) {
                connectDeferred?.complete(
                    Result.failure(Exception("Serial Service Bruce no encontrado. Activa BLE API en Config de Bruce."))
                )
                return
            }

            serialCharacteristic = serialService.getCharacteristic(BruceGatt.SERIAL_CHAR)
            if (serialCharacteristic == null) {
                connectDeferred?.complete(Result.failure(Exception("Característica serial Bruce no encontrada.")))
                return
            }

            enableNotifications(g, serialCharacteristic!!)

            val batteryService = g.getService(BruceGatt.BATTERY_SERVICE)
            batteryCharacteristic = batteryService?.getCharacteristic(BruceGatt.BATTERY_LEVEL)
            batteryCharacteristic?.let { enableNotifications(g, it) }

            connected = true
            readBattery(g)
            connectDeferred?.complete(Result.success("BLE Bruce: ${g.device.name ?: g.device.address}"))
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicValue(characteristic, value)
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            val value = characteristic.value ?: return
            handleCharacteristicValue(characteristic, value)
        }

        override fun onCharacteristicRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == BruceGatt.BATTERY_LEVEL) {
                if (value.isNotEmpty()) {
                    batteryLevel = value[0].toInt() and 0xFF
                }
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            @Suppress("DEPRECATION")
            val value = characteristic.value
            if (status == BluetoothGatt.GATT_SUCCESS &&
                characteristic.uuid == BruceGatt.BATTERY_LEVEL &&
                value != null &&
                value.isNotEmpty()
            ) {
                batteryLevel = value[0].toInt() and 0xFF
            }
        }
    }

    private fun handleCharacteristicValue(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        when (characteristic.uuid) {
            BruceGatt.BATTERY_LEVEL -> {
                if (value.isNotEmpty()) batteryLevel = value[0].toInt() and 0xFF
            }
            BruceGatt.SERIAL_CHAR -> {
                val text = String(value, Charsets.UTF_8)
                text.lines().filter { it.isNotBlank() }.forEach { _incoming.tryEmit(it) }
            }
        }
    }
}
