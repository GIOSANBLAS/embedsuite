package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.core.orchestrator.AutoDiscoveryManager
import com.embedsuite.app.core.orchestrator.BadUsbIntent
import com.embedsuite.app.core.orchestrator.BadUsbTemplates
import com.embedsuite.app.core.orchestrator.DuckyBlock
import com.embedsuite.app.core.orchestrator.IntentOrchestrator
import com.embedsuite.app.core.orchestrator.OrchestrationResult
import com.embedsuite.app.engine.payload.DuckyEditor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BadUsbForgeUiState(
    val blocks: List<DuckyBlock> = BadUsbTemplates.notepad.blocks,
    val remoteFileName: String = "embed_payload.txt",
    val busy: Boolean = false,
    val validationIssues: List<DuckyEditor.ValidationIssue> = emptyList(),
    val lastResult: OrchestrationResult? = null,
    val transportHint: String = "",
    val showAdvancedScript: Boolean = false,
    val scriptPreview: String = ""
)

class BadUsbForgeViewModel(
    private val orchestrator: IntentOrchestrator,
    private val autoDiscovery: AutoDiscoveryManager
) : ViewModel() {

    private val _state = MutableStateFlow(BadUsbForgeUiState())
    val state: StateFlow<BadUsbForgeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val hint = autoDiscovery.hintForFileUpload()
            refreshPreview()
            _state.value = _state.value.copy(transportHint = hint.message)
        }
    }

    fun setBlocks(blocks: List<DuckyBlock>) {
        _state.value = _state.value.copy(blocks = blocks)
        refreshPreview()
    }

    fun updateBlock(index: Int, block: DuckyBlock) {
        val updated = _state.value.blocks.mapIndexed { i, b -> if (i == index) block else b }
        setBlocks(updated)
    }

    fun setRemoteFileName(name: String) {
        _state.value = _state.value.copy(remoteFileName = name)
    }

    fun toggleAdvancedScript() {
        _state.value = _state.value.copy(showAdvancedScript = !_state.value.showAdvancedScript)
    }

    fun applyTemplate(template: BadUsbTemplates.Template) {
        _state.value = _state.value.copy(
            blocks = template.blocks,
            remoteFileName = template.fileName
        )
        refreshPreview()
    }

    private fun refreshPreview() {
        val script = DuckyBlock.compile(_state.value.blocks)
        _state.value = _state.value.copy(
            scriptPreview = script,
            validationIssues = DuckyEditor.validate(script)
        )
    }

    fun runPipeline() {
        viewModelScope.launch {
            val script = DuckyBlock.compile(_state.value.blocks)
            val issues = DuckyEditor.validate(script)
            if (issues.isNotEmpty()) {
                _state.value = _state.value.copy(validationIssues = issues)
                return@launch
            }
            _state.value = _state.value.copy(busy = true, lastResult = null)
            autoDiscovery.ensureCliReady().onFailure { err ->
                _state.value = _state.value.copy(
                    busy = false,
                    lastResult = OrchestrationResult(false, err.message ?: "Sin CLI")
                )
                return@launch
            }
            val intent = BadUsbIntent(_state.value.blocks, _state.value.remoteFileName)
            val result = orchestrator.execute(intent)
            _state.value = _state.value.copy(busy = false, lastResult = result)
        }
    }
}
