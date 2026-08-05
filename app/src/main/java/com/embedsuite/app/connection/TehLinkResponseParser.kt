package com.embedsuite.app.connection

import org.json.JSONArray
import org.json.JSONObject

object TehLinkResponseParser {

    fun parseDeviceInfo(data: JSONObject): TehLinkDeviceInfo {
        val plugins = mutableListOf<TehLinkPluginInfo>()
        val arr: JSONArray? = data.optJSONArray("plugins")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                plugins += TehLinkPluginInfo(
                    id = item.optString("id"),
                    name = item.optString("name"),
                    version = item.optString("version"),
                    author = item.optString("author")
                )
            }
        }
        val hardening = parseHardeningInfo(data.optJSONObject("hardening"))
        return TehLinkDeviceInfo(
            product = data.optString("product"),
            version = data.optString("version"),
            codename = data.optString("codename"),
            channel = data.optString("channel"),
            proto = data.optString("proto"),
            protoVer = data.optInt("proto_ver", 1),
            plugins = plugins,
            hardening = hardening
        )
    }

    fun parseHardeningInfo(data: JSONObject?): TehLinkHardeningInfo {
        if (data == null) return TehLinkHardeningInfo()
        return TehLinkHardeningInfo(
            twdtEnabled = data.optBoolean("twdt_enabled"),
            twdtTimeoutSeconds = data.optInt("twdt_timeout_s", 0),
            bodEnabled = data.optBoolean("bod_enabled"),
            bodVoltage = data.optDouble("bod_v_mv").let { mv ->
                if (data.has("bod_v_mv") && mv > 0) (mv / 1000.0).toFloat() else null
            },
            secureBoot = data.optBoolean("secure_boot"),
            flashEncryption = data.optBoolean("flash_encryption"),
            nvsEncryption = data.optBoolean("nvs_encryption"),
            stackCanaries = data.optBoolean("stack_canaries"),
            heapPoisoning = data.optBoolean("heap_poisoning")
        )
    }

    fun parseScreenInfo(data: JSONObject): TehLinkScreenInfo {
        return TehLinkScreenInfo(
            uiScreen = data.optString("ui_screen"),
            activePlugin = data.optString("active_plugin"),
            openedPluginId = data.optString("plugin_id")
        )
    }

    fun parseActionList(data: JSONObject): List<TehLinkActionInfo> {
        val actions = mutableListOf<TehLinkActionInfo>()
        val arr: JSONArray? = data.optJSONArray("actions")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val params = mutableListOf<String>()
                item.optJSONArray("params")?.let { paramArr ->
                    for (j in 0 until paramArr.length()) {
                        paramArr.optString(j).takeIf { it.isNotBlank() }?.let { params += it }
                    }
                }
                actions += TehLinkActionInfo(
                    pluginId = item.optString("plugin_id"),
                    action = item.optString("action"),
                    params = params
                )
            }
        }
        return actions
    }

    fun parseWifiAp(item: JSONObject): TehLinkWifiAp {
        return TehLinkWifiAp(
            ssid = item.optString("ssid"),
            bssid = item.optString("bssid"),
            channel = item.optInt("channel"),
            rssi = item.optInt("rssi"),
            security = item.optString("security")
        )
    }

    fun parseWifiAps(arr: JSONArray?): List<TehLinkWifiAp> {
        if (arr == null) return emptyList()
        val aps = mutableListOf<TehLinkWifiAp>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { aps += parseWifiAp(it) }
        }
        return aps
    }

    fun parseBleDevice(item: JSONObject): TehLinkBleDevice {
        return TehLinkBleDevice(
            name = item.optString("name"),
            address = item.optString("address"),
            rssi = item.optInt("rssi"),
            isTracker = item.optBoolean("is_tracker")
        )
    }

    fun parseBleDevices(arr: JSONArray?): List<TehLinkBleDevice> {
        if (arr == null) return emptyList()
        val devices = mutableListOf<TehLinkBleDevice>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { devices += parseBleDevice(it) }
        }
        return devices
    }

    fun parseWardrivingStatus(data: JSONObject): TehLinkWardrivingStatus {
        return TehLinkWardrivingStatus(
            running = data.optBoolean("running"),
            apCount = data.optInt("ap_count"),
            csvPath = data.optString("csv_path"),
            csvBasename = data.optString("csv_basename")
        )
    }

    fun parseNfcResult(data: JSONObject): TehLinkNfcResult {
        return TehLinkNfcResult(
            uid = data.optString("uid"),
            sak = data.optInt("sak"),
            ready = data.optBoolean("ready")
        )
    }

    fun parseIrResult(data: JSONObject): TehLinkIrResult {
        return TehLinkIrResult(
            ready = data.optBoolean("ready"),
            message = data.optString("message"),
            raw = data.optString("raw"),
            protocol = data.optString("protocol")
        )
    }

    fun parseCryptoResult(data: JSONObject): TehLinkCryptoResult {
        return TehLinkCryptoResult(
            digest = data.optString("digest"),
            result = data.optString("result").ifBlank { data.optString("last_result") },
            algo = data.optString("algo")
        )
    }

    fun parseOtaStatus(data: JSONObject): TehLinkOtaStatus {
        return TehLinkOtaStatus(
            state = data.optString("state", "idle"),
            bytesWritten = data.optLong("bytes_written", 0L),
            totalSize = data.optLong("total_size", 0L),
            sha256Verified = data.optBoolean("sha256_verified")
        )
    }

    fun parseSoakResult(data: JSONObject): TehLinkSoakResult {
        return TehLinkSoakResult(
            iterations = data.optInt("iterations"),
            failures = data.optInt("failures"),
            heapFreeBefore = data.optLong("heap_before"),
            heapFreeAfter = data.optLong("heap_after"),
            leakBytes = data.optLong("leak_bytes"),
            completed = data.optBoolean("completed")
        )
    }

    fun parseActionState(data: JSONObject): TehLinkActionState {
        val wardriving = if (data.has("ap_count") || data.has("csv_path")) {
            parseWardrivingStatus(data)
        } else {
            null
        }
        val crypto = if (data.has("digest") || data.has("result") || data.has("last_result") || data.has("algo")) {
            parseCryptoResult(data)
        } else {
            null
        }
        val nfc = if (data.has("uid") || data.optString("plugin_id") == "nfc_toolkit") {
            parseNfcResult(data)
        } else {
            null
        }
        val ir = if (data.optString("plugin_id") == "ir_toolkit") {
            parseIrResult(data)
        } else {
            null
        }
        val ota = if (data.has("sha256_verified") || data.has("state") && data.optString("plugin_id", "") == "ota") {
            parseOtaStatus(data)
        } else {
            null
        }
        val soak = if (data.has("iterations") && data.has("leak_bytes")) {
            parseSoakResult(data)
        } else {
            null
        }
        return TehLinkActionState(
            pluginId = data.optString("plugin_id"),
            action = data.optString("action"),
            state = data.optString("state"),
            progress = data.optInt("progress"),
            message = data.optString("message"),
            loadedPath = data.optString("loaded_path"),
            running = data.optBoolean("running"),
            capturing = data.optBoolean("capturing"),
            packets = data.optInt("packets"),
            secondsRemaining = data.optInt("seconds_remaining"),
            aps = parseWifiAps(data.optJSONArray("aps")),
            devices = parseBleDevices(data.optJSONArray("devices")),
            wardriving = wardriving,
            crypto = crypto,
            nfc = nfc,
            ir = ir,
            ota = ota,
            soak = soak
        )
    }

    fun parseActionResult(data: JSONObject): TehLinkActionResult {
        return TehLinkActionResult(
            pluginId = data.optString("plugin_id"),
            action = data.optString("action"),
            state = parseActionState(data)
        )
    }

    fun parseDeviceStatus(data: JSONObject): TehLinkDeviceStatus {
        val sim = mutableMapOf<String, Boolean>()
        data.optJSONObject("sim")?.let { obj ->
            obj.keys().forEach { key ->
                sim[key] = obj.optBoolean(key)
            }
        }
        val capabilities = mutableMapOf<String, Boolean>()
        data.optJSONObject("capabilities")?.let { obj ->
            obj.keys().forEach { key ->
                capabilities[key] = obj.optBoolean(key)
            }
        }
        return TehLinkDeviceStatus(
            sdMounted = data.optBoolean("sd_mounted"),
            flashMounted = data.optBoolean("flash_mounted"),
            uiScreen = data.optString("ui_screen"),
            uptimeMs = data.optLong("uptime_ms"),
            sim = sim,
            capabilities = capabilities,
            batteryPct = data.optInt("battery_pct").takeIf { data.has("battery_pct") },
            chargeStatus = data.optString("charge_status").takeIf { data.has("charge_status") },
            charging = data.optBoolean("charging").takeIf { data.has("charging") },
            vbusPresent = data.optBoolean("vbus_present").takeIf { data.has("vbus_present") },
            heapFreeBytes = data.optLong("heap_free_bytes").takeIf { data.has("heap_free_bytes") },
            psramFreeBytes = data.optLong("psram_free_bytes").takeIf { data.has("psram_free_bytes") },
            coredumpPresent = data.optBoolean("coredump_present"),
            wdtPanicReason = data.optString("panic_reason").takeIf { data.has("panic_reason") && !data.optString("panic_reason").isNullOrBlank() }
        )
    }

    fun isTehLinkLine(line: String): Boolean {
        val trimmed = line.trim()
        if (!trimmed.startsWith("{")) return false
        return runCatching {
            val obj = JSONObject(trimmed)
            obj.has("ok") && (obj.has("data") || obj.has("error"))
        }.getOrDefault(false)
    }

    /** Redacta campos sensibles en respuestas TEH-Link antes de log/UI. */
    fun redactSensitiveResponse(line: String): String {
        if (!isTehLinkLine(line)) return line
        return runCatching {
            val root = JSONObject(line.trim())
            val data = root.optJSONObject("data") ?: return line
            redactSensitiveFields(data)
            root.put("data", data)
            root.toString()
        }.getOrDefault(line)
    }

    /** Redacta campos sensibles en peticiones TEH-Link antes de log. */
    fun redactSensitiveRequest(json: String): String {
        return runCatching {
            val root = JSONObject(json.trim())
            redactSensitiveFields(root)
            val params = root.optJSONObject("params")
            if (params != null) {
                redactSensitiveFields(params)
                root.put("params", params)
            }
            root.toString()
        }.getOrDefault(json)
    }

    private fun redactSensitiveFields(obj: JSONObject) {
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            when (val value = obj.opt(key)) {
                is JSONObject -> redactSensitiveFields(value)
                is org.json.JSONArray -> {
                    for (i in 0 until value.length()) {
                        (value.opt(i) as? JSONObject)?.let { redactSensitiveFields(it) }
                    }
                }
                else -> {
                    if (key in SENSITIVE_KEYS && obj.optString(key).isNotBlank()) {
                        obj.put(key, "[REDACTED]")
                    }
                }
            }
        }
    }

    private val SENSITIVE_KEYS = setOf(
        "result", "password", "passphrase", "digest", "last_result", "input", "data", "token", "auth"
    )

    /** Valida petición TEH-Link cruda (cmd + id obligatorios). */
    fun validateRawRequest(json: String): Result<Int> {
        val trimmed = json.trim()
        if (!trimmed.startsWith("{")) {
            return Result.failure(IllegalArgumentException("TEH-Link: se esperaba JSON"))
        }
        val obj = runCatching { JSONObject(trimmed) }.getOrElse {
            return Result.failure(IllegalArgumentException("TEH-Link: JSON inválido"))
        }
        if (obj.optString("cmd").isBlank()) {
            return Result.failure(IllegalArgumentException("TEH-Link requiere campo cmd"))
        }
        if (!obj.has("id")) {
            return Result.failure(IllegalArgumentException("TEH-Link requiere campo id"))
        }
        return Result.success(obj.optInt("id"))
    }
}
