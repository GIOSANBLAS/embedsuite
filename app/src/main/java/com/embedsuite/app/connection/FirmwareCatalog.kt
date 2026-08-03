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
    val sha256Hex: String? = null
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

    val XIBALBA_FALLBACK_V0164: FirmwareRelease = FirmwareRelease(
        tagName = "v0.16.4",
        name = "v0.16.4 Glow",
        downloadUrl = "https://github.com/GIOSANBLAS/te-embed-xibalba/releases/download/v0.16.4/te-embed-xibalba.bin",
        fileName = "te-embed-xibalba.bin",
        isPrerelease = true,
        source = FirmwareSource.OFFICIAL_XIBALBA,
        description = "T-Embed Xibalba v0.16.4 Glow (BQ25896 charger + hardware TinyUSB HID + TEH-Link caps)"
    )

    val XIBALBA_FALLBACK_V0162: FirmwareRelease = FirmwareRelease(
        tagName = "v0.16.2",
        name = "v0.16.2 Glow",
        downloadUrl = "https://github.com/GIOSANBLAS/te-embed-xibalba/releases/download/v0.16.2/te-embed-xibalba.bin",
        fileName = "te-embed-xibalba.bin",
        isPrerelease = true,
        source = FirmwareSource.OFFICIAL_XIBALBA,
        description = "T-Embed Xibalba v0.16.2 Glow (embedded splash logo + LVGL polish)",
        sha256Hex = "6fbdbaeeccfbd017bf71ffe1475e170c8b9970d5528f1c9466ed749fc404c512"
    )

    val XIBALBA_FALLBACK_V0161: FirmwareRelease = XIBALBA_FALLBACK_V0164
    val XIBALBA_FALLBACK_V016: FirmwareRelease = XIBALBA_FALLBACK_V0164
    val XIBALBA_FALLBACK_V0141: FirmwareRelease = XIBALBA_FALLBACK_V0164
    val XIBALBA_FALLBACK_V014: FirmwareRelease = XIBALBA_FALLBACK_V0164
    val XIBALBA_FALLBACK_V013: FirmwareRelease = XIBALBA_FALLBACK_V0164

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
