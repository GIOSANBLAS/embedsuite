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

sealed class DuckyBlock {
    abstract fun compile(): String

    data class Comment(val text: String) : DuckyBlock() {
        override fun compile() = "REM $text"
    }

    data class Delay(val ms: Int) : DuckyBlock() {
        override fun compile() = "DELAY ${ms.coerceAtLeast(0)}"
    }

    data class StringText(val text: String) : DuckyBlock() {
        override fun compile() = "STRING $text"
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

        fun parse(script: String): List<DuckyBlock> =
            script.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { line ->
                    when {
                        line.startsWith("REM", ignoreCase = true) ->
                            Comment(line.removePrefix("REM").trim())
                        line.startsWith("DELAY", ignoreCase = true) ->
                            Delay(line.substringAfter(' ').trim().toIntOrNull() ?: 0)
                        line.startsWith("STRING", ignoreCase = true) ->
                            StringText(line.removePrefix("STRING").trim())
                        else -> {
                            val tok = line.substringBefore(' ').uppercase()
                            Key.entries.find { it.token == tok }?.let { KeyPress(it) }
                                ?: StringText(line)
                        }
                    }
                }
                .toList()
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
