package com.embedsuite.app.connection

enum class FirmwareSource {
    /** GIOSANBLAS/te-embed-xibalba — T-Embed Xibalba */
    OFFICIAL_XIBALBA,
    /** Binario local elegido por el usuario */
    CUSTOM_LOCAL
}

enum class FirmwareRiskLevel {
    OFFICIAL,
    CUSTOM
}

data class FirmwareRelease(
    val tagName: String,
    val name: String,
    val downloadUrl: String,
    val fileName: String,
    val isPrerelease: Boolean,
    val source: FirmwareSource = FirmwareSource.OFFICIAL_XIBALBA,
    val localFilePath: String? = null,
    val isRecommended: Boolean = false,
    val description: String = "",
    val sha256Hex: String? = null,
    /** Ruta en assets/ (p. ej. firmware/te-embed-xibalba.bin) — sin red ni token GitHub. */
    val bundledAssetPath: String? = null
) {
    val riskLevel: FirmwareRiskLevel = when (source) {
        FirmwareSource.OFFICIAL_XIBALBA -> FirmwareRiskLevel.OFFICIAL
        FirmwareSource.CUSTOM_LOCAL -> FirmwareRiskLevel.CUSTOM
    }

    val requiresDisclaimer: Boolean = riskLevel == FirmwareRiskLevel.CUSTOM

    val displayLabel: String = when (source) {
        FirmwareSource.OFFICIAL_XIBALBA -> "$tagName (Xibalba)"
        FirmwareSource.CUSTOM_LOCAL -> name.ifBlank { fileName }
    }

    val isLocal: Boolean get() = localFilePath != null

    fun identityKey(): String = localFilePath ?: downloadUrl
}

object FirmwareCatalog {

    const val RECOMMENDATION_REASON_KEY = "firmware_recommend_reason"

    val XIBALBA_V0180: FirmwareRelease = FirmwareRelease(
        tagName = "v0.18.0",
        name = "v0.18.0 Iron Shield",
        downloadUrl = "https://github.com/GIOSANBLAS/te-embed-xibalba/releases/download/v0.18.0/te-embed-xibalba.bin",
        fileName = "te-embed-xibalba.bin",
        isPrerelease = false,
        source = FirmwareSource.OFFICIAL_XIBALBA,
        isRecommended = true,
        description = "T-Embed Xibalba v0.18.0 Iron Shield — Evil Portal, Beacon Spam, Modo Auditoría, TEH-Link v3",
        sha256Hex = "76fa3ed1c215e6bf4d5f9b9105ae42f68ad047007265aa6ddc7c7287cc2a31dd",
        bundledAssetPath = "firmware/te-embed-xibalba.bin"
    )

    /**
     * Nota: el asset embebido puede ser imagen APP (esptool @ 0x10000) si no hay merged en repo.
     * Instalación limpia estilo Bruce (merged @ 0x0): importar .bin merged desde release oficial.
     */

    val XIBALBA_FALLBACK_V0170: FirmwareRelease = FirmwareRelease(
        tagName = "v0.17.1",
        name = "v0.17.1 Spark",
        downloadUrl = "https://github.com/GIOSANBLAS/te-embed-xibalba/releases/download/v0.17.1/te-embed-xibalba.bin",
        fileName = "te-embed-xibalba.bin",
        isPrerelease = true,
        source = FirmwareSource.OFFICIAL_XIBALBA,
        description = "T-Embed Xibalba v0.17.1 Spark (safe CDC hardware profile, OTA rollback, nRF24/NFC/EXFIL)",
        sha256Hex = null
    )

    val XIBALBA_FALLBACK_V0165: FirmwareRelease = FirmwareRelease(
        tagName = "v0.16.5",
        name = "v0.16.5 Glow",
        downloadUrl = "https://github.com/GIOSANBLAS/te-embed-xibalba/releases/download/v0.16.5/te-embed-xibalba.bin",
        fileName = "te-embed-xibalba.bin",
        isPrerelease = true,
        source = FirmwareSource.OFFICIAL_XIBALBA,
        description = "T-Embed Xibalba v0.16.5 Glow (BQ27220 SOC fix, capture freq_mhz, TinyUSB HID, charger status)"
    )

    val XIBALBA_FALLBACK_V0162: FirmwareRelease = FirmwareRelease(
        tagName = "v0.16.2",
        name = "v0.16.2 Glow",
        downloadUrl = "https://github.com/GIOSANBLAS/te-embed-xibalba/releases/download/v0.16.2/te-embed-xibalba.bin",
        fileName = "te-embed-xibalba.bin",
        isPrerelease = true,
        source = FirmwareSource.OFFICIAL_XIBALBA,
        description = "T-Embed Xibalba v0.16.2 Glow (embedded splash logo + LVGL polish)",
        sha256Hex = null
    )

    val XIBALBA_FALLBACK_V0161: FirmwareRelease = XIBALBA_FALLBACK_V0170
    val XIBALBA_FALLBACK_V016: FirmwareRelease = XIBALBA_FALLBACK_V0170
    val XIBALBA_FALLBACK_V0141: FirmwareRelease = XIBALBA_FALLBACK_V0170
    val XIBALBA_FALLBACK_V014: FirmwareRelease = XIBALBA_FALLBACK_V0170
    val XIBALBA_FALLBACK_V013: FirmwareRelease = XIBALBA_FALLBACK_V0170

    /** Catálogo embebido — siempre disponible sin GitHub API (repo privado / sin red). */
    fun embeddedReleases(): List<FirmwareRelease> = listOf(
        XIBALBA_V0180,
        XIBALBA_FALLBACK_V0162.copy(isPrerelease = true, isRecommended = false),
        XIBALBA_FALLBACK_V0165.copy(isPrerelease = true, isRecommended = false)
    )

    fun fallbackReleases(): List<FirmwareRelease> =
        markRecommended(embeddedReleases(), FirmwareProfile.XIBALBA)

    fun mergeWithEmbedded(remote: List<FirmwareRelease>): List<FirmwareRelease> {
        val merged = linkedMapOf<String, FirmwareRelease>()
        embeddedReleases().forEach { merged[it.tagName.lowercase()] = it }
        remote.forEach { release ->
            val embedded = merged[release.tagName.lowercase()]
            merged[release.tagName.lowercase()] = if (embedded != null && release.sha256Hex.isNullOrBlank()) {
                release.copy(
                    sha256Hex = embedded.sha256Hex,
                    description = embedded.description.ifBlank { release.description },
                    isRecommended = embedded.isRecommended
                )
            } else {
                release
            }
        }
        return markRecommended(merged.values.toList(), FirmwareProfile.XIBALBA)
    }

    fun markRecommended(
        releases: List<FirmwareRelease>,
        profile: FirmwareProfile = FirmwareProfile.XIBALBA
    ): List<FirmwareRelease> {
        val recommended = pickRecommended(releases, profile) ?: return releases
        return releases.map { release ->
            release.copy(
                isRecommended = release.identityKey() == recommended.identityKey()
            )
        }
    }

    fun preferredSource(profile: FirmwareProfile): FirmwareSource = FirmwareSource.OFFICIAL_XIBALBA

    fun pickRecommended(
        releases: List<FirmwareRelease>,
        profile: FirmwareProfile = FirmwareProfile.XIBALBA
    ): FirmwareRelease? {
        val stable = releases.filter { it.source == FirmwareSource.OFFICIAL_XIBALBA && !it.isPrerelease }
        val pool = stable.ifEmpty { releases.filter { it.source == FirmwareSource.OFFICIAL_XIBALBA } }
        if (pool.isEmpty()) return null
        return pool.reduce { best, current ->
            val bestVer = FirmwareRepository.extractVersion(best.tagName)
            val curVer = FirmwareRepository.extractVersion(current.tagName)
            if (FirmwareRepository.isNewer(curVer, bestVer)) current else best
        }
    }

    fun customFromFile(file: java.io.File, displayName: String? = null): FirmwareRelease {
        return FirmwareRelease(
            tagName = "CUSTOM",
            name = displayName ?: file.nameWithoutExtension,
            downloadUrl = "",
            fileName = file.name,
            isPrerelease = false,
            source = FirmwareSource.CUSTOM_LOCAL,
            localFilePath = file.absolutePath,
            isRecommended = false,
            description = ""
        )
    }
}
