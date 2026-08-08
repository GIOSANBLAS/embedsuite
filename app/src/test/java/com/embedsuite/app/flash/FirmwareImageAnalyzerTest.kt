package com.embedsuite.app.flash



import org.junit.Assert.assertEquals

import org.junit.Assert.assertTrue

import org.junit.Test

import java.io.File



class FirmwareImageAnalyzerTest {



    @Test

    fun analyze_bundledXibalbaBin_isMergedFull_at0() {

        val bin = File("src/main/assets/firmware/xibalba-t-embed-cc1101.bin")

        if (!bin.exists()) return



        val analysis = FirmwareImageAnalyzer.analyze(bin)

        assertEquals(FirmwareImageAnalyzer.ImageKind.MERGED_FULL, analysis.kind)

        assertEquals(FirmwareImageAnalyzer.MERGED_OFFSET, analysis.flashOffset)

        assertTrue(bin.length() > 1_000_000L)

    }

}

