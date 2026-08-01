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
            tagName = "v0.15.0",
            name = "Ward",
            downloadUrl = "https://example.com/xibalba.bin",
            fileName = "te-embed-xibalba.bin",
            isPrerelease = true,
            source = FirmwareSource.OFFICIAL_XIBALBA
        )
        val recommended = FirmwareCatalog.pickRecommended(listOf(bruce, xibalba), FirmwareProfile.XIBALBA)
        assertEquals(FirmwareSource.OFFICIAL_XIBALBA, recommended?.source)
    }
}
