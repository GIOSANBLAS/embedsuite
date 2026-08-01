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
}
