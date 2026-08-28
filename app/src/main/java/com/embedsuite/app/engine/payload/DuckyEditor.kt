package com.embedsuite.app.engine.payload

import com.embedsuite.app.core.bruce.BruceLimits

/** Editor/validador DuckyScript — ejecución remota vía CLI Bruce `badusb`. */
object DuckyEditor {

    val KEYWORDS = setOf(
        "REM", "DELAY", "STRING", "ENTER", "GUI", "ALT", "CTRL", "SHIFT",
        "TAB", "ESC", "SPACE", "UP", "DOWN", "LEFT", "RIGHT", "DEFAULT_DELAY"
    )

    data class ValidationIssue(val line: Int, val message: String)

    fun validate(script: String): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        script.lineSequence().forEachIndexed { idx, raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("REM")) return@forEachIndexed
            val cmd = line.substringBefore(' ').uppercase()
            if (cmd !in KEYWORDS && !cmd.matches(Regex("[A-Z_]+"))) {
                issues += ValidationIssue(idx + 1, "Comando desconocido: $cmd")
            }
        }
        return issues
    }

    /** Comando Bruce: archivo .txt en SD del T-Embed. */
    fun badUsbRunFromFileCommand(devicePath: String): String {
        val path = devicePath.trim().let { if (it.startsWith("/")) it else "/$it" }
        require(path.endsWith(".txt", ignoreCase = true)) { "Bruce badusb requiere extensión .txt" }
        return "badusb run_from_file $path"
    }

    val remoteExecutionHint: String = BruceLimits.BADUSB_HINT

    @Deprecated("TEH-Link eliminado", ReplaceWith("badUsbRunFromFileCommand(devicePath)"))
    fun toTehLinkExecuteDucky(script: String, scriptName: String = "payload.txt"): String =
        badUsbRunFromFileCommand("/badusb/$scriptName")

    fun highlightTokens(line: String): List<Pair<String, TokenKind>> {
        if (line.trimStart().startsWith("REM")) return listOf(line to TokenKind.COMMENT)
        val parts = mutableListOf<Pair<String, TokenKind>>()
        val tokens = line.split(' ')
        tokens.forEachIndexed { i, tok ->
            if (i > 0) parts += " " to TokenKind.PLAIN
            val kind = when {
                tok.uppercase() in KEYWORDS -> TokenKind.KEYWORD
                tok.startsWith('"') -> TokenKind.STRING
                else -> TokenKind.PLAIN
            }
            parts += tok to kind
        }
        return parts.ifEmpty { listOf(line to TokenKind.PLAIN) }
    }

    enum class TokenKind { KEYWORD, STRING, COMMENT, PLAIN }
}
