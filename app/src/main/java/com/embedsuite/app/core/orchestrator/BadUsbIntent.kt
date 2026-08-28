package com.embedsuite.app.core.orchestrator

import android.content.Context
import com.embedsuite.app.core.connection.TransportTask
import com.embedsuite.app.engine.payload.DuckyEditor
import java.io.File

/** MÓDULO A — DuckyScript → WiFi upload → badusb run_from_file */
data class BadUsbIntent(
    val blocks: List<DuckyBlock>,
    val remoteFileName: String = "embed_payload.txt"
) : FileRunIntent {
    override val label = "BadUSB"
    override val uploadTask = TransportTask.FILE_UPLOAD
    override val triggerTask = TransportTask.BADUSB_RUN
    override val remotePath: String =
        "/badusb/${remoteFileName.trim().ifBlank { "embed_payload.txt" }}"

    override suspend fun prepare(context: Context): Result<PrepareResult> = runCatching {
        val script = DuckyBlock.compile(blocks)
        val issues = DuckyEditor.validate(script)
        require(issues.isEmpty()) { "DuckyScript inválido: ${issues.first().message}" }
        val f = File.createTempFile("embed_badusb_", ".txt", context.cacheDir)
        f.writeText(script)
        PrepareResult(file = f, metadata = mapOf("script" to script))
    }

    override fun triggerCommand(remotePath: String): String =
        DuckyEditor.badUsbRunFromFileCommand(remotePath)

    companion object {
        fun fromScript(script: String, remoteFileName: String = "embed_payload.txt"): BadUsbIntent =
            BadUsbIntent(DuckyBlock.parse(script), remoteFileName)

        fun fromTemplate(template: BadUsbTemplates.Template): BadUsbIntent =
            BadUsbIntent(template.blocks, template.fileName)
    }
}

/** Escapes STRING payloads for round-trip safe DuckyScript compilation. */
object DuckyStringEscaper {

    private val NEEDS_QUOTE = Regex("""[\u0000-\u001F"\\]|^\s|\s$""")

    fun escape(text: String): String {
        if (text.isEmpty()) return "\"\""
        if (!NEEDS_QUOTE.containsMatchIn(text)) return text
        return buildString(text.length + 4) {
            append('"')
            text.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }
    }

    fun unescape(payload: String): String {
        val trimmed = payload.trim()
        if (!trimmed.startsWith('"')) return trimmed
        val sb = StringBuilder()
        var i = 1
        while (i < trimmed.length) {
            val ch = trimmed[i]
            if (ch == '\\' && i + 1 < trimmed.length) {
                when (trimmed[i + 1]) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    '"', '\\' -> sb.append(trimmed[i + 1])
                    else -> sb.append(trimmed[i + 1])
                }
                i += 2
            } else if (ch == '"') {
                break
            } else {
                sb.append(ch)
                i++
            }
        }
        return sb.toString()
    }
}

sealed class DuckyBlock {
    abstract fun compile(): String

    data class Comment(val text: String) : DuckyBlock() {
        override fun compile() = "REM $text"
    }

    data class Delay(val ms: Int) : DuckyBlock() {
        override fun compile() = "DELAY ${ms.coerceAtLeast(0)}"
    }

    data class StringText(val text: String) : DuckyBlock() {
        override fun compile(): String {
            if (text.contains('\n')) {
                return text.split('\n').joinToString("\n") { line ->
                    if (line.isEmpty()) "ENTER" else "STRING ${DuckyStringEscaper.escape(line)}"
                }
            }
            return "STRING ${DuckyStringEscaper.escape(text)}"
        }
    }

    data class KeyPress(val key: Key) : DuckyBlock() {
        override fun compile() = key.token
    }

    data class Combo(val keys: List<Key>) : DuckyBlock() {
        override fun compile() = keys.joinToString(" ") { it.token }
    }

    data class Repeat(val count: Int, val inner: List<DuckyBlock>) : DuckyBlock() {
        override fun compile(): String {
            val body = compile(inner)
            return "REPEAT ${count.coerceAtLeast(1)}\n$body\nEND_REPEAT"
        }
    }

    companion object {
        fun compile(blocks: List<DuckyBlock>): String =
            blocks.joinToString("\n") { it.compile() }

        fun parse(script: String): List<DuckyBlock> {
            val lines = script.lineSequence().map { it.trimEnd() }.toList()
            val blocks = mutableListOf<DuckyBlock>()
            var i = 0
            while (i < lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) {
                    i++
                    continue
                }
                when {
                    line.startsWith("REM", ignoreCase = true) -> {
                        blocks += Comment(line.substring(3).trimStart())
                        i++
                    }
                    line.startsWith("DELAY", ignoreCase = true) -> {
                        blocks += Delay(line.substringAfter(' ', "").trim().toIntOrNull() ?: 0)
                        i++
                    }
                    line.startsWith("STRING", ignoreCase = true) -> {
                        val payload = line.substring(6).trimStart()
                        blocks += StringText(DuckyStringEscaper.unescape(payload))
                        i++
                    }
                    line.startsWith("REPEAT", ignoreCase = true) -> {
                        val count = line.substringAfter(' ', "1").trim().toIntOrNull() ?: 1
                        val inner = mutableListOf<DuckyBlock>()
                        i++
                        while (i < lines.size) {
                            val innerLine = lines[i].trim()
                            if (innerLine.equals("END_REPEAT", ignoreCase = true)) {
                                i++
                                break
                            }
                            if (innerLine.isNotEmpty()) {
                                parse(innerLine).forEach { inner += it }
                            }
                            i++
                        }
                        blocks += Repeat(count, inner)
                    }
                    else -> {
                        val tokens = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
                        val keys = tokens.mapNotNull { tok ->
                            Key.entries.find { it.token.equals(tok, ignoreCase = true) }
                        }
                        when {
                            keys.size == tokens.size && keys.size > 1 -> blocks += Combo(keys)
                            keys.size == 1 && tokens.size == 1 -> blocks += KeyPress(keys.first())
                            else -> blocks += StringText(line)
                        }
                        i++
                    }
                }
            }
            return blocks
        }
    }
}

enum class Key(val token: String) {
    ENTER("ENTER"), ESC("ESC"), TAB("TAB"), SPACE("SPACE"),
    WIN("GUI"), ALT("ALT"), CTRL("CTRL"), SHIFT("SHIFT"),
    KEY_R("R"), KEY_T("T"),
    UP("UP"), DOWN("DOWN"), LEFT("LEFT"), RIGHT("RIGHT"),
    DELETE("DELETE"), BACKSPACE("BACKSPACE"), INSERT("INSERT"),
    HOME("HOME"), END("END"), PAGEUP("PAGEUP"), PAGEDOWN("PAGEDOWN"),
    F1("F1"), F2("F2"), F3("F3"), F4("F4"), F5("F5"), F6("F6"),
    F7("F7"), F8("F8"), F9("F9"), F10("F10"), F11("F11"), F12("F12"),
    CAPSLOCK("CAPSLOCK"), NUMLOCK("NUMLOCK"), SCROLLLOCK("SCROLLLOCK"),
    PRINTSCREEN("PRINTSCREEN"), PAUSE("PAUSE")
}

object BadUsbTemplates {
    data class Template(val name: String, val fileName: String, val blocks: List<DuckyBlock>)

    val notepad = Template(
        "Notepad",
        "notepad_demo.txt",
        listOf(
            DuckyBlock.Comment("EmbedSuite — abrir Notepad"),
            DuckyBlock.Combo(listOf(Key.WIN, Key.KEY_R)),
            DuckyBlock.Delay(500),
            DuckyBlock.StringText("notepad"),
            DuckyBlock.KeyPress(Key.ENTER),
            DuckyBlock.Delay(800),
            DuckyBlock.StringText("Hello from Bruce BadUSB")
        )
    )

    val terminal = Template(
        "Terminal",
        "terminal_demo.txt",
        listOf(
            DuckyBlock.Comment("Abrir terminal"),
            DuckyBlock.Combo(listOf(Key.CTRL, Key.ALT, Key.KEY_T)),
            DuckyBlock.Delay(1000),
            DuckyBlock.StringText("echo EmbedSuite BadUSB OK"),
            DuckyBlock.KeyPress(Key.ENTER)
        )
    )

    val powershell = Template(
        "PowerShell",
        "ps_demo.txt",
        listOf(
            DuckyBlock.Combo(listOf(Key.WIN, Key.KEY_R)),
            DuckyBlock.Delay(400),
            DuckyBlock.StringText("powershell"),
            DuckyBlock.KeyPress(Key.ENTER),
            DuckyBlock.Delay(900),
            DuckyBlock.StringText("Write-Host 'EmbedSuite'")
        )
    )

    val reverseShellHint = Template(
        "Reverse Shell (plantilla)",
        "revshell_template.txt",
        listOf(
            DuckyBlock.Comment("Solo plantilla — edita IP/puerto antes de usar"),
            DuckyBlock.Combo(listOf(Key.WIN, Key.KEY_R)),
            DuckyBlock.Delay(400),
            DuckyBlock.StringText("cmd"),
            DuckyBlock.KeyPress(Key.ENTER),
            DuckyBlock.Delay(600),
            DuckyBlock.StringText("REM Reemplaza HOST:PORT")
        )
    )

    val all = listOf(notepad, terminal, powershell, reverseShellHint)
}
