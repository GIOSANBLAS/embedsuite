package com.embedsuite.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.embedsuite.app.ai.AiPreferences
import com.embedsuite.app.ai.EmbedAiEngine
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.FirmwareRepository
import com.embedsuite.app.connection.OtaUpdateChecker
import com.embedsuite.app.core.AppPreferences
import com.embedsuite.app.core.CrashLogger
import com.embedsuite.app.core.SessionStatsTracker
import com.embedsuite.app.core.SoundFeedback
import com.embedsuite.app.data.*
import com.embedsuite.app.field.FieldOperationManager
import com.embedsuite.app.flash.EsptoolFlasher
import com.embedsuite.app.flash.FirmwareFlashCoordinator
import com.embedsuite.app.macro.MacroEngine
import com.embedsuite.app.rf.RfAutomationEngine
import com.embedsuite.app.rf.RfReplayEngine
import com.embedsuite.app.scan.BleGattClient
import com.embedsuite.app.scan.LocationTracker
import com.embedsuite.app.scan.WirelessScanner
import com.embedsuite.app.scripting.BuiltInScriptRepository
import com.embedsuite.app.scripting.ScriptRepository
import com.embedsuite.app.security.SecureStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EmbedApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(com.embedsuite.app.core.LocaleManager.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        CrashLogger.init(this)
        com.embedsuite.app.map.OsmdroidConfig.init(this)
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val appPreferences = AppPreferences(appContext)
    val secureStore = SecureStore(appContext)
    val sessionStats = SessionStatsTracker(appContext)

    val database: EmbedDatabase = EmbedDatabaseFactory.create(appContext, secureStore)

    val signalRepository = SignalRepository(database.capturedSignalDao())
    val irRepository = IrRepository(database.irButtonDao())
    val macroRepository = MacroRepository(database.macroDao())
    val profileRepository = ProfileRepository(database.profileDao())
    val txHistoryRepository = TxHistoryRepository(database.txHistoryDao())
    val nfcDumpRepository = NfcDumpRepository(database.nfcDumpDao())
    val bleProfileRepository = BleProfileRepository(database.bleProfileDao())
    val rfAutomationRepository = RfAutomationRepository(database.rfAutomationDao())
    val scriptRepository: ScriptRepository = BuiltInScriptRepository()

    val locationTracker = LocationTracker(appContext)
    val wirelessScanner = WirelessScanner(appContext)
    val usbSerialManager = UsbSerialManager(appContext)

    val connectionManager = DeviceConnectionManager(
        usbSerialManager = usbSerialManager,
        context = appContext,
        signalRepository = signalRepository,
        locationTracker = locationTracker,
        appPreferences = appPreferences,
        secureStore = secureStore,
        sessionStats = sessionStats
    )

    val esptoolFlasher = EsptoolFlasher(usbSerialManager)
    val firmwareRepository = FirmwareRepository(secureStore)
    val firmwareFlashCoordinator = FirmwareFlashCoordinator(
        appScope = appScope,
        connectionManager = connectionManager,
        esptoolFlasher = esptoolFlasher,
        firmwareRepository = firmwareRepository
    )
    val exportHelper = ExportHelper(appContext, signalRepository)
    val backupManager = BackupManager(appContext, database)
    val sessionReportGenerator = SessionReportGenerator(appContext, signalRepository, txHistoryRepository, sessionStats)
    val otaUpdateChecker = OtaUpdateChecker(firmwareRepository)
    val bleGattClient = BleGattClient(appContext)
    val macroEngine = MacroEngine(connectionManager, sessionStats)
    val rfReplayEngine = RfReplayEngine(connectionManager, txHistoryRepository)
    val rfAutomationEngine = RfAutomationEngine(
        context = appContext,
        repository = rfAutomationRepository,
        signalRepository = signalRepository,
        connectionManager = connectionManager,
        macroRepository = macroRepository,
        macroEngine = macroEngine
    )
    val mapTileCacheManager = com.embedsuite.app.map.MapTileCacheManager(appContext)

    val aiPreferences = AiPreferences(appContext, secureStore)
    val aiEngine = EmbedAiEngine(
        preferences = aiPreferences,
        connectionManager = connectionManager,
        signalRepository = signalRepository
    )

    init {
        instance = this
        val hadMock = appPreferences.ensureRealHardwareMode()
        if (hadMock) {
            secureStore.clearTehLinkAuthToken()
            Log.i(TAG, "Mock transport desactivado — token TEH-Link borrado; re-empareja con T-Embed físico.")
        }
        SoundFeedback.init()
        SoundFeedback.setEnabled(appPreferences.soundEnabled.value)
        rfAutomationEngine.start()
        scope.launch {
            runCatching { profileRepository.seedDefaultsIfEmpty() }
                .onFailure { e ->
                    Log.e(TAG, "seedDefaultsIfEmpty failed", e)
                }
        }
    }

    companion object {
        private const val TAG = "AppContainer"
        @Volatile
        var instance: AppContainer? = null
    }
}
