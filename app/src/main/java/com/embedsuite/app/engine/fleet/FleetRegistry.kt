package com.embedsuite.app.engine.fleet

import com.embedsuite.app.core.device.DeviceProfile
import com.embedsuite.app.core.device.DeviceProfileStore
import org.json.JSONArray
import org.json.JSONObject

/**
 * Fleet inventory over persisted [DeviceProfileStore] entries.
 */
class FleetRegistry(
    private val store: DeviceProfileStore
) {
    fun listDevices(): List<FleetDevice> =
        store.list().map { FleetDevice.fromProfile(it) }

    fun setActive(id: String) {
        store.setActive(id)
    }

    fun getActive(): FleetDevice? =
        store.getActive()?.let { FleetDevice.fromProfile(it) }

    fun setNickname(id: String, nickname: String) {
        val profile = store.list().firstOrNull { it.id == id } ?: return
        store.upsert(profile.copy(name = nickname.trim().ifBlank { profile.name }))
    }

    fun exportInventoryJson(): String {
        val arr = JSONArray()
        listDevices().forEach { arr.put(it.toJson()) }
        return JSONObject()
            .put("schema", "embedsuite-fleet/v1")
            .put("active_id", store.getActive()?.id)
            .put("devices", arr)
            .toString(2)
    }

    data class FleetDevice(
        val id: String,
        val nickname: String,
        val hardwareKind: String,
        val firmwareVersion: String,
        val productName: String,
        val capabilities: List<String>,
        val lastSeenMs: Long,
        val notes: String
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("id", id)
            put("nickname", nickname)
            put("hardwareKind", hardwareKind)
            put("firmwareVersion", firmwareVersion)
            put("productName", productName)
            put("capabilities", JSONArray(capabilities))
            put("lastSeenMs", lastSeenMs)
            put("notes", notes)
        }

        companion object {
            fun fromProfile(profile: DeviceProfile): FleetDevice = FleetDevice(
                id = profile.id,
                nickname = profile.name,
                hardwareKind = profile.hardwareKind.name,
                firmwareVersion = profile.firmwareVersion,
                productName = profile.productName,
                capabilities = profile.capabilities.map { it.name },
                lastSeenMs = profile.lastSeenMs,
                notes = profile.notes
            )
        }
    }
}
