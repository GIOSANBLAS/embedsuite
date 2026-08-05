package com.embedsuite.app.macro

import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.core.SessionStatsTracker
import com.embedsuite.app.data.MacroEntity
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class MacroEngine(
    private val connectionManager: DeviceConnectionManager,
    private val sessionStats: SessionStatsTracker? = null
) {

    suspend fun execute(macro: MacroEntity): Result<Int> {
        return try {
            withTimeout(120_000L) {
                executeCommands(macro)
            }
        } catch (_: TimeoutCancellationException) {
            Result.failure(Exception("Macro cancelado: timeout 120s."))
        }
    }

    private suspend fun executeCommands(macro: MacroEntity): Result<Int> {
        val commands = macro.commands.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }

        if (commands.isEmpty()) {
            return Result.failure(Exception("Macro vacío."))
        }

        var executed = 0
        for (cmd in commands) {
            if (cmd.startsWith("wait ", ignoreCase = true)) {
                val ms = (cmd.substringAfter(" ").trim().removeSuffix("ms").toLongOrNull() ?: 1000L)
                    .coerceIn(0L, 60_000L)
                delay(ms)
                continue
            }
            if (!cmd.startsWith("{")) {
                return Result.failure(
                    Exception("Macros solo soportan pasos JSON TEH-Link o wait Nms. Línea inválida: $cmd")
                )
            }
            dispatchTehLinkLine(cmd).fold(
                onSuccess = { executed++ },
                onFailure = { return Result.failure(Exception("Falló en '$cmd': ${it.message}")) }
            )
            delay(300)
        }
        return Result.success(executed).also { sessionStats?.incrementMacros() }
    }

    /**
     * run_action pasa por Dashboard path (TehLinkActionPolicy);
     * el resto usa consola (TehLinkCommandPolicy: solo lectura).
     */
    private suspend fun dispatchTehLinkLine(cmd: String): Result<String> {
        val obj = runCatching { JSONObject(cmd) }.getOrElse {
            return Result.failure(IllegalArgumentException("JSON inválido"))
        }
        if (obj.optString("cmd") == "run_action") {
            val pluginId = obj.optString("plugin_id")
            val action = obj.optString("action")
            val params = obj.optJSONObject("params") ?: JSONObject()
            if (pluginId.isBlank() || action.isBlank()) {
                return Result.failure(IllegalArgumentException("run_action sin plugin_id/action"))
            }
            return connectionManager.tehLinkRunAction(pluginId, action, params).map {
                it.state.message.ifBlank { "$pluginId/$action OK" }
            }
        }
        return connectionManager.sendTehLinkRaw(cmd)
    }
}
