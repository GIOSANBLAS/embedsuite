package com.embedsuite.app.connection

enum class FirmwareSource {
    /** GIOSANBLAS/xibalba-bruce — T-Embed Xibalba (runtime Bruce + TEH-Link) */
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
    /** Ruta en assets/ (p. ej. firmware/xibalba-t-embed-cc1101.bin) — sin red ni token GitHub. */
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

    /** Runtime oficial actual: Bruce + TEH-Link v3 + UI ES Maya/cyber. */
    val XIBALBA_V0190: FirmwareRelease = FirmwareRelease(
        tagName = "v0.19.0",
        name = "Xibalba-0.19.0 Maya",
        downloadUrl = "https://github.com/GIOSANBLAS/xibalba-bruce/releases/download/v0.19.0/xibalba-t-embed-cc1101.bin",
        fileName = "xibalba-t-embed-cc1101.bin",
        isPrerelease = false,
        source = FirmwareSource.OFFICIAL_XIBALBA,
        isRecommended = true,
        description = "T-Embed Xibalba-0.19.0 Maya — runtime Bruce + TEH-Link v3 USB, UI español, shell Maya/cyber EmbedSuite (merged @ 0x0)",
        sha256Hex = "f19a06cb8491edbe7c267f03a91be1649e0ed4dc214da102995cf6325c9f58c9",
        bundledAssetPath = "firmware/xibalba-t-embed-cc1101.bin"
    )

    /** Catálogo embebido — siempre disponible sin GitHub API (repo privado / sin red). */
    fun embeddedReleases(): List<FirmwareRelease> = listOf(XIBALBA_V0190)

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
