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
    fun pickRecommended_prefersStableV0190() {
        val v017 = FirmwareRelease(
            tagName = "v0.17.1",
            name = "Spark",
            downloadUrl = "https://example.com/xibalba-171.bin",
            fileName = "te-embed-xibalba.bin",
            isPrerelease = true,
            source = FirmwareSource.OFFICIAL_XIBALBA
        )
        val v018 = FirmwareRelease(
            tagName = "v0.18.0",
            name = "Iron Shield",
            downloadUrl = "https://example.com/xibalba-180.bin",
            fileName = "te-embed-xibalba.bin",
            isPrerelease = true,
            source = FirmwareSource.OFFICIAL_XIBALBA
        )
        val v019 = FirmwareRelease(
            tagName = "v0.19.0",
            name = "Maya",
            downloadUrl = "https://example.com/xibalba-t-embed-cc1101.bin",
            fileName = "xibalba-t-embed-cc1101.bin",
            isPrerelease = false,
            source = FirmwareSource.OFFICIAL_XIBALBA
        )
        val recommended = FirmwareCatalog.pickRecommended(listOf(v017, v018, v019), FirmwareProfile.XIBALBA)
        assertEquals(FirmwareSource.OFFICIAL_XIBALBA, recommended?.source)
        assertEquals("v0.19.0", recommended?.tagName)
    }

    @Test
    fun fallbackReleases_recommendsV0190WithBundledAsset() {
        val list = FirmwareCatalog.fallbackReleases()
        assertTrue(list.any { it.tagName == "v0.19.0" })
        val recommended = list.first { it.isRecommended }
        assertEquals("v0.19.0", recommended.tagName)
        assertNotNull(recommended.sha256Hex)
        assertEquals("firmware/xibalba-t-embed-cc1101.bin", recommended.bundledAssetPath)
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
