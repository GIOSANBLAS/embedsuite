package com.embedsuite.app.scripting

import org.json.JSONObject

enum class ScriptCategory(val label: String) {
    EVIL_PORTAL("Evil Portal"),
    BEACON_SPAM("Beacon Spam"),
    RECON("Recon"),
    RF("RF"),
    IR("IR"),
    NFC("NFC"),
    CRYPTO("Crypto"),
    BADUSB("BadUSB / HID"),
    WIFI("WiFi"),
    TEHLINK_JS("TEH-Link payloads"),
    BLE_SPAM("BLE Spam"),
    WIFI_OFFENSIVE("WiFi Ofensivo"),
    MOUSEJACK("Mousejack NRF24"),
    SUBGHZ_TOOLS("SubGHz Tools"),
    NFC_CLONE("NFC Clone/Write")
}

enum class ScriptDialect { TEHLINK_JSON, TEHLINK_JS, BADUSB_FINTEK, MACRO_SEQUENCE }

data class ScriptParameter(
    val key: String,
    val label: String,
    val type: String = "string",
    val default: String = "",
    val required: Boolean = false,
    val options: List<String> = emptyList()
)

data class Script(
    val id: String,
    val title: String,
    val summary: String,
    val category: ScriptCategory,
    val dialect: ScriptDialect = ScriptDialect.TEHLINK_JSON,
    val pluginId: String = "",
    val action: String = "",
    val defaultParams: Map<String, Any> = emptyMap(),
    val parameters: List<ScriptParameter> = emptyList(),
    val requiresAuditUnlock: Boolean = false,
    val icon: String = "code"
) {
    fun buildParams(overrides: Map<String, Any> = emptyMap()): JSONObject {
        val merged = defaultParams + overrides
        val o = JSONObject()
        merged.forEach { (k, v) -> o.put(k, v) }
        return o
    }
}
