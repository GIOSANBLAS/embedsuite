package com.embedsuite.app.connection

enum class FirmwareSource {
    /** GIOSANBLAS/te-embed-xibalba â€” T-Embed Xibalba */
    OFFICIAL_XIBALBA,
    /** BruceDevices/firmware â€” T-Embed CC1101 Plus */
    OFFICIAL_BRUCE,
    /** Binario local elegido por el usuario */
    CUSTOM_LOCAL
}

enum class FirmwareRiskLevel {
    /** Firmware oficial (Bruce o Xibalba) */
    OFFICIAL,
    /** Custom â€” responsabilidad del usuario */
    CUSTOM
}

data class FirmwareRelease(
    val tagName: String,
    val name: String,
    val downloadUrl: String,
    val fileName: String,
    val isPrerelease: Boolean,
    val source: FirmwareSource = FirmwareSource.OFFICIAL_BRUCE,
    val localFilePath: String? = null,
    val isRecommended: Boolean = false,
    val description: String = "",
    /** SHA256 hex opcional para verificaciÃ³n post-descarga (64 chars). */
    val sha256Hex: String? = null
) {
    val riskLevel: FirmwareRiskLevel = when (source) {
        FirmwareSource.OFFICIAL_BRUCE, FirmwareSource.OFFICIAL_XIBALBA -> FirmwareRiskLevel.OFFICIAL
        FirmwareSource.CUSTOM_LOCAL -> FirmwareRiskLevel.CUSTOM
    }

    val requiresDisclaimer: Boolean = riskLevel == FirmwareRiskLevel.CUSTOM

    val displayLabel: String = when (source) {
        FirmwareSource.OFFICIAL_XIBALBA -> "$tagName (Xibalba)"
        FirmwareSource.OFFICIAL_BRUCE -> tagName
        FirmwareSource.CUSTOM_LOCAL -> name.ifBlank { fileName }
    }

    val isLocal: Boolean get() = localFilePath != null

    fun identityKey(): String = localFilePath ?: downloadUrl
}

object FirmwareCatalog {

    /** Nota mostrada junto al firmware recomendado. */
    const val RECOMMENDATION_REASON_KEY = "firmware_recommend_reason"

    /** Fallback embebido si GitHub API no responde (v0.16.1 Beacon — boot splash UX). */
    val XIBALBA_FALLBACK_V0161: FirmwareRelease = FirmwareRelease(
        tagName = "v0.16.1",
        name = "v0.16.1 Beacon",
        downloadUrl = "https://github.com/GIOSANBLAS/te-embed-xibalba/releases/download/v0.16.1/te-embed-xibalba.bin",
        fileName = "te-embed-xibalba.bin",
        isPrerelease = true,
        source = FirmwareSource.OFFICIAL_XIBALBA,
        description = "T-Embed Xibalba v0.16.1 Beacon (boot splash LVGL + WS2812 chase)",
        sha256Hex = "5b24cd124073210dfcb5cb0c280ed738f9bb196530892fd116aff53679408555"
    )

    /** @deprecated Use [XIBALBA_FALLBACK_V0161] */
    val XIBALBA_FALLBACK_V016: FirmwareRelease = XIBALBA_FALLBACK_V0161

    /** @deprecated Use [XIBALBA_FALLBACK_V015] */
    val XIBALBA_FALLBACK_V0141: FirmwareRelease = XIBALBA_FALLBACK_V015

    /** @deprecated Use [XIBALBA_FALLBACK_V015] */
    val XIBALBA_FALLBACK_V014: FirmwareRelease = XIBALBA_FALLBACK_V015

    /** @deprecated Use [XIBALBA_FALLBACK_V015] */
    val XIBALBA_FALLBACK_V013: FirmwareRelease = XIBALBA_FALLBACK_V015

    fun markRecommended(
        releases: List<FirmwareRelease>,
        profile: FirmwareProfile = FirmwareProfile.AUTO
    ): List<FirmwareRelease> {
        val recommended = pickRecommended(releases, profile) ?: return releases
        return releases.map { release ->
            release.copy(
                isRecommended = release.identityKey() == recommended.identityKey()
            )
        }
    }

    fun preferredSource(profile: FirmwareProfile): FirmwareSource = when (profile) {
        FirmwareProfile.XIBALBA -> FirmwareSource.OFFICIAL_XIBALBA
        FirmwareProfile.BRUCE -> FirmwareSource.OFFICIAL_BRUCE
        FirmwareProfile.AUTO, FirmwareProfile.UNKNOWN -> FirmwareSource.OFFICIAL_XIBALBA
    }

    fun pickRecommended(
        releases: List<FirmwareRelease>,
        profile: FirmwareProfile = FirmwareProfile.AUTO
    ): FirmwareRelease? {
        val source = preferredSource(profile)
        val stable = releases.filter { it.source == source && !it.isPrerelease }
        val pool = stable.ifEmpty { releases.filter { it.source == source } }
        if (pool.isNotEmpty()) {
            return pool.reduce { best, current ->
                val bestVer = FirmwareRepository.extractVersion(best.tagName)
                val curVer = FirmwareRepository.extractVersion(current.tagName)
                if (FirmwareRepository.isNewer(curVer, bestVer)) current else best
            }
        }
        // Fallback: si no hay releases del perfil preferido, usar el otro oficial
        val altSource = if (source == FirmwareSource.OFFICIAL_XIBALBA) {
            FirmwareSource.OFFICIAL_BRUCE
        } else {
            FirmwareSource.OFFICIAL_XIBALBA
        }
        val altPool = releases.filter { it.source == altSource && !it.isPrerelease }
            .ifEmpty { releases.filter { it.source == altSource } }
        if (altPool.isEmpty()) return null
        return altPool.reduce { best, current ->
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
