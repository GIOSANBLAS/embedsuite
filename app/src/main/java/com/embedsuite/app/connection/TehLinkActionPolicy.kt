package com.embedsuite.app.connection

/**
 * Política central para acciones TEH-Link (Dashboard y futuros callers).
 */
object TehLinkActionPolicy {

    private val BLOCKED_ACTIONS = mapOf(
        "badusb" to setOf("run_script"),
        "wardriving" to setOf("start")
    )

    fun validate(pluginId: String, action: String): Result<Unit> {
        val blocked = BLOCKED_ACTIONS[pluginId]
        if (blocked != null && action in blocked) {
            return Result.failure(
                IllegalArgumentException(
                    "Acción '$pluginId/$action' requiere confirmación en dispositivo — no disponible desde la app"
                )
            )
        }
        return Result.success(Unit)
    }
}
