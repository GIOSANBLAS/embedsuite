package com.embedsuite.app.core.device

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class DeviceProfileStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun list(): List<DeviceProfile> {
        val raw = prefs.getString(KEY_PROFILES_JSON, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    parseProfile(arr.getJSONObject(i))?.let { add(it) }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun getActive(): DeviceProfile? {
        val activeId = prefs.getString(KEY_ACTIVE_ID, null) ?: return null
        return list().firstOrNull { it.id == activeId }
    }

    fun setActive(id: String) {
        prefs.edit().putString(KEY_ACTIVE_ID, id).apply()
    }

    fun upsert(profile: DeviceProfile) {
        val current = list().associateBy { it.id }.toMutableMap()
        current[profile.id] = profile
        persist(current.values.toList())
        if (prefs.getString(KEY_ACTIVE_ID, null) == null) {
            setActive(profile.id)
        }
    }

    fun delete(id: String) {
        val remaining = list().filterNot { it.id == id }
        persist(remaining)
        if (prefs.getString(KEY_ACTIVE_ID, null) == id) {
            prefs.edit()
                .putString(KEY_ACTIVE_ID, remaining.firstOrNull()?.id)
                .apply()
        }
    }

    private fun persist(profiles: List<DeviceProfile>) {
        val arr = JSONArray()
        profiles.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY_PROFILES_JSON, arr.toString()).apply()
    }

    private fun toJson(profile: DeviceProfile): JSONObject = JSONObject().apply {
        put("id", profile.id)
        put("name", profile.name)
        put("hardwareKind", profile.hardwareKind.name)
        put("capabilities", JSONArray(profile.capabilities.map { it.name }))
        put("firmwareVersion", profile.firmwareVersion)
        put("productName", profile.productName)
        put("lastSeenMs", profile.lastSeenMs)
        put("notes", profile.notes)
    }

    private fun parseProfile(obj: JSONObject): DeviceProfile? = runCatching {
        val capsArr = obj.optJSONArray("capabilities") ?: JSONArray()
        val caps = buildSet {
            for (i in 0 until capsArr.length()) {
                val name = capsArr.optString(i)
                runCatching { add(DeviceCapability.valueOf(name)) }
            }
        }
        DeviceProfile(
            id = obj.getString("id"),
            name = obj.optString("name", obj.getString("id")),
            hardwareKind = DeviceHardwareKind.valueOf(
                obj.optString("hardwareKind", DeviceHardwareKind.UNKNOWN.name)
            ),
            capabilities = caps,
            firmwareVersion = obj.optString("firmwareVersion"),
            productName = obj.optString("productName"),
            lastSeenMs = obj.optLong("lastSeenMs"),
            notes = obj.optString("notes")
        )
    }.getOrNull()

    companion object {
        private const val PREFS_NAME = "embed_device_profiles"
        private const val KEY_PROFILES_JSON = "profiles_json"
        private const val KEY_ACTIVE_ID = "active_profile_id"
    }
}
