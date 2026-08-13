package com.embedsuite.app.connection

import org.json.JSONObject

/**
 * Política de seguridad para JSON TEH-Link enviado desde la consola (entrada manual).
 * Los helpers del Dashboard usan TehLinkClient directamente y no pasan por aquí.
 */
object TehLinkCommandPolicy {

    /** Comandos de solo lectura / navegación permitidos desde la consola manual. */
    private val ALLOWED_CONSOLE_CMDS = setOf(
        "ping",
        "get_info",
        "get_status",
        "get_screen",
        "list_actions",
        "get_action_state",
        "back_to_menu",
        "list_files",
        "sd.list",
        "sd.status"
    )

    fun validateConsoleRequest(json: String): Result<Unit> {
        val obj = runCatching { JSONObject(json.trim()) }.getOrElse {
            return Result.failure(IllegalArgumentException("TEH-Link: JSON inválido"))
        }
        val cmd = obj.optString("cmd")
        if (cmd !in ALLOWED_CONSOLE_CMDS) {
            return Result.failure(
                IllegalArgumentException(
                    "Comando TEH-Link '$cmd' no permitido en consola — usa el Dashboard"
                )
            )
        }
        return Result.success(Unit)
    }
}
