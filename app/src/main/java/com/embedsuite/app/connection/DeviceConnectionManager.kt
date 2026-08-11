package com.embedsuite.app.connection

import android.content.Context
import android.hardware.usb.UsbDevice
import com.embedsuite.app.UsbSerialManager
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.core.AppPreferences
import com.embedsuite.app.core.SoundFeedback
import com.embedsuite.app.core.connection.PendingCommandQueue
import com.embedsuite.app.core.connection.ReconnectPolicy
import com.embedsuite.app.core.device.DeviceProfile
import com.embedsuite.app.core.device.DeviceProfileResolver
import com.embedsuite.app.core.device.DeviceProfileStore
import com.embedsuite.app.security.SecureStore
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
import kotlin.coroutines.cancellation.CancellationException
import java.io.File
import com.embedsuite.app.rf.RfLineParser
import com.embedsuite.app.BuildConfig
import org.json.JSONObject

class DeviceConnectionManager(
    private val usbSerialManager: UsbSerialManager,
    context: Context,
    private val signalRepository: SignalRepository? = null,
    private val locationTracker: LocationTracker? = null,
    private val appPreferences: AppPreferences? = null,
    private val secureStore: SecureStore? = null,
    private val sessionStats: com.embedsuite.app.core.SessionStatsTracker? = null
) {
    private val appContext = context.applicationContext
    val pendingCommandQueue = PendingCommandQueue()

    fun applicationContext(): Context = appContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val signalLogDeque = ArrayDeque<SignalEntry>(501)

    private val usbTransport = UsbTransport(usbSerialManager)
    private val wifiTransport = WifiTransport()
    private val bleTransport = BleTransport(context)
    private val mockTransport = MockTransport()

    private val tehLinkClient = TehLinkClient(scope)
    private val tehLinkOtaUploader = TehLinkOtaUploader(tehLinkClient)
    private val deviceProfileStore = DeviceProfileStore(appContext)

    private var activeTransport: TEmbedTransport? = null

    private val _detectedProfile = MutableStateFlow(FirmwareProfile.UNKNOWN)
    val detectedProfile: StateFlow<FirmwareProfile> = _detectedProfile.asStateFlow()

    private val _activeDeviceProfile = MutableStateFlow<DeviceProfile?>(deviceProfileStore.getActive())
    val activeDeviceProfile: StateFlow<DeviceProfile?> = _activeDeviceProfile.asStateFlow()
    val savedDeviceProfiles: List<DeviceProfile> get() = deviceProfileStore.list()

    fun setActiveDeviceProfile(id: String) {
        deviceProfileStore.setActive(id)
        _activeDeviceProfile.value = deviceProfileStore.getActive()
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _activeTransportType = MutableStateFlow(TransportType.USB)
    val activeTransportType: StateFlow<TransportType> = _activeTransportType.asStateFlow()

    private val _events = MutableSharedFlow<DeviceEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<DeviceEvent> = _events.asSharedFlow()

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

    @Volatile
    private var lastScanSamplePersistMs = 0L

    val mappedSignals: Flow<List<CapturedSignalEntity>> =
        signalRepository?.mappedSignals ?: flowOf(emptyList())

    val bleTransportRef: BleTransport get() = bleTransport

    init {
        tehLinkClient.authToken = secureStore?.getTehLinkAuthToken().orEmpty()
        scope.launch { usbTransport.incomingLines().collect { handleIncomingLine(it) } }
        scope.launch { wifiTransport.incomingLines().collect { handleIncomingLine(it) } }
        scope.launch { bleTransport.incomingLines().collect { handleIncomingLine(it) } }
        scope.launch { mockTransport.incomingLines().collect { handleIncomingLine(it) } }
        observeReconnectPolicy()
        observeTransportPreferenceChanges()
    }

    private var reconnectAttempt = 0

    private fun observeReconnectPolicy() {
        val prefs = appPreferences ?: return
        scope.launch {
            combine(prefs.autoReconnect, _connectionState) { autoReconnect, state ->
                autoReconnect to state
            }.collect { (autoReconnect, state) ->
                if (autoReconnect && state is ConnectionState.Disconnected) {
                    reconnectAttempt++
                    delay(ReconnectPolicy.delayMs(reconnectAttempt))
                    if (_connectionState.value is ConnectionState.Disconnected) {
                        reconnectPreferringUsb(prefs.defaultTransport.value)
                    }
                } else if (state is ConnectionState.Connected) {
                    reconnectAttempt = 0
                    flushPendingCommands()
                }
            }
        }
    }

    private fun flushPendingCommands() {
        scope.launch {
            val queued = pendingCommandQueue.drain()
            for (json in queued) {
                sendTehLinkRaw(json)
            }
        }
    }

    /**
     * Reconexión robusta para uso diario: USB OTG tiene prioridad absoluta.
     * WiFi/BLE solo como fallback si el usuario los eligió y USB no está disponible.
     */
    private suspend fun reconnectPreferringUsb(preferred: TransportType) {
        val usb = connect(TransportType.USB)
        if (usb.isSuccess) return
        if (preferred != TransportType.USB &&
            _connectionState.value is ConnectionState.Disconnected
        ) {
            connect(preferred)
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
        return Result.success(FREQ_LOCAL_HINT)
    }

    suspend fun connect(type: TransportType): Result<String> {
        _connectionState.value = ConnectionState.Connecting
        activeTransport?.disconnect()

        val transport = when (type) {
            TransportType.USB -> {
                val usbResult = requestUsbPermissionAndConnect()
                if (usbResult.isFailure &&
                    usbResult.exceptionOrNull()?.message?.contains("permiso", ignoreCase = true) == true
                ) {
                    _connectionState.value = ConnectionState.Disconnected
                }
                return usbResult
            }
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
                    if (_detectedProfile.value == FirmwareProfile.XIBALBA) {
                        ensureTehLinkAuth(transport)
                        syncTimeWithDevice()
                    }
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

    /** Conecta por USB usando un dispositivo concreto (p. ej. tras conceder permiso OTG). */
    suspend fun connectUsbDevice(device: UsbDevice): Result<String> {
        _connectionState.value = ConnectionState.Connecting
        activeTransport?.disconnect()

        val transport = usbTransport
        val result = transport.connect(device)
        result.fold(
            onSuccess = { detail ->
                activeTransport = transport
                _activeTransportType.value = TransportType.USB
                _connectionState.value = ConnectionState.Connected(TransportType.USB, detail)
                SoundFeedback.playConnect()
                EmbedWidgetProvider.updateAllWidgets(appContext)
                scope.launch {
                    _detectedProfile.value = detectFirmwareProfile(transport)
                    if (_detectedProfile.value == FirmwareProfile.XIBALBA) {
                        ensureTehLinkAuth(transport)
                        syncTimeWithDevice()
                    }
                    setSubGhzFrequency(_subGhzFrequencyMhz.value)
                    refreshSystemInfo()
                }
            },
            onFailure = { error ->
                activeTransport = null
                _detectedProfile.value = FirmwareProfile.UNKNOWN
                _connectionState.value = ConnectionState.Error(error.message ?: "Error de conexión USB.")
                SoundFeedback.playError()
                EmbedWidgetProvider.updateAllWidgets(appContext)
            }
        )
        return result
    }

    suspend fun requestUsbPermissionAndConnect(): Result<String> {
        val device = usbSerialManager.mejorDispositivo()
            ?: return Result.failure(Exception("No hay T-Embed USB conectado por OTG."))
        if (!usbSerialManager.tienePermiso(device)) {
            usbSerialManager.solicitarPermiso(device)
            return Result.failure(
                Exception("Acepta el permiso USB JTAG/Serial en el diálogo de Android.")
            )
        }
        return connectUsbDevice(device)
    }

    suspend fun disconnect() {
        activeTransport?.disconnect()
        activeTransport = null
        _detectedProfile.value = FirmwareProfile.UNKNOWN
        _connectionState.value = ConnectionState.Disconnected
        SoundFeedback.playDisconnect()
        EmbedWidgetProvider.updateAllWidgets(appContext)
    }

    /**
     * Libera el puerto CDC TEH-Link antes de esptool ROM.
     * Intenta reboot a bootloader si Xibalba está conectado por USB; luego desconecta.
     */
    suspend fun prepareForUsbFlash(): Result<Unit> {
        val transport = activeTransport
        if (transport != null &&
            _detectedProfile.value == FirmwareProfile.XIBALBA &&
            _activeTransportType.value == TransportType.USB
        ) {
            runCatching {
                ensureTehLinkAuth(transport)
                tehLinkClient.runAction(transport, "device", "reboot_bootloader")
            }
            delay(1200)
        }
        disconnect()
        delay(600)
        return Result.success(Unit)
    }

    /** Reconecta TEH-Link tras flasheo USB (best-effort). */
    suspend fun reconnectAfterUsbFlash() {
        delay(2500)
        connect(TransportType.USB)
    }

    fun isUsbActive(): Boolean =
        activeTransport is UsbTransport && _connectionState.value is ConnectionState.Connected

    suspend fun sendTehLinkRaw(json: String): Result<String> {
        TehLinkCommandPolicy.validateConsoleRequest(json).getOrElse {
            return Result.failure(it)
        }
        return executeTehLinkJson(json)
    }

    /**
     * Envío TEH-Link para motores internos (workflows, bruce sync, autopilot).
     * No aplica la ACL de consola manual.
     */
    suspend fun executeTehLinkJson(json: String): Result<String> {
        ensureXibalbaProfile()
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        val transport = activeTransport
        if (transport == null || _connectionState.value !is ConnectionState.Connected) {
            pendingCommandQueue.enqueue(json)
            return Result.failure(Exception("Sin transporte activo — comando en cola (${pendingCommandQueue.size})."))
        }

        return try {
            withTimeout(320_000L) {
                ensureTehLinkAuth(transport)
                sendTehLinkRawOnce(transport, json)
            }
        } catch (_: TimeoutCancellationException) {
            Result.failure(Exception("Timeout TEH-Link: sin respuesta."))
        }
    }

    /** Si hay transporte pero el perfil quedó UNKNOWN (p. ej. tras Autopilot/reconnect), re-detecta con ping. */
    private suspend fun ensureXibalbaProfile() {
        if (_detectedProfile.value == FirmwareProfile.XIBALBA) return
        val transport = activeTransport ?: return
        if (_connectionState.value !is ConnectionState.Connected) return
        val profile = detectFirmwareProfile(transport)
        _detectedProfile.value = profile
        if (profile == FirmwareProfile.XIBALBA) {
            _events.tryEmit(DeviceEvent.RawLine("[perfil] re-detectado XIBALBA vía ping"))
        }
    }

    private suspend fun sendTehLinkRawOnce(transport: TEmbedTransport, json: String): Result<String> {
        val first = tehLinkClient.sendRawJson(transport, json)
        if (first.isSuccess) {
            return first.onSuccess { response ->
                LinkDebugLog.appendOutgoing(TehLinkResponseParser.redactSensitiveRequest(json.trim()))
                val safe = TehLinkResponseParser.redactSensitiveResponse(response)
                _events.tryEmit(DeviceEvent.RawLine(safe))
            }
        }
        if (isAuthError(first.exceptionOrNull()?.message)) {
            clearTehLinkAuth()
            ensureTehLinkAuth(transport)
            return tehLinkClient.sendRawJson(transport, json).onSuccess { response ->
                LinkDebugLog.appendOutgoing(TehLinkResponseParser.redactSensitiveRequest(json.trim()))
                val safe = TehLinkResponseParser.redactSensitiveResponse(response)
                _events.tryEmit(DeviceEvent.RawLine(safe))
            }
        }
        return first
    }

    suspend fun refreshSystemInfo() {
        val transport = activeTransport ?: return
        refreshTehLinkSystemInfo(transport)
    }

    suspend fun clearCoredump(): Result<Boolean> {
        val transport = activeTransport ?: return Result.failure(IllegalStateException("Not connected"))
        val r = tehLinkClient.clearCoredump(transport)
        if (r.isSuccess) refreshTehLinkSystemInfo(transport)
        return r
    }

    suspend fun runSoakStress(iterations: Int, perStepSeconds: Int): Result<TehLinkSoakResult> {
        val transport = activeTransport ?: return Result.failure(IllegalStateException("Not connected"))
        val r = tehLinkClient.runSoakStress(transport, iterations, perStepSeconds)
        if (r.isSuccess) refreshTehLinkSystemInfo(transport)
        return r
    }

    private suspend fun refreshTehLinkSystemInfo(transport: TEmbedTransport) {
        var info = _systemInfo.value
        tehLinkClient.getInfo(transport).onSuccess { device ->
            info = info.copy(
                firmware = "${device.product} v${device.version} (${device.codename})",
                codename = device.codename,
                channel = device.channel,
                profile = FirmwareProfile.XIBALBA,
                xibalbaPlugins = device.plugins,
                hardening = device.hardening
            )
            _systemInfo.value = info
            _events.tryEmit(DeviceEvent.SystemInfoUpdate(info))

            val profile = DeviceProfileResolver.resolve(device)
            deviceProfileStore.upsert(profile)
            deviceProfileStore.setActive(profile.id)
            _activeDeviceProfile.value = profile
            _events.tryEmit(
                DeviceEvent.RawLine(
                    "[perfil] ${profile.name} · ${profile.hardwareKind} · caps=${profile.capabilities.size}"
                )
            )

            /* Alertas tempranas (no bloqueantes) cuando hay pánico previo o flags de seguridad desactivados. */
            if (info.wdtPanicReason != null) {
                _events.tryEmit(
                    DeviceEvent.TehLinkNotice(
                        "⚠️ Detectado RESET por Watchdog: ${info.wdtPanicReason}. " +
                            "Revisa Tarea Diagnósticos > Coredump."
                    )
                )
            }
            if (info.hardening.run { !twdtEnabled || !bodEnabled || !secureBoot || !flashEncryption || !nvsEncryption }) {
                _events.tryEmit(
                    DeviceEvent.TehLinkNotice(
                        "🛡 Hardening incompleto detectado. Abre Ajustes > Seguridad para ver flags."
                    )
                )
            }
        }.onFailure {
            _events.tryEmit(DeviceEvent.RawLine("[TEH-Link] get_info: ${it.message}"))
        }

        tehLinkClient.getStatus(transport).onSuccess { status ->
            val uptimeSec = status.uptimeMs / 1000
            val hours = uptimeSec / 3600
            val mins = (uptimeSec % 3600) / 60
            val secs = uptimeSec % 60
            val heapLine = buildString {
                status.heapFreeBytes?.let { append("${it / 1024} KB DRAM") }
                status.psramFreeBytes?.let {
                    if (this.isNotEmpty()) append(" · ")
                    append("${it / 1024} KB PSRAM")
                }
            }
            info = info.copy(
                uptime = String.format("%02d:%02d:%02d", hours, mins, secs),
                uiScreen = status.uiScreen,
                sdMounted = if (status.sdMounted) "OK" else "MISSING",
                profile = FirmwareProfile.XIBALBA,
                simFlags = status.sim,
                xibalbaCapabilities = status.capabilities,
                battery = formatBatteryLine(status) ?: info.battery,
                freeHeap = heapLine,
                freeHeapBytes = status.heapFreeBytes,
                freePsramBytes = status.psramFreeBytes,
                coredumpPending = status.coredumpPresent,
                wdtPanicReason = status.wdtPanicReason
            )
            _systemInfo.value = info
            _events.tryEmit(DeviceEvent.SystemInfoUpdate(info))
            _events.tryEmit(DeviceEvent.RawLine("[TEH-Link] UI: ${status.uiScreen}"))
        }.onFailure {
            _systemInfo.value = info
            _events.tryEmit(DeviceEvent.RawLine("[TEH-Link] get_status: ${it.message}"))
        }
    }

    private suspend fun detectFirmwareProfile(transport: TEmbedTransport): FirmwareProfile {
        val pingOk = tehLinkClient.ping(transport).getOrElse { false }
        return if (pingOk) FirmwareProfile.XIBALBA else FirmwareProfile.UNKNOWN
    }

    suspend fun tehLinkOpenPlugin(pluginId: String): Result<TehLinkScreenInfo> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.openPlugin(transport, pluginId).onSuccess { screen ->
            _systemInfo.value = _systemInfo.value.copy(uiScreen = screen.uiScreen)
            _events.tryEmit(DeviceEvent.RawLine("[TEH-Link] open_plugin: ${screen.openedPluginId.ifBlank { pluginId }} → ${screen.uiScreen}"))
        }
    }

    suspend fun tehLinkBackToMenu(): Result<TehLinkScreenInfo> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.backToMenu(transport).onSuccess { screen ->
            _systemInfo.value = _systemInfo.value.copy(uiScreen = screen.uiScreen)
            _events.tryEmit(DeviceEvent.RawLine("[TEH-Link] back_to_menu → ${screen.uiScreen}"))
        }
    }

    suspend fun tehLinkListActions(): Result<List<TehLinkActionInfo>> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.listActions(transport).onSuccess { actions ->
            _events.tryEmit(DeviceEvent.RawLine("[TEH-Link] list_actions: ${actions.size} acciones"))
        }
    }

    suspend fun tehLinkRunAction(
        pluginId: String,
        action: String,
        params: JSONObject = JSONObject()
    ): Result<TehLinkActionResult> {
        TehLinkActionPolicy.validate(pluginId, action).getOrElse {
            return Result.failure(it)
        }
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        ensureXibalbaProfile()
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        ensureTehLinkAuth(transport)
        return tehLinkClient.runAction(transport, pluginId, action, params).onSuccess { result ->
            _events.tryEmit(
                DeviceEvent.RawLine(
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
        return tehLinkRunAction(
            pluginId = "wifi_toolkit",
            action = "scan_start",
            params = JSONObject().put("seconds", seconds.coerceIn(1, 120))
        )
    }

    suspend fun tehLinkRunWardrivingStart(
        latitude: Double? = null,
        longitude: Double? = null,
        altitudeM: Double? = null
    ): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        locationTracker?.startTracking()
        val lat = latitude ?: locationTracker?.location?.value?.latitude
        val lon = longitude ?: locationTracker?.location?.value?.longitude
        val alt = altitudeM ?: locationTracker?.location?.value?.altitude
        return tehLinkClient.runWardrivingStart(transport, lat, lon, alt).onSuccess { result ->
            _events.tryEmit(
                DeviceEvent.RawLine(
                    "[TEH-Link] wardriving/start → ${result.state.state.ifBlank { result.state.message }}"
                )
            )
        }
    }

    suspend fun tehLinkRunWardrivingGpsUpdate(): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        val loc = locationTracker?.location?.value
            ?: return Result.failure(Exception("GPS del teléfono no disponible."))
        return tehLinkClient.runWardrivingGpsUpdate(
            transport,
            latitude = loc.latitude,
            longitude = loc.longitude,
            altitudeM = loc.altitude.toDouble()
        )
    }

    suspend fun tehLinkRunNfcRead(): Result<TehLinkActionResult> {
        return tehLinkRunAction(pluginId = "nfc_toolkit", action = "read")
    }

    suspend fun tehLinkRunIrSend(protocol: String, address: String, command: String): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.runIrSend(transport, protocol, address, command).onSuccess { result ->
            _events.tryEmit(
                DeviceEvent.RawLine(
                    "[TEH-Link] ir_toolkit/send → ${result.state.message.ifBlank { result.state.state }}"
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
            locationTracker?.stopTracking()
            _events.tryEmit(
                DeviceEvent.RawLine(
                    "[TEH-Link] wardriving/stop → ${result.state.state.ifBlank { result.state.message }}"
                )
            )
        }
    }

    fun hasXibalbaCapability(key: String): Boolean {
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) return false
        val caps = _systemInfo.value.xibalbaCapabilities
        // Explicit firmware capability wins — including false (sim / missing HW).
        if (caps.containsKey(key)) return caps[key] == true
        val pluginId = when (key) {
            "nfc" -> "nfc_toolkit"
            "ir" -> "ir_toolkit"
            "subghz_tx" -> "subghz_analyzer"
            "ir_rx" -> "ir_toolkit"
            "nrf24" -> "nrf24_toolkit"
            "badusb_hid" -> "badusb"
            "charger" -> "charger"
            "fuel_gauge" -> "fuel_gauge"
            else -> key
        }
        return _systemInfo.value.xibalbaPlugins.any { it.id == pluginId }
    }

    private fun formatBatteryLine(status: TehLinkDeviceStatus): String? {
        val pct = status.batteryPct?.let { "$it%" }
        val charge = when {
            status.charging == true -> status.chargeStatus?.ifBlank { "charging" } ?: "charging"
            status.vbusPresent == true -> status.chargeStatus ?: "usb"
            !status.chargeStatus.isNullOrBlank() -> status.chargeStatus
            else -> null
        }
        return when {
            pct != null && charge != null -> "$pct · $charge"
            pct != null -> pct
            charge != null -> charge
            else -> null
        }
    }

    suspend fun tehLinkRunSubGhzTx(rawHex: String, freqMhz: Double? = null): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("Sub-GHz TX TEH-Link solo en Xibalba."))
        }
        return tehLinkClient.runSubGhzTx(transport, rawHex, freqMhz).onSuccess { result ->
            _events.tryEmit(
                DeviceEvent.RawLine(
                    "[TEH-Link] subghz_tx → ${result.state.message.ifBlank { result.state.state }}"
                )
            )
        }
    }

    suspend fun tehLinkRunSubGhzReplay(devicePath: String): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("Sub-GHz replay TEH-Link solo en Xibalba."))
        }
        return tehLinkClient.runSubGhzReplay(transport, devicePath).onSuccess { result ->
            _events.tryEmit(
                DeviceEvent.RawLine(
                    "[TEH-Link] subghz_replay → ${result.state.message.ifBlank { result.state.state }}"
                )
            )
        }
    }

    suspend fun tehLinkRunIrRx(seconds: Int = 10): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("IR RX TEH-Link solo en Xibalba."))
        }
        return tehLinkClient.runIrRxStart(transport, seconds)
    }

    suspend fun tehLinkRunNfcEmulate(uid: String): Result<TehLinkActionResult> {
        return tehLinkRunAction(
            pluginId = "nfc_toolkit",
            action = "emulate",
            params = JSONObject().put("uid", uid)
        )
    }

    suspend fun tehLinkRunBleScan(seconds: Int): Result<TehLinkActionResult> {
        return tehLinkRunAction(
            pluginId = "ble_toolkit",
            action = "scan_start",
            params = JSONObject().put("seconds", seconds.coerceIn(1, 120))
        )
    }

    // ===== HW bridge flat cmds (teh_hw.cpp) =====

    suspend fun syncTimeWithDevice(timestampNs: Long = System.nanoTime()): Result<JSONObject> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        return tehLinkClient.syncTime(transport, timestampNs).onSuccess { data ->
            _events.tryEmit(
                DeviceEvent.RawLine(
                    "[TEH-Link] time.sync ok offset_ns=${data.optLong("offset_ns")}"
                )
            )
        }.onFailure {
            _events.tryEmit(DeviceEvent.RawLine("[TEH-Link] time.sync: ${it.message}"))
        }
    }

    suspend fun deviceAudioBeep(freqHz: Int = 1000, durationMs: Int = 100): Result<JSONObject> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        return tehLinkClient.audioBeep(transport, freqHz, durationMs)
    }

    suspend fun sdCardStatus(): Result<JSONObject> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        return tehLinkClient.sdStatus(transport)
    }

    suspend fun sdCardList(path: String = "/xibalba_sessions"): Result<List<String>> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        return tehLinkClient.sdList(transport, path).map { data ->
            val files = data.optJSONArray("files") ?: return@map emptyList()
            buildList {
                for (i in 0 until files.length()) {
                    val f = files.optJSONObject(i) ?: continue
                    val name = f.optString("name")
                    if (name.isNotBlank()) {
                        val marker = if (f.optBoolean("dir")) "[D] " else ""
                        add("$marker$name")
                    }
                }
            }
        }
    }

    /** Escribe en microSD del dispositivo (chunks de ≤3500 bytes UTF-8). */
    suspend fun sdCardSave(filename: String, content: String): Result<Int> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        val chunkSize = 3_000
        var offset = 0
        var total = 0
        var first = true
        while (offset < content.length) {
            val end = (offset + chunkSize).coerceAtMost(content.length)
            val chunk = content.substring(offset, end)
            val result = tehLinkClient.sdSave(transport, filename, chunk, append = !first)
            if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
            total += result.getOrThrow().optInt("bytes")
            first = false
            offset = end
        }
        return Result.success(total)
    }

    suspend fun rfScanStart(
        freqStart: Double,
        freqEnd: Double,
        step: Double = 0.25,
        rssiThreshold: Int = -100,
        dwellMs: Int = 5,
        maxHz: Int = 25
    ): Result<JSONObject> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        // GPS híbrido: Android aporta posición; time.sync ya alinea el reloj del T-Embed
        runCatching { locationTracker?.startTracking() }
        scope.launch { syncTimeWithDevice() }
        return tehLinkClient.rfScanStart(
            transport, freqStart, freqEnd, step, rssiThreshold, dwellMs, maxHz
        ).onSuccess {
            _events.tryEmit(DeviceEvent.RawLine("[TEH-Link] rf.scan.start ${freqStart}-${freqEnd}"))
        }
    }

    suspend fun rfScanStop(): Result<JSONObject> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        return tehLinkClient.rfScanStop(transport)
    }

    suspend fun rfJammerStart(
        freqMhz: Double,
        power: Int = 10,
        mode: String = "continuous",
        burstInterval: Int? = null,
        maxSeconds: Int = 30
    ): Result<JSONObject> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        return tehLinkClient.rfJammerStart(
            transport, freqMhz, power, mode, burstInterval, maxSeconds
        ).onSuccess {
            _events.tryEmit(
                DeviceEvent.RawLine(
                    "[TEH-Link] rf.jammer.start ${freqMhz}MHz mode=$mode (max ${maxSeconds}s)"
                )
            )
            scope.launch { deviceAudioBeep(600, 80) }
        }
    }

    suspend fun rfJammerStop(): Result<JSONObject> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        return tehLinkClient.rfJammerStop(transport).onSuccess {
            scope.launch { deviceAudioBeep(400, 60) }
        }
    }

    suspend fun rfJammerStatus(): Result<JSONObject> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        return tehLinkClient.rfJammerStatus(transport)
    }

    // ===== XIBALBA v0.19+ (Evil Portal API) =====
    suspend fun tehLinkRunEvilPortalStart(
        ssid: String,
        templateId: String = "generic",
        channel: Int = 6
    ): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("Evil Portal solo disponible con firmware Xibalba."))
        }
        val params = JSONObject()
            .put("ssid", ssid)
            .put("template_id", templateId)
            .put("channel", channel.coerceIn(1, 11))
        return tehLinkClient.runAction(transport, "evil_portal", "start", params).onSuccess { result ->
            _events.tryEmit(
                DeviceEvent.RawLine(
                    "[TEH-Link] evil_portal/start → ${result.state.message.ifBlank { result.state.state }}"
                )
            )
        }
    }

    suspend fun tehLinkRunEvilPortalStop(): Result<TehLinkActionResult> {
        return tehLinkRunAction(pluginId = "evil_portal", action = "stop")
    }

    suspend fun tehLinkRunEvilPortalCreds(): Result<TehLinkActionResult> {
        return tehLinkRunAction(pluginId = "evil_portal", action = "creds")
    }

    suspend fun tehLinkRunEvilPortalClearCreds(): Result<TehLinkActionResult> {
        return tehLinkRunAction(pluginId = "evil_portal", action = "clear_creds")
    }

    suspend fun tehLinkRunEvilPortalStatus(): Result<TehLinkActionResult> {
        return tehLinkRunAction(pluginId = "evil_portal", action = "status")
    }

    // ===== XIBALBA v0.19+ (Beacon Spam API) =====
    suspend fun tehLinkRunBeaconSpamStart(
        spec: String = "random:50",
        hz: Int = 10,
        channel: Int = 0
    ): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("Beacon Spam solo disponible con firmware Xibalba."))
        }
        val params = JSONObject()
            .put("spec", spec)
            .put("hz", hz.coerceIn(1, 100))
            .put("channel", channel.coerceIn(0, 11))
        return tehLinkClient.runAction(transport, "beacon_spam", "start", params).onSuccess { result ->
            _events.tryEmit(
                DeviceEvent.RawLine(
                    "[TEH-Link] beacon_spam/start → ${result.state.message.ifBlank { result.state.state }}"
                )
            )
        }
    }

    suspend fun tehLinkRunBeaconSpamStop(): Result<TehLinkActionResult> {
        return tehLinkRunAction(pluginId = "beacon_spam", action = "stop")
    }

    suspend fun tehLinkRunBeaconSpamStatus(): Result<TehLinkActionResult> {
        return tehLinkRunAction(pluginId = "beacon_spam", action = "status")
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
            _events.tryEmit(
                DeviceEvent.RawLine("[TEH-Link] crypto_toolkit/hash → [REDACTED]")
            )
        }
    }

    suspend fun tehLinkRunCryptoBase64Encode(input: String): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.runCryptoBase64Encode(transport, input).onSuccess {
            _events.tryEmit(
                DeviceEvent.RawLine("[TEH-Link] crypto_toolkit/base64_encode → [REDACTED]")
            )
        }
    }

    suspend fun tehLinkRunGenPassword(length: Int = 16): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con T-Embed Xibalba."))
        }
        return tehLinkClient.runGenPassword(transport, length).onSuccess {
            _events.tryEmit(
                DeviceEvent.RawLine("[TEH-Link] crypto_toolkit/gen_password → [REDACTED]")
            )
        }
    }

    suspend fun startSubGhzRawCapture(seconds: Int = 10): Result<String> {
        return startSubGhzTehLinkCapture(seconds).fold(
            onSuccess = { Result.success("Captura TEH-Link ${seconds}s") },
            onFailure = {
                _events.tryEmit(DeviceEvent.TehLinkNotice(it.message ?: "Captura Sub-GHz falló"))
                Result.failure(it)
            }
        )
    }

    /** Captura Sub-GHz remota vía TEH-Link (Xibalba / CC1101 Plus). */
    suspend fun startSubGhzTehLinkCapture(seconds: Int = 15): Result<TehLinkActionResult> {
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("Captura TEH-Link solo disponible con firmware Xibalba."))
        }
        val freq = _subGhzFrequencyMhz.value.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
        val params = JSONObject().put("seconds", seconds.coerceIn(1, 120))
        if (freq != null && freq > 100.0) {
            params.put("freq_mhz", freq)
        }
        return tehLinkRunAction(
            pluginId = "subghz_analyzer",
            action = "capture_start",
            params = params
        )
    }

    /**
     * Spectrum/waterfall live aún no expuesto de forma estable por TEH-Link.
     * Telemetría real de captura: usa [tehLinkGetActionState]("subghz_analyzer").
     */
    suspend fun startSubGhzSpectrumScan(): Result<String> {
        return Result.failure(
            Exception(
                "Espectro/waterfall en vivo no disponible aún vía TEH-Link. " +
                    "Usa Captura TEH-Link; el estado RX (paquetes) se actualiza vía get_action_state."
            )
        )
    }

    /** Detiene captura remota en el T-Embed y limpia UI local. */
    suspend fun stopSubGhzCapture(): Result<String> {
        _rfLive.value = RfLiveEngine.reset(_subGhzFrequencyMhz.value)
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.success("UI reset")
        }
        return tehLinkRunAction(
            pluginId = "subghz_analyzer",
            action = "capture_stop"
        ).fold(
            onSuccess = { Result.success(it.state.message.ifBlank { "Captura detenida" }) },
            onFailure = {
                // Firmware puede no exponer capture_stop; UI igual se limpia.
                Result.success("UI reset (${it.message})")
            }
        )
    }

    suspend fun pollSubGhzCaptureState(): Result<TehLinkActionState> {
        return tehLinkGetActionState("subghz_analyzer")
    }

    suspend fun uploadFirmwareOta(
        binFile: File,
        expectedSha256: String? = null,
        onProgress: (Int) -> Unit
    ): Result<String> {
        return try {
            val sha256Hex = expectedSha256?.trim()?.lowercase()
                ?: FirmwareRepository.computeFileSha256Hex(binFile)
            FirmwareRepository.verifyFileSha256(binFile, sha256Hex).getOrElse {
                return Result.failure(it)
            }

            when (_detectedProfile.value) {
                FirmwareProfile.XIBALBA -> {
                    val transport = activeTransport
                        ?: return Result.failure(Exception("Sin transporte activo para OTA."))
                    if (_activeTransportType.value != TransportType.USB &&
                        !(BuildConfig.ENABLE_MOCK_TRANSPORT && transport is MockTransport)
                    ) {
                        return Result.failure(Exception("OTA Xibalba requiere conexión USB (TEH-Link)."))
                    }
                    ensureTehLinkAuth(transport)
                    val result = tehLinkOtaUploader.upload(transport, binFile, sha256Hex, onProgress)
                        .getOrThrow()

                    /* Actualiza el systemInfo con el estado final de OTA para que el Dashboard
                     * muestre inmediatamente el tick ✅ SHA256_VERIFIED si pasó. */
                    val status = TehLinkOtaStatus(
                        state = result.otaState,
                        bytesWritten = result.totalBytes,
                        totalSize = result.totalBytes,
                        sha256Verified = result.sha256Verified
                    )
                    _systemInfo.value = _systemInfo.value.copy(lastOta = status)
                    _events.tryEmit(DeviceEvent.OtaCompleted(status))

                    if (result.sha256Verified) {
                        Result.success(
                            "OTA OK · ${result.totalBytes} B · SHA256 ✅ VERIFIED · " +
                                "REINICIA el T-Embed para aplicar."
                        )
                    } else {
                        Result.failure(
                            Exception(
                                "OTA completada pero SHA256 NO verificado " +
                                    "⚠️ NO reinicies. Flasha de nuevo vía USB con esptool."
                            )
                        )
                    }
                }
                else -> Result.failure(Exception("OTA requiere firmware Xibalba conectado por USB (TEH-Link)."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception("OTA falló: ${e.message ?: e.javaClass.simpleName}"))
        }
    }

    private suspend fun ensureTehLinkAuth(transport: TEmbedTransport) {
        val stored = secureStore?.getTehLinkAuthToken().orEmpty()
        if (stored.isNotBlank()) {
            tehLinkClient.authToken = stored
            if (tehLinkClient.getStatus(transport).isSuccess) return
            clearTehLinkAuth()
        }
        pairTehLink(transport)
    }

    private suspend fun pairTehLink(transport: TEmbedTransport) {
        _events.tryEmit(DeviceEvent.TehLinkNotice(TEH_LINK_PAIR_HINT))
        tehLinkClient.pair(transport).fold(
            onSuccess = { token ->
                tehLinkClient.authToken = token
                secureStore?.setTehLinkAuthToken(token)
                _events.tryEmit(DeviceEvent.TehLinkNotice("TEH-Link emparejado correctamente."))
            },
            onFailure = { err ->
                val msg = when {
                    err.message?.contains("pair_window", ignoreCase = true) == true ->
                        "Ventana de pairing cerrada. Mantén pulsado el botón lateral ~2 s e intenta reconectar."
                    err.message?.contains("pair_sin_token", ignoreCase = true) == true ->
                        "Pairing sin token. Long-press en el botón lateral del T-Embed e intenta de nuevo."
                    else -> "Pairing TEH-Link falló: ${err.message ?: "error desconocido"}"
                }
                _events.tryEmit(DeviceEvent.TehLinkNotice(msg))
            }
        )
    }

    private fun clearTehLinkAuth() {
        tehLinkClient.authToken = ""
        secureStore?.setTehLinkAuthToken("")
    }

    /** Borra token y ejecuta pairing de nuevo (abre ventana mock si aplica). */
    suspend fun rePairTehLink(): Result<Unit> {
        val transport = activeTransport
            ?: return Result.failure(Exception("Conecta el T-Embed antes de re-emparejar."))
        if (_detectedProfile.value != FirmwareProfile.XIBALBA) {
            return Result.failure(Exception("TEH-Link solo disponible con firmware Xibalba."))
        }
        clearTehLinkAuth()
        if (transport is MockTransport) {
            transport.openPairingWindow(120)
        }
        ensureTehLinkAuth(transport)
        return if (tehLinkClient.authToken.isNotBlank()) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("No se pudo completar el pairing TEH-Link."))
        }
    }

    fun simulateMockLongPress(): Result<Unit> {
        if (activeTransport !is MockTransport) {
            return Result.failure(Exception("Solo disponible con transporte mock."))
        }
        mockTransport.openPairingWindow(120)
        mockTransport.armBadusbRemote(120)
        _events.tryEmit(DeviceEvent.TehLinkNotice("Mock: ventana pairing + BadUSB arm (120 s)."))
        return Result.success(Unit)
    }

    fun isTehLinkPaired(): Boolean = tehLinkClient.authToken.isNotBlank()

    private fun isAuthError(message: String?): Boolean {
        if (message.isNullOrBlank()) return false
        return message.contains("auth_required", ignoreCase = true) ||
            message.contains("auth_invalid", ignoreCase = true) ||
            message.contains("unauthorized", ignoreCase = true)
    }

    private fun handleIncomingLine(line: String) {
        if (TehLinkResponseParser.isTehLinkLine(line)) {
            val safe = TehLinkResponseParser.redactSensitiveResponse(line)
            _incomingRaw.tryEmit(safe)
            LinkDebugLog.appendIncoming(safe)
            return
        }

        if (TehLinkResponseParser.isTehLinkEventLine(line)) {
            _incomingRaw.tryEmit(line)
            LinkDebugLog.appendIncoming(line)
            dispatchTehLinkEvent(line)
            return
        }

        _incomingRaw.tryEmit(line)
        LinkDebugLog.appendIncoming(line)
        _events.tryEmit(DeviceEvent.RawLine(line))

        _rfLive.value = RfLiveEngine.feed(_rfLive.value, line, _subGhzFrequencyMhz.value)

        val decoded = RfProtocolDecoder.decode(line)
        decoded?.let { _lastDecoded.value = RfProtocolDecoder.formatDecoded(it) }

        RfLineParser.parseSubGhzSignal(line)?.let { entry ->
            val event = DeviceEvent.SubGhzSignal(entry)
            _events.tryEmit(event)
            signalLogDeque.addLast(entry)
            while (signalLogDeque.size > SIGNAL_LOG_MAX) {
                signalLogDeque.removeFirst()
            }
            _signalLog.value = signalLogDeque.toList()
            SoundFeedback.playCapture()
            WidgetStateStore.updateLastSignal(
                appContext,
                entry.protocol,
                entry.frequency
            )
            scope.launch {
                val repo = signalRepository ?: return@launch
                val (lat, lng) = locationTracker?.currentLatLng() ?: (null to null)
                val signalId = repo.saveSubGhzSignal(entry, lat, lng, decoded)
                _events.tryEmit(DeviceEvent.SubGhzSignalSaved(entry, signalId))
                sessionStats?.incrementSignals()
            }
        } ?: run {
            if (decoded != null) {
                scope.launch {
                    val (lat, lng) = locationTracker?.currentLatLng() ?: (null to null)
                    signalRepository?.saveFromDecodedLine(line, lat, lng)
                }
            }
        }
    }

    private fun dispatchTehLinkEvent(line: String) {
        val root = runCatching { JSONObject(line.trim()) }.getOrNull() ?: return
        val type = root.optString("event")
        val data = root.optJSONObject("data") ?: JSONObject()
        _events.tryEmit(DeviceEvent.TehLinkAsyncEvent(type, data.toString()))
        _events.tryEmit(DeviceEvent.RawLine(line))

        when (type) {
            "rf.scan.sample", "SubGhzSample" -> {
                val freq = data.optDouble("freq_mhz", Double.NaN)
                val rssi = data.optInt("rssi", Int.MIN_VALUE)
                if (!freq.isNaN() && rssi != Int.MIN_VALUE) {
                    val (lat, lng) = locationTracker?.currentLatLng() ?: (null to null)
                    val ts = System.currentTimeMillis()
                    _events.tryEmit(DeviceEvent.SubGhzSample(freq, rssi, lat, lng, ts))
                    // Persistir muestras geotagged (throttle) para mapa / export KML
                    if (lat != null && lng != null && shouldPersistScanSample(ts)) {
                        scope.launch {
                            signalRepository?.saveRfScanSample(freq, rssi, lat, lng, ts)
                        }
                    }
                }
            }
            "rf.scan.stopped" -> {
                _events.tryEmit(
                    DeviceEvent.RfScanStopped(
                        reason = data.optString("reason", "user"),
                        sweeps = data.optLong("sweeps"),
                        samples = data.optLong("samples")
                    )
                )
            }
            "rf.jammer.stopped" -> {
                _events.tryEmit(
                    DeviceEvent.RfJammerStopped(
                        reason = data.optString("reason", "user"),
                        elapsedMs = data.optLong("elapsed_ms")
                    )
                )
            }
            "SubGhzDecodedFrame" -> {
                _events.tryEmit(
                    DeviceEvent.SubGhzDecodedFrame(
                        proto = data.optString("proto", "?"),
                        decoded = data.optString("decoded", ""),
                        rssi = data.optInt("rssi"),
                        freqMhz = data.optDouble("freq_mhz")
                    )
                )
            }
        }
    }

    companion object {
        private const val SIGNAL_LOG_MAX = 500
        private const val TEH_LINK_PAIR_HINT =
            "Para emparejar TEH-Link: mantén pulsado el botón lateral del T-Embed ~2 s (ventana 120 s), luego reconecta USB."
        private const val FREQ_LOCAL_HINT =
            "Frecuencia guardada en la app. Ajusta también la frecuencia en el menú Sub-GHz del T-Embed si hace falta."
        private const val SCAN_SAMPLE_PERSIST_MIN_MS = 750L
    }

    private fun shouldPersistScanSample(nowMs: Long): Boolean {
        if (nowMs - lastScanSamplePersistMs < SCAN_SAMPLE_PERSIST_MIN_MS) return false
        lastScanSamplePersistMs = nowMs
        return true
    }
}
