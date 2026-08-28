package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.core.bruce.BruceCliCaptureParser
import com.embedsuite.app.core.orchestrator.AutoDiscoveryManager
import com.embedsuite.app.core.orchestrator.IntentOrchestrator
import com.embedsuite.app.core.orchestrator.IrIntent
import com.embedsuite.app.core.orchestrator.OrchestrationResult
import com.embedsuite.app.data.IrButtonEntity
import com.embedsuite.app.data.IrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IrFinderUiState(
    val listenSec: Int = 10,
    val busy: Boolean = false,
    val lastResult: OrchestrationResult? = null
)

class IrFinderViewModel(
    private val orchestrator: IntentOrchestrator,
    private val autoDiscovery: AutoDiscoveryManager,
    private val irRepository: IrRepository
) : ViewModel() {

    private val _state = MutableStateFlow(IrFinderUiState())
    val state: StateFlow<IrFinderUiState> = _state.asStateFlow()

    fun setListenSec(sec: Int) {
        _state.value = _state.value.copy(listenSec = sec.coerceIn(1, 60))
    }

    fun listen() {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, lastResult = null)
            autoDiscovery.ensureCliReady().onFailure { err ->
                _state.value = _state.value.copy(
                    busy = false,
                    lastResult = OrchestrationResult(false, err.message ?: "Sin CLI")
                )
                return@launch
            }
            val intent = IrIntent.Capture(_state.value.listenSec)
            val result = orchestrator.execute(intent)
            val responseText = when {
                result.cliResponse.isNotBlank() -> result.cliResponse
                result.localFile != null -> runCatching { result.localFile.readText() }.getOrDefault("")
                else -> ""
            }
            if (result.success && responseText.isNotBlank()) {
                runCatching {
                    BruceCliCaptureParser.parseIrCapture(responseText)?.let { cap ->
                        irRepository.save(
                            IrButtonEntity(
                                buttonName = "RX_${System.currentTimeMillis() % 100_000}",
                                protocol = cap.protocol,
                                hexCode = "${cap.address}:${cap.command}",
                                irPayload = cap.cliCommand
                            )
                        )
                    }
                }
            }
            _state.value = _state.value.copy(busy = false, lastResult = result)
        }
    }
}
