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
            if (pluginId == "badusb" && action == "run_script") {
                return Result.failure(
                    IllegalArgumentException("BadUSB run_script bloqueado en consola — usa el Dashboard")
                )
            }
        }
        return Result.success(Unit)
    }
}
