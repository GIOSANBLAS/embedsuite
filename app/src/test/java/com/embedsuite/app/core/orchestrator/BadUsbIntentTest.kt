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
    fun badUsbTemplate_notepad_hasBlocks() {
        assertEquals(4, BadUsbTemplates.all.size)
        assertTrue(BadUsbTemplates.notepad.blocks.isNotEmpty())
    }

    @Test
    fun capturedSignal_trimSilence_keepsPulses() {
        val sig = CapturedSignal("Filetype: RAW", 433.92, listOf(100L, 500L, 200L))
        assertEquals(3, sig.trimSilence(50).pulses.size)
    }
}
