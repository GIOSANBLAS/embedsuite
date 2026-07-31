package com.embedsuite.app.connection

/**
 * Valida comandos Bruce antes de enviarlos al T-Embed.
 * Mitiga inyección de líneas múltiples, payloads enormes y órdenes peligrosas.
 */
object BruceCommandValidator {

    const val MAX_LENGTH = 512

    private val BLOCKED_PATTERNS = Regex(
        """(?i)(^|\s)(""" +
            """reboot|""" +
            """format|""" +
            """factory\s*reset|""" +
            """erase\s*flash|""" +
            """wifi\s*deauth|""" +
            """storage\s+rm(\s|$)|""" +
            """storage\s+format|""" +
            """rm\s+-rf|""" +
            """rm\s+-r(\s|$)|""" +
            """dd\s+if=|""" +
            """mkfs""" +
            """)"""
    )

    fun validate(command: String): Result<String> {
        val trimmed = command.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Comando vacío"))
        }
        if (trimmed.length > MAX_LENGTH) {
            return Result.failure(IllegalArgumentException("Comando demasiado largo (máx. $MAX_LENGTH caracteres)"))
        }
        if (trimmed.contains('\n') || trimmed.contains('\r')) {
            return Result.failure(IllegalArgumentException("Solo un comando por línea"))
        }
        if (BLOCKED_PATTERNS.containsMatchIn(trimmed)) {
            return Result.failure(IllegalArgumentException("Comando bloqueado por seguridad"))
        }
        return Result.success(trimmed)
    }
}
