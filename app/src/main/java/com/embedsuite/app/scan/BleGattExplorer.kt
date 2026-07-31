package com.embedsuite.app.scan

import android.bluetooth.le.ScanResult

data class BleGattInfo(
    val name: String,
    val address: String,
    val rssi: Int,
    val serviceUuids: List<String>,
    val manufacturerData: String,
    val txPower: Int?,
    val isConnectable: Boolean
)

object BleGattExplorer {

    fun fromScanResult(result: ScanResult): BleGattInfo {
        val record = result.scanRecord
        val services = record?.serviceUuids?.map { it.uuid.toString() } ?: emptyList()
        val mfg = record?.manufacturerSpecificData?.let { sparse ->
            if (sparse.size() == 0) return@let ""
            val sb = StringBuilder()
            for (i in 0 until sparse.size()) {
                val id = sparse.keyAt(i)
                val data = sparse.valueAt(i)
                sb.append("0x${id.toString(16).uppercase()}:${data.joinToString("") { "%02X".format(it) }} ")
            }
            sb.toString().trim()
        } ?: ""

        return BleGattInfo(
            name = result.device.name ?: record?.deviceName ?: "Unknown",
            address = result.device.address,
            rssi = result.rssi,
            serviceUuids = services,
            manufacturerData = mfg,
            txPower = record?.txPowerLevel,
            isConnectable = record?.advertiseFlags?.let { it and 0x02 != 0 } ?: false
        )
    }
}
