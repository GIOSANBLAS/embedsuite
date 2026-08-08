package com.embedsuite.app.flash

import android.content.Context
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.FirmwareRelease
import com.embedsuite.app.connection.FirmwareRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class FirmwareFlashCoordinator(
    private val appScope: CoroutineScope,
    private val connectionManager: DeviceConnectionManager,
    private val esptoolFlasher: EsptoolFlasher,
    private val firmwareRepository: FirmwareRepository
) {
    private val _isFlashing = MutableStateFlow(false)
    val isFlashing: StateFlow<Boolean> = _isFlashing.asStateFlow()

    private val _otaProgress = MutableStateFlow(0)
    val otaProgress: StateFlow<Int> = _otaProgress.asStateFlow()

    private val _flashStatus = MutableStateFlow("")
    val flashStatus: StateFlow<String> = _flashStatus.asStateFlow()

    fun setStatusMessage(message: String) {
        _flashStatus.value = message
    }

    fun flashOta(context: Context, release: FirmwareRelease) {
        if (_isFlashing.value) return
        appScope.launch {
            _isFlashing.value = true
            try {
                setStatus(context, release, ota = true)
                _otaProgress.value = 5
                val cacheDir = File(context.cacheDir, "firmware")
                firmwareRepository.resolveFlashFile(context, release, cacheDir).fold(
                    onSuccess = { file ->
                        _flashStatus.value = context.getString(com.embedsuite.app.R.string.firmware_status_ota_upload)
                        val sha256 = release.sha256Hex?.trim()?.lowercase()
                            ?: FirmwareRepository.computeFileSha256Hex(file)
                        connectionManager.uploadFirmwareOta(file, sha256) { _otaProgress.value = it }.fold(
                            onSuccess = {
                                _flashStatus.value = context.getString(
                                    com.embedsuite.app.R.string.firmware_status_ota_ok,
                                    it
                                )
                            },
                            onFailure = {
                                _flashStatus.value = context.getString(
                                    com.embedsuite.app.R.string.firmware_status_ota_fail,
                                    it.message ?: "?"
                                )
                                _otaProgress.value = 0
                            }
                        )
                    },
                    onFailure = {
                        _flashStatus.value = context.getString(
                            com.embedsuite.app.R.string.firmware_status_error,
                            it.message ?: "?"
                        )
                        _otaProgress.value = 0
                    }
                )
            } finally {
                _isFlashing.value = false
            }
        }
    }

    fun flashUsb(context: Context, release: FirmwareRelease) {
        if (_isFlashing.value) return
        appScope.launch {
            _isFlashing.value = true
            try {
                setStatus(context, release, ota = false)
                _otaProgress.value = 5
                val cacheDir = File(context.cacheDir, "firmware")
                firmwareRepository.resolveFlashFile(context, release, cacheDir).fold(
                    onSuccess = { file ->
                        val analysis = com.embedsuite.app.flash.FirmwareImageAnalyzer.analyze(file)
                        _flashStatus.value = when (analysis.kind) {
                            com.embedsuite.app.flash.FirmwareImageAnalyzer.ImageKind.MERGED_FULL ->
                                context.getString(
                                    com.embedsuite.app.R.string.firmware_status_usb_merged,
                                    release.fileName
                                )
                            com.embedsuite.app.flash.FirmwareImageAnalyzer.ImageKind.APP_ONLY ->
                                context.getString(
                                    com.embedsuite.app.R.string.firmware_status_usb_app,
                                    analysis.appVersion ?: release.tagName
                                )
                        }
                        analysis.warning?.let { _flashStatus.value = it }
                        connectionManager.prepareForUsbFlash()
                        _flashStatus.value = context.getString(com.embedsuite.app.R.string.firmware_status_usb_flash)
                        esptoolFlasher.flashFirmware(file) { pct, msg ->
                            _otaProgress.value = pct
                            _flashStatus.value = msg
                        }.fold(
                            onSuccess = {
                                _flashStatus.value = context.getString(
                                    com.embedsuite.app.R.string.firmware_status_usb_ok,
                                    it
                                )
                                connectionManager.reconnectAfterUsbFlash()
                            },
                            onFailure = {
                                _flashStatus.value = context.getString(
                                    com.embedsuite.app.R.string.firmware_status_usb_fail,
                                    it.message ?: "?"
                                )
                                _otaProgress.value = 0
                            }
                        )
                    },
                    onFailure = {
                        _flashStatus.value = context.getString(
                            com.embedsuite.app.R.string.firmware_status_error,
                            it.message ?: "?"
                        )
                        _otaProgress.value = 0
                    }
                )
            } finally {
                _isFlashing.value = false
            }
        }
    }

    private fun setStatus(context: Context, release: FirmwareRelease, ota: Boolean) {
        _flashStatus.value = when {
            release.isLocal && ota -> context.getString(com.embedsuite.app.R.string.firmware_status_using_local, release.fileName)
            release.isLocal && !ota -> context.getString(com.embedsuite.app.R.string.firmware_status_preparing_usb, release.fileName)
            release.bundledAssetPath != null -> context.getString(
                com.embedsuite.app.R.string.firmware_status_using_bundled,
                release.tagName
            )
            else -> context.getString(com.embedsuite.app.R.string.firmware_status_downloading, release.fileName)
        }
    }
}
