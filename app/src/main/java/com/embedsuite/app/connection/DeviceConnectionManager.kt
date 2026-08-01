package com.embedsuite.app.connection

import android.content.Context
import com.embedsuite.app.UsbSerialManager
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.core.AppPreferences
import com.embedsuite.app.core.SoundFeedback
import com.embedsuite.app.rf.RfFrequencyPresets
import com.embedsuite.app.rf.RfLiveEngine
import com.embedsuite.app.rf.RfLiveSnapshot
import com.embedsuite.app.rf.RfProtocolDecoder
import com.embedsuite.app.scan.LocationTracker
import com.embedsuite.app.widget.EmbedWidgetProvider
import com.embedsuite.app.widget.WidgetStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import com.embedsuite.app.data.SignalRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.IOException
import com.embedsuite.app.BuildConfig
import org.json.JSONObject

class DeviceConnectionManager(
    usbSerialManager: UsbSerialManager,
    context: Context,
    private val signalRepository: SignalRepository? = null,
    private val locationTracker: LocationTracker? = null,
    private val appPreferences: AppPreferences? = null,
    private val sessionStats: com.embedsuite.app.core.SessionStatsTracker? = null
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val usbTransport = UsbTransport(usbSerialManager)
    private val wifiTransport = WifiTransport()
    private val bleTransport = BleTransport(context)
    private val mockTransport = MockTransport()

    private val tehLinkClient = TehLinkClient(scope)

    private var activeTransport: TEmbedTransport? = null

    private val _detectedProfile = MutableStateFlow(FirmwareProfile.UNKNOWN)
    val detectedProfile: StateFlow<FirmwareProfile> = _detectedProfile.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _activeTransportType = MutableStateFlow(TransportType.USB)
    val activeTransportType: StateFlow<TransportType> = _activeTransportType.asStateFlow()

    private val _events = MutableSharedFlow<BruceEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<BruceEvent> = _events.asSharedFlow()

    private val _systemInfo = MutableStateFlow(SystemInfo())
    val systemInfo: StateFlow<SystemInfo> = _systemInfo.asStateFlow()

    private val _signalLog = MutableStateFlow<List<SignalEntry>>(emptyList())
    val signalLog: StateFlow<List<SignalEntry>> = _signalLog.asStateFlow()

    private val _lastDecoded = MutableStateFlow<String?>(null)
    val lastDecoded: StateFlow<String?> = _lastDecoded.asStateFlow()

    private val _subGhzFrequencyMhz = MutableStateFlow(
        appPreferences?.rfFrequencyMhz ?: RfFrequencyPresets.DEFAULT
    )
    val subGhzFrequencyMhz: StateFlow<String> = _subGhzFrequencyMhz.asStateFlow()

    private val _rfLive = MutableStateFlow(RfLiveSnapshot())
    val rfLive: StateFlow<RfLiveSnapshot> = _rfLive.asStateFlow()

    private val _incomingRaw = MutableSharedFlow<String>(extraBufferCapacity = 256)

    val mappedSignals: Flow<List<CapturedSignalEntity>> =
        signalRepository?.mappedSignals ?: flowOf(emptyList())

    val bleTransportRef: BleTransport get() = bleTransport

    init {
        scope.launch { usbTransport.incomingLines().collect { handleIncomingLine(it) } }
        scope.launch { wifiTransport.incomingLines().collect { handleIncomingLine(it) } }
        scope.launch { bleTransport.incomingLines().collect { handleIncomingLine(it) } }
        scope.launch { mockTransport.incomingLines().collect { handleIncomingLine(it) } }
        observeReconnectPolicy()
        observeTransportPreferenceChanges()
    }

    private fun observeReconnectPolicy() {
        val prefs = appPreferences ?: return
        scope.launch {
            combine(prefs.autoReconnect, _connectionState) { autoReconnect, state ->
                autoReconnect to state
            }.collect { (autoReconnect, state) ->
                if (autoReconnect && state is ConnectionState.Disconnected) {
                    delay(3000)
                    if (_connectionState.value is ConnectionState.Disconnected) {
                        connect(prefs.defaultTransport.value)
                    }
                }
            }
        }
    }

    private fun observeTransportPreferenceChanges() {
        val prefs = appPreferences ?: return
        scope.launch {
            prefs.defaultTransport.drop(1).collect { newTransport ->
                val current = _connectionState.value
                if (current is ConnectionState.Connected && _activeTransportType.value != newTransport) {
                    connect(newTransport)
                }
            }
        }
        scope.launch {
            prefs.rfFrequencyMhzFlow.collect { mhz ->
                _subGhzFrequencyMhz.value = mhz
            }
        }
    }

    fun wifiHost(): String = WifiTransport.DEFAULT_HOST

    fun setWifiHost(host: String) {
        wifiTransport.updateHost(host)
    }

    suspend fun setSubGhzFrequency(mhz: String): Result<String> {
        val normalized = mhz.trim().ifBlank { RfFrequencyPresets.DEFAULT }
        _subGhzFrequencyMhz.value = normalized
        appPreferences?.rfFrequencyMhz = normalized
        // Bruce Serial wiki no documenta `subghz setfrequency`. Solo persistimos en app;
        // el usuario debe fijar freq en el menú RF del T-Embed si el firmware lo requiere.
        return Result.success(BruceCommands.FREQ_LOCAL_HINT)
    }

    suspend fun connect(type: TransportType): Result<String> {
        _connectionState.value = ConnectionState.Connecting
        activeTransport?.disconnect()

        if (BuildConfig.ENABLE_MOCK_TRANSPORT && appPreferences?.useMockTransport == true) {
            val detail = mockTransport.connect().getOrElse { return Result.failure(it) }
            activeTransport = mockTransport
            _activeTransportType.value = TransportType.USB
            _detectedProfile.value = FirmwareProfile.XIBALBA
            _connectionState.value = ConnectionState.Connected(TransportType.USB, detail)
            SoundFeedback.playConnect()
            EmbedWidgetProvider.updateAllWidgets(appContext)
            return Result.success(detail)
        }

        val transport = when (type) {
            TransportType.USB -> usbTransport
            TransportType.WIFI -> wifiTransport
            TransportType.BLE -> bleTransport
        }

        val result = transport.connect()
        result.fold(
            onSuccess = { detail ->
                activeTransport = transport
                _activeTransportType.value = type
                _connectionState.value = ConnectionState.Connected(type, detail)
                SoundFeedback.playConnect()
                EmbedWidgetProvider.updateAllWidgets(appContext)
                scope.launch {
                    _detectedProfile.value = detectFirmwareProfile(transport)
                    setSubGhzFrequency(_subGhzFrequencyMhz.value)
                    refreshSystemInfo()
                }
            },
            onFailure = { error ->
                activeTransport = null
                _detectedProfile.value = FirmwareProfile.UNKNOWN
                _connectionState.value = ConnectionState.Error(error.message ?: "Error de conexión.")
                SoundFeedback.playError()
                EmbedWidgetProvider.updateAllWidgets(appContext)
            }
        )
        return result
    }

    suspend fun disconnect() {
        activeTransport?.disconnect()
        activeTransport = null
        _detectedProfile.value = FirmwareProfile.UNKNOWN
        _connectionState.value = ConnectionState.Disconnected
        SoundFeedback.playDisconnect()
        EmbedWidgetProvider.updateAllWidgets(appContext)
    }

    fun isUsbActive(): Boolean =
        activeTransport is UsbTransport && _connectionState.value is ConnectionState.Connected

    /**
     * Sube texto a SD/LittleFS vía Serial (`storage write` + EOF). Requiere USB.
     */
    suspend fun writeTextFileToDevice(relativePath: String, content: String): Result<String> {
        if (BuildConfig.ENABLE_MOCK_TRANSPORT && appPreferences?.useMockTransport == true &&
            activeTransport is MockTransport
        ) {
            BruceDebugLog.appendOutgoing("storage write $relativePath (mock)")
            return Result.success(BruceCommands.sanitizeDeviceRelativePath(relativePath))
        }
        val usb = activeTransport as? UsbTransport
            ?: return Result.failure(Exception("Push de archivo solo por USB OTG (no WiFi/BLE)."))
        return try {
            withTimeout(30_000L) {
                usb.writeTextFile(relativePath, content).also { result ->
                    result.onSuccess {
                        BruceDebugLog.appendOutgoing("storage write $it OK")
                        _events.tryEmit(BruceEvent.RawLine("[PUSH] $it"))
                    }
                }
            }
        } catch (_: TimeoutCancellationException) {
            Result.failure(Exception("Timeout subiendo archivo al T-Embed."))
        }
    }

    suspend fun sendTehLinkRaw(json: String): Result<String> {
        val transport = activeTransport
            ?: return Result.failure(Exception("Sin transporte activo. Conecta USB, WiFi o BLE."))

        return try {
            withTimeout(5_000L) {
                tehLinkClient.sendRawJson(transport, json).onSuccess { response ->
                    BruceDebugLog.appendOutgoing(json.trim())
                    _events.tryEmit(BruceEvent.RawLine(response))
                }
            }
        } catch (_: TimeoutCancellationException) {
            Result.failure(Exception("Timeout TEH-Link: sin respuesta en 5s."))
        }
    }

    suspend fun sendCommand(command: String): Result<String> {
        val validated = BruceCommandValidator.validate(command).getOrElse {
            return Result.failure(it)
        }

        val transport = activeTransport
            ?: return Result.failure(Exception("Sin transporte activo. Conecta USB, WiFi o BLE."))

        return try {
            withTimeout(5_000L) {
                val result = transport.sendCommand(validated)
                result.onSuccess {
                    BruceDebugLog.appendOutgoing(validated)
                    _events.tryEmit(BruceEvent.RawLine("> $validated"))
                }
                result
            }
        } catch (_: TimeoutCancellationException) {
            Result.failure(Exception("Timeout: T-Embed no respondió en 5s."))
        }
    }

    suspend fun sendCommandAndCollect(command: String, waitMs: Long = 5000L): Result<List<String>> {
        val validated = BruceCommandValidator.validate(command).getOrElse {
            return Result.failure(it)
        }

        val transport = activeTransport
            ?: return Result.failure(Exception("Sin transporte activo. Conecta USB, WiFi o BLE."))

        val collected = mutableListOf<String>()
        var collectorJob: Job? = null
        return try {
            collectorJob = scope.launch {
                _incomingRaw.collect { collected.add(it) }
            }
            delay(100)
            collected.clear()
            BruceDebugLog.appendOutgoing(validated)
            val sent = transport.sendCommand(validated)
            if (sent.isFailure) return sent.map { emptyList() }
            delay(waitMs)
            Result.success(collected.toList())
        } catch (_: TimeoutCancellationException) {
            Result.failure(Exception("Timeout esperando respuesta Bruce."))
        } catch (e: IOException) {
            Result.failure(Exception("Error de transporte: ${e.message}"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception("Error inesperado: ${e.message}"))
        } finally {
            collectorJob?.cancel()
        }
    }

    suspend fun refreshSystemInfo() {
        val transport = activeTransport ?: return
        if (_detectedProfile.value == FirmwareProfile.XIBALBA) {
            refreshTehLinkSystemInfo(transport)
            return
        }
        sendCommand(BruceCommands.info())
        sendCommand(BruceCommands.free())
        sendCommand(BruceCommands.uptime())
    }

    private suspend fun refreshTehLinkSystemInfo(transport: TEmbedTransport) {
        var info = _systemInfo.value
        tehLinkClient.getInfo(transport).onSuccess { device ->
            info = info.copy(
                firmware = "${device.product} v${device.version} (${device.codename})",
                codename = device.codename,
                channel = device.channel,
                profile = FirmwareProfile.XIBALBA,
                xibalbaPlugins = device.plugins
            )
            _systemInfo.value = info
            _events.tryEmit(BruceEvent.SystemInfoUpdate(info))
        }.onFailure {
            _events.tryEmit(BruceEvent.RawLine("[TEH-Link] get_info: ${it.message}"))
        }

        tehLinkClient.getStatus(transport).onSuccess { status ->
            val uptimeSec = status.uptimeMs / 1000
            val hours = uptimeSec / 3600
            val mins = (uptimeSec % 3600) / 60
            val secs = uptimeSec % 60
            info = info.copy(
                uptime = String.format("%02d:%02d:%02d", hours, mins, secs),
                uiScreen = status.uiScreen,
                sdMounted = if (status.sdMounted) "OK" else "MISSING",
                profile = FirmwareProfile.XIBALBA
            )
            _systemInfo.value = info
            _events.tryEmit(BruceEvent.SystemInfoUpdate(info))
            _events.tryEmit(BruceEvent.RawLine("[TEH-Link] UI: ${status.uiScreen}"))
        }.onFailure {
            _systemInfo.value = info
            _events.tryEmit(BruceEvent.RawLine("[TEH-Link] get_status: ${it.message}"))
        }
    }

    private suspend fun detectFirmwareProfile(transport: TEmbedTransport): FirmwareProfile {
        val pref = appPreferences?.firmwareProfile?.value ?: FirmwareProfile.AUTO
        if (pref == FirmwareProfile.BRUCE) return FirmwareProfile.BRUCE
        val pingOk = tehLinkClient.ping(transport).getOrElse { false }
        return when {
            pref == FirmwareProfile.XIBALBA && pingOk -> FirmwareProfile.XIBALBA
            pref == FirmwareProfile.XIBALBA && !pingOk -> FirmwareProfile.UNKNOWN
            pref == FirmwareProfile.AUTO && pingOk -> FirmwareProfile.XIBALBA
            pref == FirmwareProfile.AUTO && !pingOk -> FirmwareProfile.BRUCE
            else -> if (pingOk) FirmwareProfile.XIBALBA else FirmwareProfile.BRUCE
        }
    }

    suspend fun tehLinkOpenPlugin(pluginId: String): Result<TehLinkScreenInfo> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.openPlugin(transport, pluginId).onSuccess { screen ->
            _systemInfo.value = _systemInfo.value.copy(uiScreen = screen.uiScreen)
            _events.tryEmit(BruceEvent.RawLine("[TEH-Link] open_plugin: ${screen.openedPluginId.ifBlank { pluginId }} → ${screen.uiScreen}"))
        }
    }

    suspend fun tehLinkBackToMenu(): Result<TehLinkScreenInfo> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.backToMenu(transport).onSuccess { screen ->
            _systemInfo.value = _systemInfo.value.copy(uiScreen = screen.uiScreen)
            _events.tryEmit(BruceEvent.RawLine("[TEH-Link] back_to_menu → ${screen.uiScreen}"))
        }
    }

    suspend fun tehLinkListActions(): Result<List<TehLinkActionInfo>> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.listActions(transport).onSuccess { actions ->
            _events.tryEmit(BruceEvent.RawLine("[TEH-Link] list_actions: ${actions.size} acciones"))
        }
    }

    suspend fun tehLinkRunAction(
        pluginId: String,
        action: String,
        params: JSONObject = JSONObject()
    ): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.runAction(transport, pluginId, action, params).onSuccess { result ->
            _events.tryEmit(
                BruceEvent.RawLine(
                    "[TEH-Link] run_action $pluginId/$action → ${result.state.state.ifBlank { result.state.message }}"
                )
            )
        }
    }

    suspend fun tehLinkGetActionState(
        pluginId: String,
        action: String? = null
    ): Result<TehLinkActionState> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.getActionState(transport, pluginId, action)
    }

    suspend fun tehLinkRunWifiScan(seconds: Int): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.runWifiScan(transport, seconds).onSuccess { result ->
            _events.tryEmit(
                BruceEvent.RawLine(
                    "[TEH-Link] wifi_toolkit/scan_start → ${result.state.state.ifBlank { result.state.message }}"
                )
            )
        }
    }

    suspend fun tehLinkRunWardrivingStart(): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.runWardrivingStart(transport).onSuccess { result ->
            _events.tryEmit(
                BruceEvent.RawLine(
                    "[TEH-Link] wardriving/start → ${result.state.state.ifBlank { result.state.message }}"
                )
            )
        }
    }

    suspend fun tehLinkRunWardrivingStop(): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.runAction(transport, "wardriving", "stop").onSuccess { result ->
            _events.tryEmit(
                BruceEvent.RawLine(
                    "[TEH-Link] wardriving/stop → ${result.state.state.ifBlank { result.state.message }}"
                )
            )
        }
    }

    suspend fun tehLinkRunBleScan(seconds: Int): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.runBleScan(transport, seconds).onSuccess { result ->
            _events.tryEmit(
                BruceEvent.RawLine(
                    "[TEH-Link] ble_toolkit/scan_start → ${result.state.state.ifBlank { result.state.message }}"
                )
            )
        }
    }

    suspend fun tehLinkRunCryptoHash(
        input: String,
        algo: String = "sha256"
    ): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.runCryptoHash(transport, input, algo).onSuccess { result ->
            val out = result.state.crypto?.digest?.ifBlank { null }
                ?: result.state.crypto?.result
                ?: result.state.message
            _events.tryEmit(
                BruceEvent.RawLine("[TEH-Link] crypto_toolkit/hash → $out")
            )
        }
    }

    suspend fun tehLinkRunCryptoBase64Encode(input: String): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.runCryptoBase64Encode(transport, input).onSuccess { result ->
            val out = result.state.crypto?.result?.ifBlank { null } ?: result.state.message
            _events.tryEmit(
                BruceEvent.RawLine("[TEH-Link] crypto_toolkit/base64_encode → $out")
            )
        }
    }

    suspend fun tehLinkRunGenPassword(length: Int = 16): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.runGenPassword(transport, length).onSuccess { result ->
            val out = result.state.crypto?.result?.ifBlank { null } ?: result.state.message
            _events.tryEmit(
                BruceEvent.RawLine("[TEH-Link] crypto_toolkit/gen_password → $out")
            )
        }
    }

    suspend fun startSubGhzRawCapture(seconds: Int = 10) {
        _rfLive.value = RfLiveEngine.reset(_subGhzFrequencyMhz.value)
        setSubGhzFrequency(_subGhzFrequencyMhz.value)
        sendCommand(BruceCommands.subGhzRxRaw(seconds))
    }

    /** Spectrum/waterfall se alimentan del stream de `rx raw` (no hay `subghz scan` en wiki). */
    suspend fun startSubGhzSpectrumScan() {
        _rfLive.value = RfLiveEngine.reset(_subGhzFrequencyMhz.value)
        setSubGhzFrequency(_subGhzFrequencyMhz.value)
        sendCommand(BruceCommands.subGhzRxRaw(20))
    }

    /**
     * Bruce no documenta stop explícito; la RX termina al acabar los segundos.
     * Solo resetea estado local de live UI.
     */
    suspend fun stopSubGhzCapture() {
        _rfLive.value = RfLiveEngine.reset(_subGhzFrequencyMhz.value)
    }

    suspend fun uploadFirmwareOta(binFile: File, onProgress: (Int) -> Unit): Result<String> {
        if (_activeTransportType.value != TransportType.WIFI) {
            return Result.failure(Exception("OTA requiere conexión WiFi al T-Embed (BruceNet)."))
        }
        return wifiTransport.uploadFirmware(binFile, onProgress)
    }

    private fun handleIncomingLine(line: String) {
        if (TehLinkResponseParser.isTehLinkLine(line)) {
            _incomingRaw.tryEmit(line)
            BruceDebugLog.appendIncoming(line)
            return
        }

        _incomingRaw.tryEmit(line)
        BruceDebugLog.appendIncoming(line)

        _rfLive.value = RfLiveEngine.feed(_rfLive.value, line, _subGhzFrequencyMhz.value)

        RfProtocolDecoder.decode(line)?.let { decoded ->
            _lastDecoded.value = RfProtocolDecoder.formatDecoded(decoded)
        }

        val event = BruceResponseParser.parseLine(line)
        _events.tryEmit(event)

        when (event) {
            is BruceEvent.SubGhzSignal -> {
                _signalLog.value = _signalLog.value + event.entry
                SoundFeedback.playCapture()
                WidgetStateStore.updateLastSignal(
                    appContext,
                    event.entry.protocol,
                    event.entry.frequency
                )
                scope.launch {
                    val repo = signalRepository ?: return@launch
                    val (lat, lng) = locationTracker?.currentLatLng() ?: (null to null)
                    val decoded = RfProtocolDecoder.decode(line)
                    val signalId = repo.saveSubGhzSignal(event.entry, lat, lng, decoded)
                    _events.tryEmit(BruceEvent.SubGhzSignalSaved(event.entry, signalId))
                    sessionStats?.incrementSignals()
                }
            }
            is BruceEvent.SystemInfoUpdate -> {
                _systemInfo.value = mergeSystemInfo(_systemInfo.value, event.info)
            }
            is BruceEvent.RawLine -> {
                scope.launch {
                    val (lat, lng) = locationTracker?.currentLatLng() ?: (null to null)
                    if (RfProtocolDecoder.decode(line) != null) {
                        signalRepository?.saveFromDecodedLine(line, lat, lng)
                    }
                }
            }
            else -> Unit
        }
    }

    private fun mergeSystemInfo(current: SystemInfo, update: SystemInfo): SystemInfo {
        return SystemInfo(
            uptime = update.uptime.ifBlank { current.uptime },
            freeHeap = update.freeHeap.ifBlank { current.freeHeap },
            battery = update.battery.ifBlank { current.battery },
            firmware = update.firmware.ifBlank { current.firmware },
            codename = update.codename.ifBlank { current.codename },
            channel = update.channel.ifBlank { current.channel },
            uiScreen = update.uiScreen.ifBlank { current.uiScreen },
            sdMounted = update.sdMounted.ifBlank { current.sdMounted },
            profile = if (update.profile != FirmwareProfile.UNKNOWN) update.profile else current.profile
        )
    }
}
