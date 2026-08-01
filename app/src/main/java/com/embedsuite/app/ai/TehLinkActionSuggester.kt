package com.embedsuite.app.ai

import org.json.JSONObject

object TehLinkActionSuggester {

    private data class ActionPattern(
        val keywords: List<String>,
        val json: () -> String?,
        val response: String,
        val confidence: Float = 0.85f
    )

    private fun pingJson() = """{"cmd":"ping"}"""

    private fun captureJson(seconds: Int) = JSONObject().apply {
        put("cmd", "run_action")
        put("plugin_id", "subghz_analyzer")
        put("action", "capture_start")
        put("params", JSONObject().put("seconds", seconds))
    }.toString()

    private fun statusJson() = """{"cmd":"get_status"}"""

    private fun infoJson() = """{"cmd":"get_info"}"""

    private val patterns = listOf(
        ActionPattern(
            keywords = listOf("captur", "raw", "subghz", "sub-ghz", "rf", "433", "868", "915"),
            json = {
                val seconds = 15
                captureJson(seconds)
            },
            response = "Sugerencia TEH-Link: capture_start en subghz_analyzer."
        ),
        ActionPattern(
            keywords = listOf("ir", "infrarrojo", "control remoto", "capturar ir"),
            json = {
                JSONObject().apply {
                    put("cmd", "run_action")
                    put("plugin_id", "ir_toolkit")
                    put("action", "rx_start")
                    put("params", JSONObject().put("seconds", 10))
                }.toString()
            },
            response = "Sugerencia TEH-Link: ir_toolkit rx_start (10s)."
        ),
        ActionPattern(
            keywords = listOf("nfc", "tarjeta", "rfid", "leer nfc", "uid"),
            json = {
                JSONObject().apply {
                    put("cmd", "run_action")
                    put("plugin_id", "nfc_toolkit")
                    put("action", "read")
                }.toString()
            },
            response = "Sugerencia TEH-Link: nfc_toolkit read."
        ),
        ActionPattern(
            keywords = listOf("info", "estado", "diagnostico", "diagnóstico", "sistema"),
            json = { infoJson() },
            response = "Consultando get_info vía TEH-Link…"
        ),
        ActionPattern(
            keywords = listOf("uptime", "batería", "battery", "status"),
            json = { statusJson() },
            response = "Consultando get_status vía TEH-Link…"
        ),
        ActionPattern(
            keywords = listOf("wifi", "escane", "scan ap"),
            json = {
                JSONObject().apply {
                    put("cmd", "run_action")
                    put("plugin_id", "wifi_toolkit")
                    put("action", "scan_start")
                    put("params", JSONObject().put("seconds", 30))
                }.toString()
            },
            response = "Sugerencia TEH-Link: wifi_toolkit scan_start."
        )
    )

    fun parse(input: String): AiResponse {
        val normalized = input.lowercase().trim()

        if (normalized.startsWith("{")) {
            return AiResponse(
                message = "JSON TEH-Link detectado — envía desde consola o activa auto-ejecución.",
                suggestedCommand = input.trim(),
                actionType = AiActionType.EXECUTE_COMMAND,
                confidence = 1f
            )
        }

        for (pattern in patterns) {
            if (pattern.keywords.any { normalized.contains(it) }) {
                val json = pattern.json()
                return if (json != null) {
                    AiResponse(
                        message = pattern.response,
                        suggestedCommand = json,
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
                "EMBED AI — T-Embed Xibalba + TEH-Link.\n\n" +
                    "Ejemplos:\n" +
                    "• \"Captura RF 15 segundos\"\n" +
                    "• \"Leer NFC\"\n" +
                    "• \"Estado del sistema\"\n" +
                    "• JSON directo: {\"cmd\":\"ping\"}\n\n" +
                    "Auto-ejecución solo acepta JSON TEH-Link."

            input.contains("protocolo") || input.contains("que es") || input.contains("qué es") ->
                "Captura con TEH-Link y pregúntame sobre la señal en biblioteca RF."

            else ->
                "No identifiqué una acción TEH-Link. Prueba:\n" +
                    "• \"Captura subghz 15 segundos\"\n" +
                    "• \"Info del sistema\"\n" +
                    "• {\"cmd\":\"ping\"} en consola CLI"
        }
    }
}
