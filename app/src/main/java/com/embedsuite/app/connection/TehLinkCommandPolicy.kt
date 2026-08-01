package com.embedsuite.app.connection

import org.json.JSONObject

/**
 * Política de seguridad para JSON TEH-Link enviado desde la consola (entrada manual).
 * Los helpers del Dashboard usan TehLinkClient directamente y no pasan por aquí.
 */
object TehLinkCommandPolicy {

    private val BLOCKED_CMDS = setOf(
        "ota_begin",
        "ota_chunk",
        "ota_finish",
        "ota_abort"
    )

    private val BLOCKED_RUN_ACTIONS = mapOf(
        "badusb" to setOf("run_script", "stop"),
        "wardriving" to setOf("start"),
        "crypto_toolkit" to setOf("gen_password", "gen_passphrase")
    )

    fun validateConsoleRequest(json: String): Result<Unit> {
        val obj = runCatching { JSONObject(json.trim()) }.getOrElse {
            return Result.failure(IllegalArgumentException("TEH-Link: JSON inválido"))
        }
        val cmd = obj.optString("cmd")
        if (cmd in BLOCKED_CMDS) {
            return Result.failure(
                IllegalArgumentException("Comando TEH-Link '$cmd' bloqueado en consola")
            )
        }
        if (cmd == "run_action") {
            val pluginId = obj.optString("plugin_id").ifBlank {
                obj.optJSONObject("params")?.optString("plugin_id").orEmpty()
            }
            val action = obj.optString("action").ifBlank {
                obj.optJSONObject("params")?.optString("action").orEmpty()
            }
            val blocked = BLOCKED_RUN_ACTIONS[pluginId]
            if (blocked != null && action in blocked) {
                return Result.failure(
                    IllegalArgumentException(
                        "Acción TEH-Link '$pluginId/$action' bloqueada en consola — usa el Dashboard"
                    )
                )
            }
        }
        return Result.success(Unit)
    }
}
