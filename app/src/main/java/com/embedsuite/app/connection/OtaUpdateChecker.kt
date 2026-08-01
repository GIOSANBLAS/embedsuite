package com.embedsuite.app.connection

sealed class OtaUpdateStatus {
    data object Unknown : OtaUpdateStatus()
    data object UpToDate : OtaUpdateStatus()
    data object Checking : OtaUpdateStatus()
    data class UpdateAvailable(
        val deviceVersion: String,
        val latestVersion: String,
        val release: FirmwareRelease,
        val sourceLabel: String = "Bruce"
    ) : OtaUpdateStatus()
    data class Error(val message: String) : OtaUpdateStatus()
}

class OtaUpdateChecker(private val firmwareRepository: FirmwareRepository) {

    private var cachedStatus: OtaUpdateStatus = OtaUpdateStatus.Unknown
    private var lastCheckMs: Long = 0
    private val cacheTtlMs = 24 * 60 * 60 * 1000L

    suspend fun check(
        deviceFirmware: String,
        profile: FirmwareProfile = FirmwareProfile.AUTO,
        force: Boolean = false
    ): OtaUpdateStatus {
        if (!force && cachedStatus !is OtaUpdateStatus.Unknown && System.currentTimeMillis() - lastCheckMs < cacheTtlMs) {
            return cachedStatus
        }
        val deviceVer = FirmwareRepository.extractVersion(deviceFirmware)
        if (deviceVer.isBlank()) return OtaUpdateStatus.Unknown

        val isXibalba = when (profile) {
            FirmwareProfile.XIBALBA -> true
            FirmwareProfile.BRUCE -> false
            FirmwareProfile.AUTO, FirmwareProfile.UNKNOWN ->
                deviceFirmware.contains("xibalba", ignoreCase = true)
        }
        val fetchResult = if (isXibalba) {
            firmwareRepository.fetchXibalbaReleases()
        } else {
            firmwareRepository.fetchTEmbedReleases()
        }

        return fetchResult.fold(
            onSuccess = { releases ->
                val latest = releases.firstOrNull { !it.isPrerelease }
                    ?: releases.firstOrNull()
                if (latest == null) {
                    cachedStatus = OtaUpdateStatus.UpToDate
                } else {
                    val latestVer = FirmwareRepository.extractVersion(latest.tagName)
                    val sourceLabel = if (isXibalba) "Xibalba" else "Bruce"
                    cachedStatus = if (FirmwareRepository.isNewer(latestVer, deviceVer)) {
                        OtaUpdateStatus.UpdateAvailable(deviceVer, latestVer, latest, sourceLabel)
                    } else {
                        OtaUpdateStatus.UpToDate
                    }
                }
                lastCheckMs = System.currentTimeMillis()
                cachedStatus
            },
            onFailure = { e ->
                OtaUpdateStatus.Error(e.message ?: "Error OTA check")
            }
        )
    }
}
