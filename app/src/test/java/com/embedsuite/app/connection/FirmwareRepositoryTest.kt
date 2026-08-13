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
    fun pickRecommended_prefersStableV0201() {
        val v0190 = FirmwareRelease(
            tagName = "v0.19.0",
            name = "Maya",
            downloadUrl = "https://example.com/xibalba-0190.bin",
            fileName = "xibalba-t-embed-cc1101.bin",
            isPrerelease = false,
            source = FirmwareSource.OFFICIAL_XIBALBA
        )
        val v0201 = FirmwareRelease(
            tagName = "v0.20.1",
            name = "Maya",
            downloadUrl = "https://example.com/xibalba-0201.bin",
            fileName = "xibalba-t-embed-cc1101.bin",
            isPrerelease = false,
            source = FirmwareSource.OFFICIAL_XIBALBA
        )
        val recommended = FirmwareCatalog.pickRecommended(listOf(v0190, v0201), FirmwareProfile.XIBALBA)
        assertEquals(FirmwareSource.OFFICIAL_XIBALBA, recommended?.source)
        assertEquals("v0.20.1", recommended?.tagName)
    }

    @Test
    fun fallbackReleases_recommendsV0201() {
        val list = FirmwareCatalog.fallbackReleases()
        assertTrue(list.size >= 2)
        assertTrue(list.any { it.tagName == "v0.20.1" })
        val recommended = list.first { it.isRecommended }
        assertEquals("v0.20.1", recommended.tagName)
        assertEquals(FirmwareCatalog.XIBALBA_V0201.downloadUrl, recommended.downloadUrl)
    }

    @Test
    fun embeddedReleases_containsCurrentAndLegacy() {
        val embedded = FirmwareCatalog.embeddedReleases()
        assertTrue(embedded.size >= 2)
        assertEquals(FirmwareCatalog.XIBALBA_V0201.tagName, embedded.first().tagName)
        assertTrue(embedded.any { it.tagName == "v0.19.0" })
    }

    @Test
    fun mergeWithEmbedded_preservesSha256FromEmbedded() {
        val remote = listOf(
            FirmwareRelease(
                tagName = "v0.19.0",
                name = "from github",
                downloadUrl = "https://github.com/example/xibalba-t-embed-cc1101.bin",
                fileName = "xibalba-t-embed-cc1101.bin",
                isPrerelease = false,
                source = FirmwareSource.OFFICIAL_XIBALBA,
                sha256Hex = null
            )
        )
        val merged = FirmwareCatalog.mergeWithEmbedded(remote)
        val v019 = merged.first { it.tagName == "v0.19.0" }
        assertEquals(FirmwareCatalog.XIBALBA_V0190.sha256Hex, v019.sha256Hex)
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
