package com.embedsuite.app.macro

import com.embedsuite.app.connection.BruceCommandValidator
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.data.MacroEntity
import kotlinx.coroutines.delay

class MacroEngine(
    private val connectionManager: DeviceConnectionManager,
    private val sessionStats: com.embedsuite.app.core.SessionStatsTracker? = null
) {

    suspend fun execute(macro: MacroEntity): Result<Int> {
        val commands = macro.commands.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }

        if (commands.isEmpty()) {
            return Result.failure(Exception("Macro vacío."))
        }

        var executed = 0
        for (cmd in commands) {
            if (cmd.startsWith("wait ", ignoreCase = true)) {
                val ms = cmd.substringAfter(" ").trim().removeSuffix("ms").toLongOrNull() ?: 1000L
                delay(ms)
                continue
            }
            BruceCommandValidator.validate(cmd).getOrElse {
                return Result.failure(Exception("Macro inválido en '$cmd': ${it.message}"))
            }
            connectionManager.sendCommand(cmd).fold(
                onSuccess = { executed++ },
                onFailure = { return Result.failure(Exception("Falló en '$cmd': ${it.message}")) }
            )
            delay(300)
        }
        return Result.success(executed).also { sessionStats?.incrementMacros() }
    }
}
