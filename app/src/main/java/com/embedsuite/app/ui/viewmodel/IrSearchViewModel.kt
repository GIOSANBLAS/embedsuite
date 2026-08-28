package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.core.orchestrator.AutoDiscoveryManager
import com.embedsuite.app.core.orchestrator.IntentOrchestrator
import com.embedsuite.app.core.orchestrator.IrIntent
import com.embedsuite.app.core.orchestrator.OrchestrationResult
import com.embedsuite.app.data.IrdbEntryEntity
import com.embedsuite.app.data.IrdbRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IrSearchUiState(
    val query: String = "",
    val results: List<IrdbEntryEntity> = emptyList(),
    val indexedCount: Int = 0,
    val syncing: Boolean = false,
    val transmitting: Boolean = false,
    val lastResult: OrchestrationResult? = null,
    val status: String = ""
)

class IrSearchViewModel(
    private val irdbRepository: IrdbRepository,
    private val orchestrator: IntentOrchestrator,
    private val autoDiscovery: AutoDiscoveryManager
) : ViewModel() {

    private val _state = MutableStateFlow(IrSearchUiState())
    val state: StateFlow<IrSearchUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(indexedCount = irdbRepository.indexedCount())
        }
    }

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
        viewModelScope.launch {
            _state.value = _state.value.copy(results = irdbRepository.search(q))
        }
    }

    fun syncIndex() {
        viewModelScope.launch {
            _state.value = _state.value.copy(syncing = true, status = "Indexando IRDB…")
            irdbRepository.syncIndex().fold(
                onSuccess = { n ->
                    _state.value = _state.value.copy(
                        syncing = false,
                        indexedCount = n,
                        status = "$n archivos indexados",
                        results = irdbRepository.search(_state.value.query)
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(syncing = false, status = it.message ?: "Sync falló")
                }
            )
        }
    }

    fun transmit(entry: IrdbEntryEntity) {
        viewModelScope.launch {
            _state.value = _state.value.copy(transmitting = true, lastResult = null)
            autoDiscovery.ensureCliReady().onFailure { err ->
                _state.value = _state.value.copy(
                    transmitting = false,
                    lastResult = OrchestrationResult(false, err.message ?: "Sin CLI")
                )
                return@launch
            }
            val content = irdbRepository.downloadContent(entry.path).getOrElse { err ->
                _state.value = _state.value.copy(transmitting = false, status = err.message ?: "Download falló")
                return@launch
            }
            val remote = "/bruce/ir/${entry.fileName}.ir"
            val intent = IrIntent.TransmitLocal(content, remote)
            val result = orchestrator.execute(intent)
            _state.value = _state.value.copy(transmitting = false, lastResult = result, status = result.message)
        }
    }
}
