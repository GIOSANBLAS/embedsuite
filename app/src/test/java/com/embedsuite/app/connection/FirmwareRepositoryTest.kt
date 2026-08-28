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
    fun pickRecommended_prefersNewerStable() {
        val older = FirmwareRelease(
            tagName = "v1.7.0",
            name = "Bruce",
            downloadUrl = "https://example.com/bruce-170.bin",
            fileName = "Bruce-lilygo-t-embed-cc1101.bin",
            isPrerelease = false,
            source = FirmwareSource.OFFICIAL_BRUCE
        )
        val newer = FirmwareRelease(
            tagName = "v1.8.0",
            name = "Bruce",
            downloadUrl = "https://example.com/bruce-180.bin",
            fileName = "Bruce-lilygo-t-embed-cc1101.bin",
            isPrerelease = false,
            source = FirmwareSource.OFFICIAL_BRUCE
        )
        val recommended = FirmwareCatalog.pickRecommended(listOf(older, newer), FirmwareProfile.BRUCE)
        assertEquals(FirmwareSource.OFFICIAL_BRUCE, recommended?.source)
        assertEquals("v1.8.0", recommended?.tagName)
    }

    @Test
    fun fallbackReleases_recommendsEmbeddedBruce() {
        val list = FirmwareCatalog.fallbackReleases()
        assertEquals(1, list.size)
        val recommended = list.first { it.isRecommended }
        assertEquals(FirmwareCatalog.BRUCE_EMBEDDED.downloadUrl, recommended.downloadUrl)
    }

    @Test
    fun embeddedReleases_containsOfficialBruce() {
        val embedded = FirmwareCatalog.embeddedReleases()
        assertEquals(1, embedded.size)
        assertEquals(FirmwareCatalog.BRUCE_EMBEDDED.tagName, embedded.first().tagName)
        assertTrue(embedded.first().downloadUrl.contains("pr3y/Bruce"))
    }

    @Test
    fun mergeWithEmbedded_preservesSha256FromEmbedded() {
        val embedded = FirmwareCatalog.BRUCE_EMBEDDED.copy(sha256Hex = "abc123")
        val catalog = object {
            fun merge(remote: List<FirmwareRelease>): List<FirmwareRelease> {
                val merged = linkedMapOf<String, FirmwareRelease>()
                listOf(embedded).forEach { merged[it.tagName.lowercase()] = it }
                remote.forEach { release ->
                    val hit = merged[release.tagName.lowercase()]
                    merged[release.tagName.lowercase()] = if (hit != null && release.sha256Hex.isNullOrBlank()) {
                        release.copy(sha256Hex = hit.sha256Hex)
                    } else release
                }
                return merged.values.toList()
            }
        }
        val remote = listOf(
            FirmwareRelease(
                tagName = "latest",
                name = "from github",
                downloadUrl = "https://github.com/pr3y/Bruce/releases/latest/download/Bruce-lilygo-t-embed-cc1101.bin",
                fileName = "Bruce-lilygo-t-embed-cc1101.bin",
                isPrerelease = false,
                source = FirmwareSource.OFFICIAL_BRUCE,
                sha256Hex = null
            )
        )
        val merged = catalog.merge(remote)
        assertEquals("abc123", merged.first().sha256Hex)
    }

    @Test
    fun settingsDisplayOrder_isBruceOnly() {
        assertEquals(listOf(FirmwareProfile.BRUCE), FirmwareProfile.settingsDisplayOrder)
    }

    @Test
    fun fromPref_defaultsToBruce() {
        assertEquals(FirmwareProfile.BRUCE, FirmwareProfile.fromPref(null))
        assertEquals(FirmwareProfile.BRUCE, FirmwareProfile.fromPref("invalid"))
        assertEquals(FirmwareProfile.BRUCE, FirmwareProfile.fromPref("BRUCE"))
        assertEquals(FirmwareProfile.BRUCE, FirmwareProfile.fromPref("BRUCE_TEHLINK"))
    }
}
