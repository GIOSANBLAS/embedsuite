package com.embedsuite.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.FirmwareCatalog
import com.embedsuite.app.connection.FirmwareRelease
import com.embedsuite.app.connection.FirmwareRepository
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.connection.FirmwareSource
import com.embedsuite.app.connection.OtaUpdateChecker
import com.embedsuite.app.connection.OtaUpdateStatus
import com.embedsuite.app.data.*
import com.embedsuite.app.flipper.FlipperFileManager
import com.embedsuite.app.scan.LocationTracker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class MapToolsUiState(
    val mappedSignals: List<CapturedSignalEntity> = emptyList(),
    val signalCount: Int = 0,
    val exportStatus: String = "",
    val releases: List<FirmwareRelease> = emptyList(),
    val customRelease: FirmwareRelease? = null,
    val selectedRelease: FirmwareRelease? = null,
    val recommendedRelease: FirmwareRelease? = null,
    val isLoadingReleases: Boolean = false,
    val otaStatus: OtaUpdateStatus = OtaUpdateStatus.Unknown,
    val importPreview: ImportPreview? = null
) {
    val allFirmwareOptions: List<FirmwareRelease> =
        releases + listOfNotNull(customRelease)
}

data class ImportPreview(
    val fileName: String,
    val type: String,
    val summary: String,
    val content: String
)

class MapToolsViewModel(
    private val connectionManager: DeviceConnectionManager,
    private val locationTracker: LocationTracker,
    private val exportHelper: ExportHelper,
    private val backupManager: BackupManager,
    private val signalRepository: SignalRepository,
    private val irRepository: IrRepository,
    private val nfcDumpRepository: NfcDumpRepository,
    private val sessionReportGenerator: SessionReportGenerator,
    private val firmwareRepository: FirmwareRepository,
    private val otaUpdateChecker: OtaUpdateChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapToolsUiState())
    val uiState: StateFlow<MapToolsUiState> = _uiState.asStateFlow()

    val connectionState = connectionManager.connectionState
    val systemInfo = connectionManager.systemInfo
    val detectedProfile = connectionManager.detectedProfile
    val location = locationTracker.location

    init {
        viewModelScope.launch {
            signalRepository.mappedSignals.collect { signals ->
                _uiState.update {
                    it.copy(mappedSignals = signals, signalCount = signals.size)
                }
            }
        }
        viewModelScope.launch {
            connectionManager.systemInfo.collect { info ->
                if (info.firmware.isNotBlank()) {
                    val status = otaUpdateChecker.check(info.firmware, detectedProfile.value)
                    _uiState.update { it.copy(otaStatus = status) }
                }
            }
        }
        loadReleases()
    }

    fun setExportStatus(msg: String) {
        _uiState.update { it.copy(exportStatus = msg) }
    }

    suspend fun exportJson() = exportHelper.exportJson()
    suspend fun exportCsv() = exportHelper.exportCsv()
    suspend fun exportKml() = exportHelper.exportKml()
    suspend fun exportBackup() = backupManager.exportFullBackup()
    suspend fun exportSessionHtml() = sessionReportGenerator.generateHtmlReport()
    suspend fun exportSessionPdf() = sessionReportGenerator.generatePdfReport()

    /** Exporta JSON de sesión al microSD del T-Embed (`/bruce_sessions/`). */
    suspend fun exportJsonToDeviceSd(): Result<String> {
        val file = exportJson().getOrElse { return Result.failure(it) }
        val name = "session_${System.currentTimeMillis()}.json"
        return connectionManager.sdCardSave(name, file.readText()).map { bytes ->
            "/bruce_sessions/$name ($bytes B)"
        }
    }

    suspend fun exportSub(context: Context): File? {
        val signal = signalRepository.getLatest() ?: return null
        return FlipperFileManager.writeSubFile(context, signal)
    }

    fun loadReleases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReleases = true) }
            val profile = detectedProfile.value
            firmwareRepository.fetchDeviceFirmwares().fold(
                onSuccess = { list ->
                    val recommended = FirmwareCatalog.pickRecommended(list, profile)
                    _uiState.update { state ->
                        val keepSelection = state.customRelease != null &&
                            state.selectedRelease?.source == FirmwareSource.CUSTOM_LOCAL
                        state.copy(
                            releases = list,
                            recommendedRelease = recommended,
                            selectedRelease = when {
                                keepSelection -> state.selectedRelease
                                state.selectedRelease != null &&
                                    list.any { it.identityKey() == state.selectedRelease?.identityKey() } ->
                                    state.selectedRelease
                                else -> recommended ?: list.firstOrNull()
                            },
                            isLoadingReleases = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoadingReleases = false, exportStatus = "Error: ${error.message}") }
                }
            )
        }
    }

    fun importCustomFirmware(context: Context, uri: Uri) {
        viewModelScope.launch {
            val cacheDir = File(context.cacheDir, "firmware")
            firmwareRepository.importLocalBin(context, uri, cacheDir).fold(
                onSuccess = { custom ->
                    _uiState.update {
                        it.copy(customRelease = custom, selectedRelease = custom)
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(exportStatus = "Custom FW: ${error.message}") }
                }
            )
        }
    }

    fun clearCustomFirmware() {
        _uiState.update { state ->
            val fallback = state.recommendedRelease ?: state.releases.firstOrNull()
            state.copy(customRelease = null, selectedRelease = fallback)
        }
    }

    fun selectRelease(release: FirmwareRelease) {
        _uiState.update { it.copy(selectedRelease = release) }
    }

    suspend fun resolveFlashFile(context: Context, release: FirmwareRelease, cacheDir: File) =
        firmwareRepository.resolveFlashFile(context, release, cacheDir)

    fun parseImportFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val name = uri.lastPathSegment ?: "import"
                val content = readImportContentLimited(context, uri)
                val ext = name.substringAfterLast('.', "").lowercase()
                val preview = when (ext) {
                    "sub" -> FlipperFileManager.parseSubFile(content)?.let {
                        ImportPreview(name, "RF .sub", "${it.protocol} @ ${it.frequency}", content)
                    }
                    "ir" -> FlipperFileManager.parseIrFile(content)?.let {
                        ImportPreview(name, "IR .ir", "${it.buttonName} (${it.protocol})", content)
                    }
                    "nfc" -> FlipperFileManager.parseNfcFile(content)?.let {
                        ImportPreview(name, "NFC .nfc", "UID: ${it.uid}", content)
                    }
                    "json" -> ImportPreview(name, "Backup JSON", "Restaurar backup completo", content)
                    else -> null
                }
                _uiState.update { it.copy(importPreview = preview ?: ImportPreview(name, "Desconocido", "Formato no reconocido", content)) }
            }.onFailure { e ->
                _uiState.update { it.copy(exportStatus = "Import error: ${e.message}") }
            }
        }
    }

    fun confirmImport() {
        viewModelScope.launch {
            val preview = _uiState.value.importPreview ?: return@launch
            when {
                preview.type.startsWith("RF") -> {
                    FlipperFileManager.parseSubFile(preview.content)?.let { signalRepository.saveImported(it) }
                    setExportStatus("Señal RF importada")
                }
                preview.type.startsWith("IR") -> {
                    FlipperFileManager.parseIrFile(preview.content)?.let { irRepository.save(it) }
                    setExportStatus("Botón IR importado")
                }
                preview.type.startsWith("NFC") -> {
                    FlipperFileManager.parseNfcFile(preview.content)?.let { nfcDumpRepository.save(it) }
                    setExportStatus("Dump NFC importado")
                }
                preview.type.contains("Backup") -> {
                    backupManager.importFullBackup(preview.content).fold(
                        onSuccess = { result -> setExportStatus("Restaurado: ${result.total} registros (RF:${result.signals} IR:${result.irButtons} NFC:${result.nfcDumps})") },
                        onFailure = { setExportStatus("Backup error: ${it.message}") }
                    )
                }
            }
            _uiState.update { it.copy(importPreview = null) }
        }
    }

    fun dismissImportPreview() {
        _uiState.update { it.copy(importPreview = null) }
    }

    private fun readImportContentLimited(context: Context, uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(8192)
            val out = StringBuilder()
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
                if (total > MAX_IMPORT_PREVIEW_BYTES) {
                    throw IllegalArgumentException(
                        "Archivo demasiado grande para vista previa (máx ${MAX_IMPORT_PREVIEW_BYTES / (1024 * 1024)} MB)"
                    )
                }
                out.append(String(buffer, 0, read, Charsets.UTF_8))
            }
            return out.toString()
        }
        return ""
    }

    companion object {
        private const val MAX_IMPORT_PREVIEW_BYTES = 2 * 1024 * 1024L
    }
}
