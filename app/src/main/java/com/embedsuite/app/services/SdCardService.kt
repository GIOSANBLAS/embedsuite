package com.embedsuite.app.services

import com.embedsuite.app.connection.SdFileEntry
import com.embedsuite.app.connection.SdStatus
import com.embedsuite.app.connection.XibalbaAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SdCardService — microSD del T-Embed vía TEH-Link.
 *
 * Monta la tarjeta bajo demanda, lista directorios y guarda sesiones/dumps
 * en /embedsuite/ para recuperarlos luego sin el teléfono.
 */
class SdCardService(
    private val xibalba: XibalbaAdapter
) {
    sealed class SdState {
        data object Disconnected : SdState()
        data object Mounting : SdState()
        data class Ready(val status: SdStatus) : SdState()
        data class Error(val message: String) : SdState()
    }

    private val _state = MutableStateFlow<SdState>(SdState.Disconnected)
    val state: StateFlow<SdState> = _state.asStateFlow()

    suspend fun mount(): Result<SdStatus> {
        _state.value = SdState.Mounting
        val result = xibalba.sdMount()
        result.fold(
            onSuccess = { _state.value = SdState.Ready(it) },
            onFailure = { _state.value = SdState.Error(it.message ?: "sd_mount_failed") }
        )
        return result
    }

    suspend fun refresh(): Result<SdStatus> {
        val result = xibalba.sdStatus()
        result.onSuccess {
            _state.value = if (it.mounted) SdState.Ready(it) else SdState.Disconnected
        }
        return result
    }

    suspend fun listFiles(path: String = "/embedsuite"): List<SdFileEntry> {
        return xibalba.sdListFiles(path).getOrDefault(emptyList())
    }

    /** Guarda una sesión (JSON) en la SD del dispositivo. */
    suspend fun saveSession(sessionId: String, json: String): Result<String> {
        val filename = "session_${sessionId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }}.json"
        return xibalba.sdSaveFile(filename, json.toByteArray(Charsets.UTF_8))
    }

    /** Guarda un dump arbitrario (NFC, IR, Sub-GHz) en la SD del dispositivo. */
    suspend fun saveDump(name: String, content: String): Result<String> {
        return xibalba.sdSaveFile(name, content.toByteArray(Charsets.UTF_8))
    }
}
