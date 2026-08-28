package com.embedsuite.app.scan

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WirelessDevice(
    val name: String,
    val mac: String,
    val rssi: Int,
    val type: String,
    val detail: String,
    val gattDetail: String = ""
)

class WirelessScanner(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bleScanner = bluetoothManager?.adapter?.bluetoothLeScanner

    private val _devices = MutableStateFlow<List<WirelessDevice>>(emptyList())
    val devices: StateFlow<List<WirelessDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val bleResults = mutableMapOf<String, WirelessDevice>()

    private val bleCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val gatt = BleGattExplorer.fromScanResult(result)
            val services = gatt.serviceUuids.joinToString(", ") { it.take(8) }.ifBlank { "ADV" }
            val entry = WirelessDevice(
                name = gatt.name,
                mac = gatt.address,
                rssi = gatt.rssi,
                type = "BLE",
                detail = "$services // ${gatt.rssi}dBm",
                gattDetail = buildString {
                    appendLine("Services: ${gatt.serviceUuids.joinToString()}")
                    if (gatt.manufacturerData.isNotBlank()) appendLine("MFG: ${gatt.manufacturerData}")
                    gatt.txPower?.let { appendLine("TX Power: $it dBm") }
                    append("Connectable: ${gatt.isConnectable}")
                }
            )
            bleResults[device.address] = entry
            mergeResults()
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun scanWifi(): Result<List<WirelessDevice>> {
        return try {
            if (!wifiManager.isWifiEnabled) {
                wifiManager.isWifiEnabled = true
            }
            @Suppress("DEPRECATION")
            val scanSuccess = wifiManager.startScan()
            if (!scanSuccess) {
                return Result.failure(Exception("No se pudo iniciar escaneo WiFi."))
            }

            val wifiDevices = wifiManager.scanResults.map { result ->
                val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.wifiSsid?.toString()?.removeSurrounding("\"") ?: "Hidden"
                } else {
                    @Suppress("DEPRECATION")
                    result.SSID.ifBlank { "Hidden" }
                }
                val security = when {
                    result.capabilities.contains("WPA3") -> "WPA3"
                    result.capabilities.contains("WPA2") -> "WPA2"
                    result.capabilities.contains("WPA") -> "WPA"
                    result.capabilities.contains("WEP") -> "WEP"
                    else -> "OPEN"
                }
                WirelessDevice(
                    name = ssid,
                    mac = result.BSSID,
                    rssi = result.level,
                    type = "WIFI",
                    detail = "$security // CH ${result.frequency / 1000}"
                )
            }.sortedByDescending { it.rssi }

            mergeResults(wifiDevices)
            Result.success(wifiDevices)
        } catch (e: SecurityException) {
            Result.failure(Exception("Permiso de ubicación requerido para escanear WiFi."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    fun startBleScan() {
        val scanner = bleScanner ?: return
        if (_isScanning.value) return
        _isScanning.value = true
        bleResults.clear()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()

        scanner.startScan(null, settings, bleCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        bleScanner?.stopScan(bleCallback)
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun scanAll(): Result<List<WirelessDevice>> {
        startBleScan()
        return scanWifi()
    }

    private fun mergeResults(wifiDevices: List<WirelessDevice>? = null) {
        val wifi = wifiDevices ?: _devices.value.filter { it.type == "WIFI" }
        _devices.value = (wifi + bleResults.values).sortedByDescending { it.rssi }
    }
}
