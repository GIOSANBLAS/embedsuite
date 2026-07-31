package com.embedsuite.app.ai

import com.embedsuite.app.connection.BruceCommands

object BruceCommandGenerator {

    private data class CommandPattern(
        val keywords: List<String>,
        val command: (MatchResult?) -> String?,
        val response: String,
        val confidence: Float = 0.85f
    )

    private val patterns = listOf(
        CommandPattern(
            keywords = listOf("captur", "raw", "subghz", "sub-ghz", "rf", "433", "868", "915"),
            command = { match ->
                val seconds = Regex("""(\d+)\s*(?:seg|sec|s)""").find(match?.value ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 15
                BruceCommands.subGhzRxRaw(seconds)
            },
            response = "Iniciando captura RAW Sub-GHz (comando Bruce documentado)."
        ),
        CommandPattern(
            keywords = listOf("detener", "stop", "parar"),
            command = { _ -> null },
            response = "Bruce no tiene stop CLI: la RX termina sola al acabar los segundos. Espera o lanza otra captura corta."
        ),
        CommandPattern(
            keywords = listOf("ir", "infrarrojo", "control remoto", "capturar ir"),
            command = { match ->
                val seconds = Regex("""(\d+)""").find(match?.value ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 10
                BruceCommands.irRxRaw(seconds)
            },
            response = "Modo captura IR activo. Apunta un control remoto al T-Embed."
        ),
        CommandPattern(
            keywords = listOf("nfc", "tarjeta", "rfid", "leer nfc", "uid"),
            command = { _ -> null },
            response = BruceCommands.NFC_CLI_UNSUPPORTED
        ),
        CommandPattern(
            keywords = listOf("info", "estado", "diagnostico", "diagnóstico", "sistema"),
            command = { _ -> BruceCommands.info() },
            response = "Consultando estado del ESP32-S3..."
        ),
        CommandPattern(
            keywords = listOf("memoria", "ram", "heap", "free"),
            command = { _ -> BruceCommands.free() },
            response = "Consultando memoria libre del T-Embed..."
        ),
        CommandPattern(
            keywords = listOf("uptime", "tiempo activo", "encendido"),
            command = { _ -> BruceCommands.uptime() },
            response = "Consultando uptime del dispositivo..."
        ),
        CommandPattern(
            keywords = listOf("webui", "web ui", "activar wifi", "punto de acceso"),
            command = { _ -> BruceCommands.webui() },
            response = "Activando WebUI de Bruce. Conéctate a BruceNet."
        ),
        CommandPattern(
            keywords = listOf("i2c", "bus i2c", "escanear i2c"),
            command = { _ -> BruceCommands.i2cScan() },
            response = "Escaneando bus I2C en busca de módulos..."
        ),
        CommandPattern(
            keywords = listOf("settings", "configuracion", "configuración", "ajustes bruce"),
            command = { _ -> BruceCommands.settings() },
            response = "Mostrando configuración actual de Bruce."
        ),
        CommandPattern(
            keywords = listOf("storage", "archivos", "listar archivos"),
            command = { _ -> BruceCommands.storageList("/") },
            response = "Listando archivos en SD/LittleFS del T-Embed."
        ),
        CommandPattern(
            keywords = listOf("transmit", "transmitir", "tx", "enviar señal"),
            command = { _ -> null },
            response = "Para TX usa biblioteca (señal decodificada) o Tools → Sync SD → TX en un .sub del dispositivo."
        ),
        CommandPattern(
            keywords = listOf("escane", "scan", "ble", "bluetooth"),
            command = { _ -> null },
            response = "Escaneo BLE/WiFi: usa la pestaña WiFi del teléfono (no hay `ble scan` en Serial Bruce)."
        )
    )

    fun parse(input: String): AiResponse {
        val normalized = input.lowercase().trim()

        if (normalized.startsWith("/") || normalized.startsWith(">")) {
            val cmd = normalized.removePrefix("/").removePrefix(">").trim()
            return AiResponse(
                message = "Ejecutando comando directo: $cmd",
                suggestedCommand = cmd,
                actionType = AiActionType.EXECUTE_COMMAND,
                confidence = 1f
            )
        }

        for (pattern in patterns) {
            if (pattern.keywords.any { normalized.contains(it) }) {
                val match = Regex(""".*(\d+).*""").find(normalized)
                val cmd = pattern.command(match)
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
                "EMBED AI online. Comandos Bruce documentados para T-Embed CC1101.\n\n" +
                    "Ejemplos:\n" +
                    "• \"Captura RF 15 segundos\"\n" +
                    "• \"Capturar señal IR\"\n" +
                    "• \"Estado del sistema\"\n" +
                    "• \"/info\" (comando directo)\n\n" +
                    "NFC: menú del T-Embed (no hay CLI Serial oficial).\n" +
                    "TX RAW: Sync SD → TX, o señal decodificada en biblioteca."

            input.contains("protocolo") || input.contains("que es") || input.contains("qué es") ->
                "Captura con \"captura raw 15s\" y pregúntame sobre la señal. " +
                    "También puedo analizar el log de señales guardadas."

            else ->
                "No identifiqué un comando Bruce documentado. Prueba:\n" +
                    "• \"Captura subghz raw 10 segundos\"\n" +
                    "• \"Info del sistema\"\n" +
                    "• \"Listar archivos\"\n" +
                    "O escribe /comando para envío directo."
        }
    }
}
