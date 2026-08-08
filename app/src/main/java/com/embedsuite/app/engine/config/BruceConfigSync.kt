package com.embedsuite.app.engine.config

import android.content.Context
import android.content.SharedPreferences
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.core.error.HumanErrorMapper
import org.json.JSONObject
import java.io.File

/**
 * Synchronises Bruce-style JSON config with the connected T-Embed.
 * Prefers TEH-Link device plugin actions; falls back to local shadow copy.
 */
class BruceConfigSync(
    context: Context,
    private val connectionManager: DeviceConnectionManager
) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun pullFromDevice(): Result<String> {
        val viaAction = runDeviceAction("export_bruce_json")
            .recoverCatching { runDeviceAction("get_config").getOrThrow() }
        viaAction.onSuccess { json ->
            saveShadow(json)
        }
        return viaAction.map { it }.recoverCatching {
            val shadow = getLocalShadow()
            if (shadow.isNotBlank()) {
                shadow
            } else {
                throw Exception(HumanErrorMapper.map(it))
            }
        }
    }

    suspend fun pushToDevice(json: String): Result<Unit> {
        if (json.isBlank()) {
            return Result.failure(IllegalArgumentException("JSON de configuración vacío."))
        }
        saveShadow(json)
        val viaSet = runDeviceAction("import_bruce_json", mapOf("json" to json))
            .recoverCatching {
                runDeviceAction("set_config", mapOf("json" to json)).getOrThrow()
            }
        return viaSet.map { saveShadow(json) }
            .recoverCatching {
                saveShadow(json)
                throw Exception(
                    HumanErrorMapper.map(it) +
                        " Configuración guardada localmente como copia sombra."
                )
            }
    }

    fun backupToFile(context: Context): Result<File> = runCatching {
        val json = getLocalShadow()
        if (json.isBlank()) {
            throw IllegalStateException("No hay configuración local para respaldar.")
        }
        val dir = File(context.applicationContext.filesDir, "bruce_backups").also { it.mkdirs() }
        val file = File(dir, "bruce_${System.currentTimeMillis()}.json")
        file.writeText(json)
        file
    }

    fun restoreFromString(json: String): Result<Unit> = runCatching {
        if (json.isBlank()) throw IllegalArgumentException("JSON vacío.")
        saveShadow(json)
    }

    fun getLocalShadow(): String = prefs.getString(KEY_SHADOW, "").orEmpty()

    private fun saveShadow(json: String) {
        prefs.edit().putString(KEY_SHADOW, json).apply()
    }

    private suspend fun runDeviceAction(
        action: String,
        params: Map<String, String> = emptyMap()
    ): Result<String> {
        val jsonParams = JSONObject()
        params.forEach { (k, v) -> jsonParams.put(k, v) }

        val pluginResult = connectionManager.tehLinkRunAction("device", action, jsonParams)
        if (pluginResult.isSuccess) {
            val payload = pluginResult.getOrNull()?.rawResponse?.optString("json")
                ?: pluginResult.getOrNull()?.state?.message
                ?: pluginResult.getOrNull()?.state?.loadedPath
            if (!payload.isNullOrBlank()) {
                return Result.success(payload)
            }
        }

        val raw = JSONObject()
            .put("cmd", "run_action")
            .put("plugin_id", "device")
            .put("action", action)
            .apply {
                if (params.isNotEmpty()) {
                    put("params", jsonParams)
                }
            }
        return connectionManager.sendTehLinkRaw(raw.toString()).map { response ->
            runCatching { JSONObject(response).optString("json", response) }.getOrDefault(response)
        }
    }

    companion object {
        private const val PREFS_NAME = "bruce_config_sync"
        private const val KEY_SHADOW = "bruce_json_shadow"
    }
}
