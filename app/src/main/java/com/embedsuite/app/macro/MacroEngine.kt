package com.embedsuite.app.macro

import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.data.MacroEntity
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

class MacroEngine(
    private val connectionManager: DeviceConnectionManager,
    private val sessionStats: com.embedsuite.app.core.SessionStatsTracker? = null
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
            connectionManager.sendTehLinkRaw(cmd).fold(
                onSuccess = { executed++ },
                onFailure = { return Result.failure(Exception("Falló en '$cmd': ${it.message}")) }
            )
            delay(300)
        }
        return Result.success(executed).also { sessionStats?.incrementMacros() }
    }
}
