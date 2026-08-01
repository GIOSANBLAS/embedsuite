package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.embedsuite.app.AppContainer

class EmbedViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
            DashboardViewModel(
                connectionManager = container.connectionManager,
                signalRepository = container.signalRepository,
                txHistoryRepository = container.txHistoryRepository,
                sessionStats = container.sessionStats,
                firmwareRepository = container.firmwareRepository,
                otaUpdateChecker = container.otaUpdateChecker,
                locationTracker = container.locationTracker
            ) as T
        modelClass.isAssignableFrom(MapToolsViewModel::class.java) ->
            MapToolsViewModel(
                connectionManager = container.connectionManager,
                locationTracker = container.locationTracker,
                exportHelper = container.exportHelper,
                backupManager = container.backupManager,
                signalRepository = container.signalRepository,
                irRepository = container.irRepository,
                nfcDumpRepository = container.nfcDumpRepository,
                sessionReportGenerator = container.sessionReportGenerator,
                firmwareRepository = container.firmwareRepository,
                otaUpdateChecker = container.otaUpdateChecker
            ) as T
        modelClass.isAssignableFrom(NfcIrViewModel::class.java) ->
            NfcIrViewModel(
                connectionManager = container.connectionManager,
                irRepository = container.irRepository,
                nfcDumpRepository = container.nfcDumpRepository
            ) as T
        modelClass.isAssignableFrom(WirelessViewModel::class.java) ->
            WirelessViewModel(
                wirelessScanner = container.wirelessScanner,
                bleProfileRepository = container.bleProfileRepository,
                bleGattClient = container.bleGattClient
            ) as T
        modelClass.isAssignableFrom(ConsoleViewModel::class.java) ->
            ConsoleViewModel(
                connectionManager = container.connectionManager,
                macroRepository = container.macroRepository,
                macroEngine = container.macroEngine
            ) as T
        modelClass.isAssignableFrom(RfHubViewModel::class.java) ->
            RfHubViewModel(connectionManager = container.connectionManager) as T
        modelClass.isAssignableFrom(AiAssistantViewModel::class.java) ->
            AiAssistantViewModel(
                aiEngine = container.aiEngine,
                connectionManager = container.connectionManager,
                signalRepository = container.signalRepository
            ) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
