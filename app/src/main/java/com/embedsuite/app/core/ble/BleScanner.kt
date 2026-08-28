package com.embedsuite.app.core.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BleScanEntry(
    val device: BluetoothDevice,
    val name: String,
    val rssi: Int,
    val isTEmbedCandidate: Boolean
)

/** Escaneo BLE para T-Embed / Bruce / LilyGO. */
class BleScanner(context: Context) {

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private val _results = MutableStateFlow<List<BleScanEntry>>(emptyList())
    val results: StateFlow<List<BleScanEntry>> = _results.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val callback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val name = device.name ?: result.scanRecord?.deviceName ?: "?"
            val lower = name.lowercase()
            val candidate = lower.contains("t-embed") || lower.contains("bruce") ||
                lower.contains("lilygo") || lower.contains("esp32") || lower.contains("embed")
            val entry = BleScanEntry(device, name, result.rssi, candidate)
            _results.value = (_results.value.filter { it.device.address != device.address } + entry)
                .sortedByDescending { it.isTEmbedCandidate }
        }

        override fun onScanFailed(errorCode: Int) {
            _scanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val bt = adapter ?: return
        if (!bt.isEnabled) return
        _results.value = emptyList()
        _scanning.value = true
        bt.bluetoothLeScanner?.startScan(callback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        adapter?.bluetoothLeScanner?.stopScan(callback)
        _scanning.value = false
    }
}
