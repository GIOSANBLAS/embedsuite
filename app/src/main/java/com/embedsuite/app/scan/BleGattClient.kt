package com.embedsuite.app.scan

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

data class GattCharacteristicInfo(
    val uuid: String,
    val properties: Int,
    val canRead: Boolean,
    val canWrite: Boolean,
    val canNotify: Boolean
)

data class GattServiceInfo(
    val uuid: String,
    val characteristics: List<GattCharacteristicInfo>
)

data class GattNotification(
    val serviceUuid: String,
    val characteristicUuid: String,
    val data: ByteArray
)

class BleGattClient(context: Context) {

    private val appContext = context.applicationContext
    private var gatt: BluetoothGatt? = null
    private val pending = AtomicReference<((Result<ByteArray>) -> Unit)?>(null)
    private var connectContinuation: ((Result<List<GattServiceInfo>>) -> Unit)? = null

    private val _notifications = MutableSharedFlow<GattNotification>(extraBufferCapacity = 64)
    val notifications: SharedFlow<GattNotification> = _notifications.asSharedFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> g.discoverServices()
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectContinuation?.invoke(Result.failure(Exception("Desconectado")))
                    connectContinuation = null
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val cont = connectContinuation ?: return
            connectContinuation = null
            if (status != BluetoothGatt.GATT_SUCCESS) {
                cont(Result.failure(Exception("Discovery failed: $status")))
                return
            }
            cont(Result.success(mapServices(g)))
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            pending.getAndSet(null)?.invoke(
                if (status == BluetoothGatt.GATT_SUCCESS) Result.success(characteristic.value ?: byteArrayOf())
                else Result.failure(Exception("Read failed: $status"))
            )
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            pending.getAndSet(null)?.invoke(
                if (status == BluetoothGatt.GATT_SUCCESS) Result.success(byteArrayOf())
                else Result.failure(Exception("Write failed: $status"))
            )
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            pending.getAndSet(null)?.invoke(
                if (status == BluetoothGatt.GATT_SUCCESS) Result.success(byteArrayOf())
                else Result.failure(Exception("Descriptor write failed: $status"))
            )
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            emitNotification(gatt, characteristic, value)
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            characteristic.value?.let { emitNotification(gatt, characteristic, it) }
        }
    }

    private fun emitNotification(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        val serviceUuid = characteristic.service?.uuid?.toString() ?: return
        _notifications.tryEmit(
            GattNotification(
                serviceUuid = serviceUuid,
                characteristicUuid = characteristic.uuid.toString(),
                data = value
            )
        )
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(address: String): Result<List<GattServiceInfo>> = suspendCancellableCoroutine { cont ->
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            cont.resume(Result.failure(Exception("Bluetooth desactivado")))
            return@suspendCancellableCoroutine
        }
        gatt?.close()
        connectContinuation = { result -> if (cont.isActive) cont.resume(result) }
        gatt = adapter.getRemoteDevice(address).connectGatt(
            appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE
        )
        cont.invokeOnCancellation {
            connectContinuation = null
            gatt?.close()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun readCharacteristic(serviceUuid: String, charUuid: String): Result<ByteArray> =
        suspendCancellableCoroutine { cont ->
            val g = gatt ?: return@suspendCancellableCoroutine cont.resume(Result.failure(Exception("No conectado")))
            val ch = g.getService(UUID.fromString(serviceUuid))?.getCharacteristic(UUID.fromString(charUuid))
                ?: return@suspendCancellableCoroutine cont.resume(Result.failure(Exception("Característica no encontrada")))
            pending.set { result -> cont.resume(result) }
            g.readCharacteristic(ch)
        }

    @SuppressLint("MissingPermission")
    suspend fun writeCharacteristic(serviceUuid: String, charUuid: String, data: ByteArray): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val g = gatt ?: return@suspendCancellableCoroutine cont.resume(Result.failure(Exception("No conectado")))
            val ch = g.getService(UUID.fromString(serviceUuid))?.getCharacteristic(UUID.fromString(charUuid))
                ?: return@suspendCancellableCoroutine cont.resume(Result.failure(Exception("Característica no encontrada")))
            ch.value = data
            ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            pending.set { result -> cont.resume(result.map { }) }
            g.writeCharacteristic(ch)
        }

    @SuppressLint("MissingPermission")
    suspend fun subscribeCharacteristic(serviceUuid: String, charUuid: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val g = gatt ?: return@suspendCancellableCoroutine cont.resume(Result.failure(Exception("No conectado")))
            val ch = g.getService(UUID.fromString(serviceUuid))?.getCharacteristic(UUID.fromString(charUuid))
                ?: return@suspendCancellableCoroutine cont.resume(Result.failure(Exception("Característica no encontrada")))
            if (!g.setCharacteristicNotification(ch, true)) {
                cont.resume(Result.failure(Exception("No se pudo activar notify")))
                return@suspendCancellableCoroutine
            }
            val descriptor = ch.getDescriptor(CCCD)
            if (descriptor == null) {
                cont.resume(Result.success(Unit))
                return@suspendCancellableCoroutine
            }
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            pending.set { result -> cont.resume(result.map { }) }
            g.writeDescriptor(descriptor)
        }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        connectContinuation = null
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    private fun mapServices(g: BluetoothGatt): List<GattServiceInfo> {
        return g.services.map { svc ->
            GattServiceInfo(
                uuid = svc.uuid.toString(),
                characteristics = svc.characteristics.map { ch ->
                    val props = ch.properties
                    GattCharacteristicInfo(
                        uuid = ch.uuid.toString(),
                        properties = props,
                        canRead = props and BluetoothGattCharacteristic.PROPERTY_READ != 0,
                        canWrite = props and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0,
                        canNotify = props and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                    )
                }
            )
        }
    }

    companion object {
        private val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
