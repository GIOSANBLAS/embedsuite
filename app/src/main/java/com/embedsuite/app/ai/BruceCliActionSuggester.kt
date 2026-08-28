package com.embedsuite.app.ai

object BruceCliActionSuggester {

    private data class ActionPattern(
        val keywords: List<String>,
        val command: () -> String?,
        val response: String,
        val confidence: Float = 0.85f
    )

    private val patterns = listOf(
        ActionPattern(
            keywords = listOf("captur", "raw", "subghz", "sub-ghz", "rf", "433", "868", "915"),
            command = { "subghz rx" },
            response = "Sugerencia: captura Sub-GHz con «subghz rx» (detén con subghz tx stop si aplica)."
        ),
        ActionPattern(
            keywords = listOf("ir", "infrarrojo", "control remoto", "capturar ir"),
            command = { "ir rx" },
            response = "Sugerencia: captura IR con «ir rx»."
        ),
        ActionPattern(
            keywords = listOf("nfc", "tarjeta", "rfid", "leer nfc", "uid"),
            command = { "nfc read" },
            response = "Sugerencia: lee NFC con «nfc read»."
        ),
        ActionPattern(
            keywords = listOf("info", "estado", "diagnostico", "diagnóstico", "sistema"),
            command = { "info" },
            response = "Consultando «info» vía Bruce CLI…"
        ),
        ActionPattern(
            keywords = listOf("uptime", "batería", "battery", "status"),
            command = { "info" },
            response = "Consultando «info» vía Bruce CLI…"
        ),
        ActionPattern(
            keywords = listOf("wifi", "escane", "scan ap"),
            command = { "wifi scan" },
            response = "Sugerencia: escaneo WiFi con «wifi scan»."
        ),
        ActionPattern(
            keywords = listOf("ping", "conexion", "conexión", "link"),
            command = { "info" },
            response = "Verificando enlace con «info»."
        )
    )

    fun parse(input: String): AiResponse {
        val normalized = input.lowercase().trim()

        if (normalized.startsWith("{")) {
            return AiResponse(
                message = "Los comandos JSON ya no se usan. Escribe líneas Bruce CLI (info, subghz rx, ir rx…).",
                actionType = AiActionType.CHAT_ONLY,
                confidence = 1f
            )
        }

        for (pattern in patterns) {
            if (pattern.keywords.any { normalized.contains(it) }) {
                val cmd = pattern.command()
                return if (cmd != null) {
                    AiResponse(
                        message = pattern.response,
                        suggestedCommand = cmd,
                        actionType = AiActionType.EXECUTE_COMMAND,
                        confidence = pattern.confidence
                    )
                } else {
                    AiResponse(
                        message = pattern.response,
                        actionType = AiActionType.CHAT_ONLY,
                        confidence = pattern.confidence
                    )
                }
            }
        }

        return AiResponse(
            message = buildHelpMessage(normalized),
            actionType = AiActionType.CHAT_ONLY,
            confidence = 0.5f
        )
    }

    private fun buildHelpMessage(input: String): String {
        return when {
            input.contains("hola") || input.contains("help") || input.contains("ayuda") ->
                "EMBED AI — T-Embed Bruce.\n\n" +
                    "Ejemplos:\n" +
                    "• \"Captura RF\"\n" +
                    "• \"Leer NFC\"\n" +
                    "• \"Estado del sistema\"\n\n" +
                    "Auto-ejecución envía líneas Bruce CLI (info, subghz rx, …)."

            input.contains("protocolo") || input.contains("que es") || input.contains("qué es") ->
                "Captura señales desde Tools y pregúntame sobre la última en biblioteca RF."

            else ->
                "No identifiqué una acción. Prueba:\n" +
                    "• \"Captura subghz\"\n" +
                    "• \"Info del sistema\"\n" +
                    "• Consola CLI en modo desarrollador"
        }
    }
}
