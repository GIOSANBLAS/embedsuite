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
        val xibalbaOld = FirmwareRelease(
            tagName = "v0.16.0",
            name = "Beacon",
            downloadUrl = "https://example.com/xibalba-old.bin",
            fileName = "te-embed-xibalba.bin",
            isPrerelease = true,
            source = FirmwareSource.OFFICIAL_XIBALBA
        )
        val xibalbaNew = FirmwareRelease(
            tagName = "v0.17.0",
            name = "Glow",
            downloadUrl = "https://example.com/xibalba.bin",
            fileName = "te-embed-xibalba.bin",
            isPrerelease = true,
            source = FirmwareSource.OFFICIAL_XIBALBA
        )
        val recommended = FirmwareCatalog.pickRecommended(listOf(xibalbaOld, xibalbaNew), FirmwareProfile.XIBALBA)
        assertEquals(FirmwareSource.OFFICIAL_XIBALBA, recommended?.source)
        assertEquals("v0.17.0", recommended?.tagName)
    }

    @Test
    fun pickRecommended_xibalbaOnlyList() {
        val xibalba = FirmwareRelease(
            tagName = "v0.17.0",
            name = "Glow",
            downloadUrl = "https://example.com/xibalba.bin",
            fileName = "te-embed-xibalba.bin",
            isPrerelease = true,
            source = FirmwareSource.OFFICIAL_XIBALBA
        )
        val recommended = FirmwareCatalog.pickRecommended(listOf(xibalba), FirmwareProfile.XIBALBA)
        assertEquals(FirmwareSource.OFFICIAL_XIBALBA, recommended?.source)
        assertEquals("v0.17.0", recommended?.tagName)
    }

    @Test
    fun deviceCatalog_isXibalbaOnly() {
        val xibalba = FirmwareRelease(
            tagName = "v0.17.0",
            name = "Glow",
            downloadUrl = "https://example.com/xibalba.bin",
            fileName = "te-embed-xibalba.bin",
            isPrerelease = true,
            source = FirmwareSource.OFFICIAL_XIBALBA
        )
        val list = listOf(xibalba)
        assertEquals(1, list.size)
        assertEquals(FirmwareSource.OFFICIAL_XIBALBA, list.first().source)
    }

    @Test
    fun settingsDisplayOrder_isXibalbaOnly() {
        assertEquals(listOf(FirmwareProfile.XIBALBA), FirmwareProfile.settingsDisplayOrder)
    }

    @Test
    fun fromPref_defaultsToXibalba() {
        assertEquals(FirmwareProfile.XIBALBA, FirmwareProfile.fromPref(null))
        assertEquals(FirmwareProfile.XIBALBA, FirmwareProfile.fromPref("invalid"))
        assertEquals(FirmwareProfile.XIBALBA, FirmwareProfile.fromPref("BRUCE"))
    }
}
