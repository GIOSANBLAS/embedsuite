package com.embedsuite.app.connection

enum class FirmwareSource {
    /** pr3y/Bruce — firmware oficial T-Embed CC1101 Plus */
    OFFICIAL_BRUCE,
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
    val source: FirmwareSource = FirmwareSource.OFFICIAL_BRUCE,
    val localFilePath: String? = null,
    val isRecommended: Boolean = false,
    val description: String = "",
    val sha256Hex: String? = null,
    /** Ruta en assets/ (p. ej. firmware/bruce-t-embed-cc1101.bin) — sin red ni token GitHub. */
    val bundledAssetPath: String? = null
) {
    val riskLevel: FirmwareRiskLevel = when (source) {
        FirmwareSource.OFFICIAL_BRUCE -> FirmwareRiskLevel.OFFICIAL
        FirmwareSource.CUSTOM_LOCAL -> FirmwareRiskLevel.CUSTOM
    }

    val requiresDisclaimer: Boolean = riskLevel == FirmwareRiskLevel.CUSTOM

    val displayLabel: String = when (source) {
        FirmwareSource.OFFICIAL_BRUCE -> "$tagName (Bruce)"
        FirmwareSource.CUSTOM_LOCAL -> name.ifBlank { fileName }
    }

    val isLocal: Boolean get() = localFilePath != null

    fun identityKey(): String = localFilePath ?: downloadUrl
}

object FirmwareCatalog {

    const val RECOMMENDATION_REASON_KEY = "firmware_recommend_reason"

    /** Catálogo embebido cuando GitHub no responde — apunta a pr3y/Bruce. */
    val BRUCE_EMBEDDED: FirmwareRelease = FirmwareRelease(
        tagName = "latest",
        name = "Bruce T-Embed CC1101",
        downloadUrl = "https://github.com/pr3y/Bruce/releases/latest/download/Bruce-lilygo-t-embed-cc1101.bin",
        fileName = "Bruce-lilygo-t-embed-cc1101.bin",
        isPrerelease = false,
        source = FirmwareSource.OFFICIAL_BRUCE,
        isRecommended = true,
        description = "Firmware Bruce oficial para LilyGO T-Embed CC1101 Plus (merged @ 0x0)"
    )

    fun embeddedReleases(): List<FirmwareRelease> = listOf(BRUCE_EMBEDDED)

    fun fallbackReleases(): List<FirmwareRelease> =
        markRecommended(embeddedReleases(), FirmwareProfile.BRUCE)

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
        return markRecommended(merged.values.toList(), FirmwareProfile.BRUCE)
    }

    fun markRecommended(
        releases: List<FirmwareRelease>,
        profile: FirmwareProfile = FirmwareProfile.BRUCE
    ): List<FirmwareRelease> {
        val recommended = pickRecommended(releases, profile) ?: return releases
        return releases.map { release ->
            release.copy(
                isRecommended = release.identityKey() == recommended.identityKey()
            )
        }
    }

    fun preferredSource(profile: FirmwareProfile): FirmwareSource = FirmwareSource.OFFICIAL_BRUCE

    fun pickRecommended(
        releases: List<FirmwareRelease>,
        profile: FirmwareProfile = FirmwareProfile.BRUCE
    ): FirmwareRelease? {
        val stable = releases.filter { it.source == FirmwareSource.OFFICIAL_BRUCE && !it.isPrerelease }
        val pool = stable.ifEmpty { releases.filter { it.source == FirmwareSource.OFFICIAL_BRUCE } }
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
