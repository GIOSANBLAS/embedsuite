package com.embedsuite.app.core.orchestrator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BadUsbIntentTest {

    @Test
    fun duckyBlockCompile_producesValidScript() {
        val blocks = listOf(
            DuckyBlock.Comment("test"),
            DuckyBlock.Delay(500),
            DuckyBlock.StringText("hello"),
            DuckyBlock.KeyPress(Key.ENTER)
        )
        val script = DuckyBlock.compile(blocks)
        assertTrue(script.contains("REM test"))
        assertTrue(script.contains("DELAY 500"))
        assertTrue(script.contains("STRING hello"))
        assertTrue(script.contains("ENTER"))
    }

    @Test
    fun duckyBlockCompile_escapesQuotesAndSpecialChars() {
        val script = DuckyBlock.compile(listOf(DuckyBlock.StringText("say \"hi\" \\ test")))
        assertEquals("STRING \"say \\\"hi\\\" \\\\ test\"", script)
        val roundTrip = DuckyBlock.parse(script).filterIsInstance<DuckyBlock.StringText>().single()
        assertEquals("say \"hi\" \\ test", roundTrip.text)
    }

    @Test
    fun duckyBlockCompile_multilineString_splitsLines() {
        val script = DuckyBlock.compile(listOf(DuckyBlock.StringText("line1\nline2")))
        assertTrue(script.contains("STRING line1"))
        assertTrue(script.contains("STRING line2"))
    }

    @Test
    fun duckyBlockParse_comboAndRepeat() {
        val script = """
            GUI R
            REPEAT 2
            STRING a
            END_REPEAT
        """.trimIndent()
        val blocks = DuckyBlock.parse(script)
        assertTrue(blocks[0] is DuckyBlock.Combo)
        assertTrue(blocks[1] is DuckyBlock.Repeat)
    }

    @Test
    fun badUsbTemplate_notepad_hasBlocks() {
        assertEquals(4, BadUsbTemplates.all.size)
        assertTrue(BadUsbTemplates.notepad.blocks.isNotEmpty())
    }

    @Test
    fun capturedSignal_trimSilence_keepsPulses() {
        val sub = com.embedsuite.app.engine.decoder.FlipperSubFile(
            rawTimings = listOf(100, 500, 200)
        )
        val sig = CapturedSignal(sub)
        assertEquals(3, sig.trimSilence(50).pulses.size)
    }
}
