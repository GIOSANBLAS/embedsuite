package com.embedsuite.app.engine.ota

import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.connection.FirmwareRelease
import com.embedsuite.app.connection.TransportType
import com.embedsuite.app.flash.FirmwareFlashCoordinator
import kotlinx.coroutines.delay

data class HealthResult(
    val healthy: Boolean,
    val pingOk: Boolean,
    val bruceDetected: Boolean,
    val message: String
)

data class RollbackResult(
    val success: Boolean,
    val attemptedRelease: FirmwareRelease,
    val rolledBack: Boolean,
    val healthAfterFlash: HealthResult?,
    val message: String
)

object SmartOtaGuard {

    suspend fun postFlashHealthCheck(cm: DeviceConnectionManager): HealthResult {
        if (cm.connectionState.value !is ConnectionState.Connected ||
            cm.activeTransportType.value != TransportType.BLE
        ) {
            cm.connect(TransportType.BLE)
            delay(2_000)
        }

        cm.refreshSystemInfo()
        val bruceDetected = cm.detectedProfile.value == FirmwareProfile.BRUCE
        val pingOk = cm.bruceLinkReady.value

        val healthy = pingOk && bruceDetected
        val message = when {
            healthy -> "Dispositivo responde y perfil Bruce confirmado por BLE."
            !pingOk -> "Bruce BLE no respondió tras el flasheo. Conecta por BLE."
            !bruceDetected -> "Perfil Bruce no detectado."
            else -> "Comprobación de salud fallida."
        }

        return HealthResult(
            healthy = healthy,
            pingOk = pingOk,
            bruceDetected = bruceDetected,
            message = message
        )
    }

    suspend fun flashWithRollback(
        coordinator: FirmwareFlashCoordinator,
        release: FirmwareRelease,
        previousBundledOrLocal: FirmwareRelease?
    ): RollbackResult {
        val flashResult = coordinator.flashOtaAndAwait(release)
        if (flashResult.isFailure) {
            return RollbackResult(
                success = false,
                attemptedRelease = release,
                rolledBack = false,
                healthAfterFlash = null,
                message = flashResult.exceptionOrNull()?.message ?: "OTA falló."
            )
        }

        delay(3_000)
        val health = postFlashHealthCheck(coordinator.connectionManager)
        if (health.healthy) {
            return RollbackResult(
                success = true,
                attemptedRelease = release,
                rolledBack = false,
                healthAfterFlash = health,
                message = "OTA OK — ${health.message}"
            )
        }

        val fallback = previousBundledOrLocal
            ?: return RollbackResult(
                success = false,
                attemptedRelease = release,
                rolledBack = false,
                healthAfterFlash = health,
                message = "OTA aplicada pero salud fallida y sin firmware anterior para rollback."
            )

        val rollbackFlash = coordinator.flashOtaAndAwait(fallback)
        delay(3_000)
        val healthAfterRollback = postFlashHealthCheck(coordinator.connectionManager)

        return RollbackResult(
            success = healthAfterRollback.healthy,
            attemptedRelease = release,
            rolledBack = rollbackFlash.isSuccess,
            healthAfterFlash = healthAfterRollback,
            message = if (healthAfterRollback.healthy) {
                "Rollback a ${fallback.tagName} completado — ${healthAfterRollback.message}"
            } else {
                "Rollback intentado pero salud sigue fallida: ${healthAfterRollback.message}"
            }
        )
    }
}
