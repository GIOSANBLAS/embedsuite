package com.embedsuite.app.engine.terminal

import org.json.JSONObject

data class TranslateResult(
    val json: String,
    val explanation: String
)

/**
 * Maps Spanish/English natural-language phrases to TEH-Link JSON commands.
 * If input already looks like JSON, returns it unchanged.
 */
object NaturalLanguageTranslator {

    val suggestionPhrases: List<String> = listOf(
        "ping",
        "get_info",
        "get_status",
        "list_actions",
        "escanea wifi",
        "scan wifi",
        "escanea ble",
        "captura subghz",
        "rx 15",
        "emparejar",
        "pair"
    )

    fun translate(input: String): TranslateResult? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null
        if (looksLikeJson(trimmed)) {
            return TranslateResult(trimmed, "Entrada JSON detectada — sin traducción.")
        }

        val lower = trimmed.lowercase()

        pingMatch(lower)?.let { return it }
        getInfoMatch(lower)?.let { return it }
        getStatusMatch(lower)?.let { return it }
        listActionsMatch(lower)?.let { return it }
        wifiScanMatch(lower)?.let { return it }
        bleScanMatch(lower)?.let { return it }
        subGhzCaptureMatch(lower, trimmed)?.let { return it }
        pairMatch(lower)?.let { return it }

        return null
    }

    private fun looksLikeJson(text: String): Boolean {
        if (!text.startsWith("{") && !text.startsWith("[")) return false
        return runCatching { JSONObject(text) }.isSuccess || text.contains("\"cmd\"")
    }

    private fun pingMatch(lower: String): TranslateResult? {
        if (lower == "ping" || lower == "prueba" || lower == "test") {
            return cmd("ping", 1, "Ping TEH-Link.")
        }
        return null
    }

    private fun getInfoMatch(lower: String): TranslateResult? {
        if (lower.contains("get_info") || lower.contains("get info") ||
            lower.contains("info del dispositivo") || lower == "info"
        ) {
            return cmd("get_info", 2, "Solicitar get_info del firmware.")
        }
        return null
    }

    private fun getStatusMatch(lower: String): TranslateResult? {
        if (lower.contains("get_status") || lower.contains("get status") ||
            lower.contains("estado") || lower == "status"
        ) {
            return cmd("get_status", 3, "Solicitar get_status (uptime, heap, batería).")
        }
        return null
    }

    private fun listActionsMatch(lower: String): TranslateResult? {
        if (lower.contains("list_actions") || lower.contains("list actions") ||
            lower.contains("acciones") || lower.contains("plugins")
        ) {
            return cmd("list_actions", 5, "Listar acciones TEH-Link disponibles.")
        }
        return null
    }

    private fun wifiScanMatch(lower: String): TranslateResult? {
        if (lower.contains("escanea wifi") || lower.contains("scan wifi") ||
            lower.contains("wifi scan") || lower.contains("escanear wifi")
        ) {
            val seconds = extractSeconds(lower) ?: 10
            return runAction("wifi_toolkit", "scan_start", seconds, "Escaneo WiFi ${seconds}s.")
        }
        return null
    }

    private fun bleScanMatch(lower: String): TranslateResult? {
        if (lower.contains("escanea ble") || lower.contains("scan ble") ||
            lower.contains("ble scan") || lower.contains("bluetooth")
        ) {
            val seconds = extractSeconds(lower) ?: 10
            return runAction("ble_toolkit", "scan_start", seconds, "Escaneo BLE ${seconds}s.")
        }
        return null
    }

    private fun subGhzCaptureMatch(lower: String, original: String): TranslateResult? {
        val rxMatch = Regex("""rx\s+(\d+)""", RegexOption.IGNORE_CASE).find(original)
        val seconds = rxMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: extractSeconds(lower)
            ?: if (lower.contains("15")) 15 else null

        if (lower.contains("captura subghz") || lower.contains("subghz capture") ||
            lower.contains("sub-ghz") || rxMatch != null || seconds != null
        ) {
            val sec = (seconds ?: 15).coerceIn(1, 120)
            return runAction("subghz_analyzer", "capture_start", sec, "Captura Sub-GHz ${sec}s.")
        }
        return null
    }

    private fun pairMatch(lower: String): TranslateResult? {
        if (lower.contains("emparejar") || lower == "pair" || lower.contains("pairing")) {
            return TranslateResult("""{"cmd":"pair","id":99}""", "Iniciar emparejamiento TEH-Link.")
        }
        return null
    }

    private fun cmd(cmd: String, id: Int, explanation: String) =
        TranslateResult("""{"cmd":"$cmd","id":$id}""", explanation)

    private fun runAction(
        pluginId: String,
        action: String,
        seconds: Int,
        explanation: String
    ): TranslateResult {
        val root = JSONObject()
            .put("cmd", "run_action")
            .put("id", 10)
            .put("plugin_id", pluginId)
            .put("action", action)
            .put("params", JSONObject().put("seconds", seconds))
        return TranslateResult(root.toString(), explanation)
    }

    private fun extractSeconds(lower: String): Int? {
        val match = Regex("""(\d+)\s*(s|seg|sec|seconds?)""").find(lower)
            ?: Regex("""(\d+)""").find(lower)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()
    }
}
