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
        return TehLinkDeviceInfo(
            product = data.optString("product"),
            version = data.optString("version"),
            codename = data.optString("codename"),
            channel = data.optString("channel"),
            proto = data.optString("proto"),
            protoVer = data.optInt("proto_ver", 1),
            plugins = plugins
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

    fun parseActionState(data: JSONObject): TehLinkActionState {
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
            secondsRemaining = data.optInt("seconds_remaining")
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
        return TehLinkDeviceStatus(
            sdMounted = data.optBoolean("sd_mounted"),
            flashMounted = data.optBoolean("flash_mounted"),
            uiScreen = data.optString("ui_screen"),
            uptimeMs = data.optLong("uptime_ms"),
            sim = sim
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
