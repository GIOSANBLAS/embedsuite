package com.embedsuite.app.connection

import org.junit.Assert.*
import org.junit.Test

class FirmwareRepositoryTest {

    @Test
    fun isNewer_detectsNewVersion() {
        assertTrue(FirmwareRepository.isNewer("1.8.0", "1.7.5"))
        assertFalse(FirmwareRepository.isNewer("1.7.0", "1.7.5"))
        assertFalse(FirmwareRepository.isNewer("1.7.5", "1.7.5"))
    }

    @Test
    fun pickRecommended_prefersXibalbaForXibalbaProfile() {
        val bruce = FirmwareRelease(
            tagName = "v1.8.0",
            name = "Bruce",
            downloadUrl = "https://example.com/bruce.bin",
            fileName = "bruce.bin",
            isPrerelease = false,
            source = FirmwareSource.OFFICIAL_BRUCE
        )
        val xibalba = FirmwareRelease(
            tagName = "v0.16.0",
            name = "Beacon",
            downloadUrl = "https://example.com/xibalba.bin",
            fileName = "te-embed-xibalba.bin",
            isPrerelease = true,
            source = FirmwareSource.OFFICIAL_XIBALBA
        )
        val recommended = FirmwareCatalog.pickRecommended(listOf(bruce, xibalba), FirmwareProfile.XIBALBA)
        assertEquals(FirmwareSource.OFFICIAL_XIBALBA, recommended?.source)
        assertEquals("v0.16.0", recommended?.tagName)
    }

    @Test
    fun pickRecommended_prefersXibalbaForAutoProfile() {
        val bruce = FirmwareRelease(
            tagName = "v1.8.0",
            name = "Bruce",
            downloadUrl = "https://example.com/bruce.bin",
            fileName = "bruce.bin",
            isPrerelease = false,
            source = FirmwareSource.OFFICIAL_BRUCE
        )
        val xibalba = FirmwareRelease(
            tagName = "v0.16.2",
            name = "Glow",
            downloadUrl = "https://example.com/xibalba.bin",
            fileName = "te-embed-xibalba.bin",
            isPrerelease = true,
            source = FirmwareSource.OFFICIAL_XIBALBA
        )
        val recommended = FirmwareCatalog.pickRecommended(listOf(bruce, xibalba), FirmwareProfile.AUTO)
        assertEquals(FirmwareSource.OFFICIAL_XIBALBA, recommended?.source)
    }

    @Test
    fun fromPref_defaultsToXibalba() {
        assertEquals(FirmwareProfile.XIBALBA, FirmwareProfile.fromPref(null))
        assertEquals(FirmwareProfile.XIBALBA, FirmwareProfile.fromPref("invalid"))
    }
}
