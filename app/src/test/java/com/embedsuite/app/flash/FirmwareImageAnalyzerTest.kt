package com.embedsuite.app.flash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FirmwareImageAnalyzerTest {

    @Test
    fun analyze_bundledXibalbaBin_isMergedFull_at0() {
        val primary = File("src/main/assets/firmware/xibalba-t-embed-cc1101.bin")
        val legacy = File("src/main/assets/firmware/te-embed-xibalba.bin")
        val bin = when {
            primary.exists() -> primary
            legacy.exists() -> legacy
            else -> return
        }

        val analysis = FirmwareImageAnalyzer.analyze(bin)
        // Official Xibalba-0.19 (Bruce PlatformIO) ships as merged image @ 0x0
        if (bin.name.contains("xibalba-t-embed", ignoreCase = true) || bin.length() > 3_000_000L) {
            assertEquals(FirmwareImageAnalyzer.ImageKind.MERGED_FULL, analysis.kind)
            assertEquals(FirmwareImageAnalyzer.MERGED_OFFSET, analysis.flashOffset)
            assertTrue(bin.length() > 1_000_000L)
        }
    }
}
