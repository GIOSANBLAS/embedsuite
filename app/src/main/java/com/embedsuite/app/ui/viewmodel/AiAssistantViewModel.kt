package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.embedsuite.app.ai.EmbedAiEngine
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.data.SignalRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AiAssistantViewModel(
    val aiEngine: EmbedAiEngine,
    private val connectionManager: DeviceConnectionManager,
    private val signalRepository: SignalRepository
) : ViewModel() {

    val messages = aiEngine.messages
    val isProcessing = aiEngine.isProcessing
    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState

    fun processInput(input: String) {
        viewModelScope.launch { aiEngine.processUserInput(input) }
    }

    fun analyzeLastSignal() {
        viewModelScope.launch { aiEngine.analyzeLastSignal() }
    }

    fun analyzeCapturedSignal() {
        viewModelScope.launch {
            val signal = signalRepository.getLatest() ?: return@launch
            val report = aiEngine.analyzeCapturedSignal(signal)
            aiEngine.processUserInput("Capturé esto, ¿qué es?\n$report")
        }
    }
}
