package com.embedsuite.app.connection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DebugDirection { IN, OUT }

enum class DebugCategory {
    ALL, RF, SYSTEM, ERROR, STORAGE, OTHER
}

data class DebugLine(
    val timestamp: Long,
    val direction: DebugDirection,
    val text: String,
    val category: DebugCategory
)

object LinkDebugLog {

    private const val MAX_LINES = 200

    private val _lines = MutableStateFlow<List<DebugLine>>(emptyList())
    val lines: StateFlow<List<DebugLine>> = _lines.asStateFlow()

    fun appendIncoming(text: String) = append(DebugDirection.IN, text)

    fun appendOutgoing(text: String) = append(DebugDirection.OUT, text)

    fun clear() {
        _lines.value = emptyList()
    }

    fun asPlainText(filter: DebugCategory = DebugCategory.ALL): String {
        return filtered(filter).joinToString("\n") { line ->
            val prefix = if (line.direction == DebugDirection.OUT) ">" else " "
            "$prefix ${line.text}"
        }
    }

    fun filtered(category: DebugCategory): List<DebugLine> {
        if (category == DebugCategory.ALL) return _lines.value
        return _lines.value.filter { it.category == category }
    }

    private fun append(direction: DebugDirection, text: String) {
        val trimmed = sanitizeForLog(text.trim())
        if (trimmed.isBlank()) return
        val entry = DebugLine(
            timestamp = System.currentTimeMillis(),
            direction = direction,
            text = trimmed,
            category = categorize(trimmed)
        )
        _lines.value = (_lines.value + entry).takeLast(MAX_LINES)
    }

    private fun categorize(text: String): DebugCategory = when {
        text.contains("error", ignoreCase = true) ||
            text.contains("fail", ignoreCase = true) ||
            text.startsWith("[ERROR]") -> DebugCategory.ERROR
        text.contains("subghz", ignoreCase = true) ||
            text.contains("433", ignoreCase = true) ||
            text.contains("868", ignoreCase = true) ||
            text.contains("915", ignoreCase = true) ||
            text.contains("MHz", ignoreCase = true) ||
            text.startsWith("[RF]") -> DebugCategory.RF
        text.contains("storage", ignoreCase = true) ||
            text.endsWith(".sub", ignoreCase = true) ||
            text.endsWith(".ir", ignoreCase = true) ||
            text.endsWith(".nfc", ignoreCase = true) -> DebugCategory.STORAGE
        text.contains("uptime", ignoreCase = true) ||
            text.contains("heap", ignoreCase = true) ||
            text.contains("battery", ignoreCase = true) ||
            text.contains("firmware", ignoreCase = true) ||
            text.startsWith("[SYS]") -> DebugCategory.SYSTEM
        else -> DebugCategory.OTHER
    }

    fun sanitize(text: String): String = sanitizeForLog(text)

    private fun sanitizeForLog(text: String): String {
        if (text.contains("RAW", ignoreCase = true) && text.length > 96) {
            return text.take(72) + "… [RAW ${text.length} chars redacted]"
        }
        if (Regex("""[0-9A-Fa-f]{12,}""").containsMatchIn(text)) {
            return text.replace(Regex("""[0-9A-Fa-f]{8,}"""), "[HEX_REDACTED]")
        }
        return text
    }
}
