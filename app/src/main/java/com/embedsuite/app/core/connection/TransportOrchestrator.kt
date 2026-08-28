package com.embedsuite.app.core.connection

import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.TEmbedTransport
import com.embedsuite.app.connection.TransportType
import com.embedsuite.app.core.wifi.WifiApManager
import com.embedsuite.app.core.wifi.WifiFileTransfer
import java.io.File

/**
 * Selección task-aware USB / BLE / WiFi.
 */
class TransportOrchestrator(
    private val connectionManager: DeviceConnectionManager,
    private val wifiApManager: WifiApManager?,
    private val wifiFileTransfer: WifiFileTransfer = WifiFileTransfer()
) {

    suspend fun <T> withTransport(
        task: TransportTask,
        block: suspend (TEmbedTransport) -> Result<T>
    ): Result<T> {
        val primary = TransportSelector.preferred(task)
        ensureBound(primary)
        connectIfNeeded(primary).getOrElse { err ->
            val fb = TransportSelector.fallback(task, primary) ?: return Result.failure(err)
            ensureBound(fb)
            connectIfNeeded(fb).getOrElse { return Result.failure(it) }
            return runWithActive(block)
        }
        return runWithActive(block)
    }

    suspend fun executeBruceCliForTask(task: TransportTask, cliLine: String): Result<String> =
        withTransport(task) { transport ->
            connectionManager.executeBruceCliOn(transport, cliLine)
        }

    suspend fun downloadHeavyFile(remotePath: String, localFile: File): Result<String> {
        wifiApManager?.bindToWifiTransport()
        val wifiResult = connectIfNeeded(TransportType.WIFI)
        if (wifiResult.isFailure) {
            return Result.failure(
                Exception(
                    "Descarga WiFi requiere AP Bruce (${wifiResult.exceptionOrNull()?.message})"
                )
            )
        }
        val host = connectionManager.wifiHost()
        val transfer = WifiFileTransfer(host = host)
        return transfer.downloadFile(remotePath).mapCatching { bytes ->
            localFile.parentFile?.mkdirs()
            localFile.writeBytes(bytes)
            "descargado ${bytes.size} bytes → ${localFile.name}"
        }
    }

    suspend fun uploadHeavyFile(localFile: File, remotePath: String): Result<String> {
        if (!localFile.exists()) {
            return Result.failure(IllegalArgumentException("Archivo local no existe: ${localFile.name}"))
        }
        wifiApManager?.bindToWifiTransport()
        val wifiResult = connectIfNeeded(TransportType.WIFI)
        if (wifiResult.isFailure) {
            return Result.failure(
                Exception(
                    "${com.embedsuite.app.core.bruce.BruceLimits.WIFI_UPLOAD_HINT} (${wifiResult.exceptionOrNull()?.message})"
                )
            )
        }
        val host = connectionManager.wifiHost()
        val transfer = WifiFileTransfer(host = host)
        return transfer.uploadFile(localFile, remotePath.trim())
    }

    private suspend fun connectIfNeeded(type: TransportType): Result<String> {
        if (connectionManager.activeTransportType.value == type &&
            connectionManager.connectionState.value is com.embedsuite.app.connection.ConnectionState.Connected
        ) {
            return Result.success("already connected")
        }
        return connectionManager.connect(type)
    }

    private fun ensureBound(type: TransportType) {
        if (type == TransportType.WIFI) {
            wifiApManager?.bindToWifiTransport()
        }
    }

    private suspend fun <T> runWithActive(block: suspend (TEmbedTransport) -> Result<T>): Result<T> {
        val transport = connectionManager.activeTransportOrNull()
            ?: return Result.failure(IllegalStateException("Sin transporte activo"))
        return block(transport)
    }
}
