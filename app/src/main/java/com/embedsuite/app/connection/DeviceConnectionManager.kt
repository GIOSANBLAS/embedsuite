package com.embedsuite.app.connection

import android.content.Context
import android.hardware.usb.UsbDevice
import com.embedsuite.app.R
import com.embedsuite.app.UsbSerialManager
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.core.AppPreferences
import com.embedsuite.app.core.SoundFeedback
import com.embedsuite.app.core.connection.PendingCommandQueue
import com.embedsuite.app.core.connection.TransportAvailability
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
import com.embedsuite.app.notifications.EmbedNotificationHelper
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
import com.embedsuite.app.core.bruce.BruceCli
import com.embedsuite.app.core.bruce.BruceCliCaptureParser
import com.embedsuite.app.rf.RfLineParser
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

    val deviceBleTransport: BleTransport get() = bleTransport

    private val bruceLinkClient = BruceLinkClient(scope)
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

    /** Opcional: enlazar antes de conectar WiFi (AP T-Embed). */
    var wifiApManager: com.embedsuite.app.core.wifi.WifiApManager? = null

    private val _bruceLinkReady = MutableStateFlow(false)
    val bruceLinkReady: StateFlow<Boolean> = _bruceLinkReady.asStateFlow()

    private val _transportAvailability = MutableStateFlow(TransportAvailability())
    val transportAvailability: StateFlow<TransportAvailability> = _transportAvailability.asStateFlow()

    init {
        scope.launch { usbTransport.incomingLines().collect { handleIncomingLine(it) } }
        scope.launch { wifiTransport.incomingLines().collect { handleIncomingLine(it) } }
        scope.launch { bleTransport.incomingLines().collect { handleIncomingLine(it) } }
        scope.launch { mockTransport.incomingLines().collect { handleIncomingLine(it) } }
        observeReconnectPolicy()
        observeTransportPreferenceChanges()
        scope.launch {
            combine(_connectionState, _activeTransportType) { state, type ->
                state to type
            }.collect { (state, type) ->
                refreshTransportAvailability(state, type)
            }
        }
    }

    private fun refreshTransportAvailability(
        state: ConnectionState,
        active: TransportType
    ) {
        val connected = state is ConnectionState.Connected
        _transportAvailability.value = TransportAvailability(
            usbAvailable = usbSerialManager.mejorDispositivo() != null || connected,
            bleAvailable = true,
            wifiAvailable = true,
            active = if (connected) active else null
        )
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
            for (line in queued) {
                sendBruceCliLine(line)
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
            TransportType.WIFI -> {
                wifiApManager?.bindToWifiTransport()
                wifiTransport
            }
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
                EmbedNotificationHelper.notifyDeviceConnected(appContext, type.name, detail)
                scope.launch {
                    _detectedProfile.value = detectFirmwareProfile(transport)
                    when {
                        _detectedProfile.value.supportsBruce() -> {
                            _bruceLinkReady.value = true
                            refreshSystemInfo()
                        }
                        type == TransportType.USB -> applyUsbFlashOnlyHint()
                        else -> applySerialOnlyFallback()
                    }
                    setSubGhzFrequency(_subGhzFrequencyMhz.value)
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
                EmbedNotificationHelper.notifyDeviceConnected(appContext, TransportType.USB.name, detail)
                scope.launch {
                    _detectedProfile.value = detectFirmwareProfile(transport)
                    if (_detectedProfile.value.supportsBruce()) {
                        _bruceLinkReady.value = true
                        refreshSystemInfo()
                    } else {
                        applyUsbFlashOnlyHint()
                    }
                    setSubGhzFrequency(_subGhzFrequencyMhz.value)
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
        _bruceLinkReady.value = false
        _connectionState.value = ConnectionState.Disconnected
        SoundFeedback.playDisconnect()
        EmbedWidgetProvider.updateAllWidgets(appContext)
        EmbedNotificationHelper.cancelConnectionNotification(appContext)
    }

    /**
     * Libera el puerto USB antes de esptool ROM.
     */
    suspend fun prepareForUsbFlash(): Result<Unit> {
        disconnect()
        delay(600)
        return Result.success(Unit)
    }

    /** Reconecta USB tras flasheo (best-effort). */
    suspend fun reconnectAfterUsbFlash() {
        delay(2500)
        connect(TransportType.USB)
    }

    fun isUsbActive(): Boolean =
        activeTransport is UsbTransport && _connectionState.value is ConnectionState.Connected

    suspend fun sendBruceCliLine(line: String): Result<String> {
        val trimmed = line.trim()
        if (trimmed.startsWith("{")) {
            return Result.failure(
                Exception("Comando JSON obsoleto. Usa líneas CLI Bruce (info, subghz rx, …).")
            )
        }
        return executeBruceCli(trimmed)
    }

    /**
     * Envío CLI Bruce para motores internos (workflows, bruce sync, autopilot).
     */
    suspend fun executeBruceCli(line: String): Result<String> {
        ensureBruceProfile()
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("Conecta T-Embed (USB, BLE o WiFi) con firmware Bruce."))
        }
        val transport = activeTransport
        if (transport == null || _connectionState.value !is ConnectionState.Connected) {
            pendingCommandQueue.enqueue(line)
            return Result.failure(Exception("Sin transporte activo — comando en cola (${pendingCommandQueue.size})."))
        }

        return try {
            withTimeout(320_000L) {
                sendBruceCliLineOnce(transport, line)
            }
        } catch (_: TimeoutCancellationException) {
            Result.failure(Exception("Timeout Bruce CLI: sin respuesta."))
        }
    }

    /** Si hay transporte pero el perfil quedó UNKNOWN (p. ej. tras Autopilot/reconnect), re-detecta con ping. */
    private suspend fun ensureBruceProfile() {
        if (_detectedProfile.value.supportsBruce()) return
        val transport = activeTransport ?: return
        if (_connectionState.value !is ConnectionState.Connected) return
        val profile = detectFirmwareProfile(transport)
        _detectedProfile.value = profile
        if (profile.supportsBruce()) {
            _events.tryEmit(DeviceEvent.RawLine("[perfil] re-detectado ${profile.name} vía ping"))
        }
    }

    private suspend fun sendBruceCliLineOnce(transport: TEmbedTransport, line: String): Result<String> {
        return bruceLinkClient.sendRawJson(transport, line, timeoutMs = timeoutForCli(line)).onSuccess { response ->
            LinkDebugLog.appendOutgoing(line)
            val safe = TehLinkResponseParser.redactSensitiveResponse(response)
            _events.tryEmit(DeviceEvent.RawLine(safe))
            ingestCompanionCliResponse(line, response)
        }
    }

    private fun timeoutForCli(line: String): Long {
        val parts = line.trim().split(Regex("\\s+"))
        return when {
            parts.getOrNull(0)?.equals("subghz", ignoreCase = true) == true &&
                parts.getOrNull(1)?.equals("rx", ignoreCase = true) == true -> {
                val seconds = parts.getOrNull(3)?.toIntOrNull() ?: 15
                (seconds + 10) * 1000L
            }
            parts.getOrNull(0)?.equals("ir", ignoreCase = true) == true &&
                parts.getOrNull(1)?.equals("rx", ignoreCase = true) == true -> {
                val seconds = parts.getOrNull(2)?.toIntOrNull() ?: 10
                (seconds + 8) * 1000L
            }
            parts.getOrNull(0)?.equals("rfid", ignoreCase = true) == true -> 35_000L
            parts.getOrNull(0)?.equals("badusb", ignoreCase = true) == true -> 35_000L
            else -> 8_000L
        }
    }

    private fun ingestCompanionCliResponse(command: String, response: String) {
        if (response.isBlank()) return
        response.lineSequence().forEach { line ->
            val t = line.trim()
            if (t.isNotBlank()) handleIncomingLine(t)
        }
        if (!command.trim().startsWith("subghz rx", ignoreCase = true)) return
        scope.launch {
            val repo = signalRepository ?: return@launch
            val recent = repo.getLatest()
            if (recent != null && recent.timestamp >= System.currentTimeMillis() - 5_000) return@launch
            val freqMhz = _subGhzFrequencyMhz.value.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 433.92
            val entry = BruceCliCaptureParser.parseSubGhzResponse(response, freqMhz) ?: return@launch
            val (lat, lng) = locationTracker?.currentLatLng() ?: (null to null)
            val decoded = RfProtocolDecoder.decode(response)
            val id = repo.saveSubGhzSignal(entry, lat, lng, decoded)
            _events.tryEmit(DeviceEvent.SubGhzSignalSaved(entry, id))
        }
    }

    /** Captura Sub-GHz companion: CLI `subghz rx FREQ SECONDS` → biblioteca local. */
    suspend fun captureSubGhzCompanion(seconds: Int = 15): Result<String> {
        ensureBruceProfile()
        val transport = activeTransport
            ?: return Result.failure(Exception("Sin transporte Bruce conectado."))
        if (_connectionState.value !is ConnectionState.Connected) {
            return Result.failure(Exception("Sin conexión activa."))
        }
        val freqMhz = _subGhzFrequencyMhz.value.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 433.92
        val freqHz = BruceCli.mhzToHz(freqMhz)
        val sec = seconds.coerceIn(1, 120)
        val cmd = "subghz rx $freqHz $sec"
        _events.tryEmit(DeviceEvent.RawLine("> $cmd"))
        return BruceCli.sendAndCollect(transport, cmd, (sec + 10) * 1000L).map { response ->
            ingestCompanionCliResponse(cmd, response)
            val latest = signalRepository?.getLatest()
            when {
                latest != null && latest.timestamp >= System.currentTimeMillis() - 8_000 -> {
                    "Captura guardada #${latest.id} · ${latest.protocol} @ ${latest.frequency} MHz"
                }
                response.isNotBlank() -> "Captura ${sec}s @ ${"%.2f".format(freqMhz)} MHz — revisa consola"
                else -> "Captura ${sec}s — sin respuesta del CC1101"
            }
        }
    }

    /** Escucha IR companion vía `ir rx SECONDS`. */
    suspend fun captureIrCompanion(seconds: Int = 10): Result<String> {
        ensureBruceProfile()
        val transport = activeTransport
            ?: return Result.failure(Exception("Sin transporte Bruce conectado."))
        if (_connectionState.value !is ConnectionState.Connected) {
            return Result.failure(Exception("Sin conexión activa."))
        }
        val sec = seconds.coerceIn(1, 60)
        val cmd = "ir rx $sec"
        _events.tryEmit(DeviceEvent.RawLine("> $cmd"))
        return BruceCli.sendAndCollect(transport, cmd, (sec + 8) * 1000L).map { response ->
            ingestCompanionCliResponse(cmd, response)
            BruceCliCaptureParser.parseIrCapture(response)?.cliCommand
                ?: response.lineSequence().firstOrNull { it.isNotBlank() }?.take(120)
                ?: "IR RX ${sec}s — sin código detectado"
        }
    }

    suspend fun refreshSystemInfo() {
        val transport = activeTransport ?: return
        refreshTehLinkSystemInfo(transport)
    }

    suspend fun clearCoredump(): Result<Boolean> {
        val transport = activeTransport ?: return Result.failure(IllegalStateException("Not connected"))
        val r = bruceLinkClient.clearCoredump(transport)
        if (r.isSuccess) refreshTehLinkSystemInfo(transport)
        return r
    }

    suspend fun runSoakStress(iterations: Int, perStepSeconds: Int): Result<TehLinkSoakResult> {
        val transport = activeTransport ?: return Result.failure(IllegalStateException("Not connected"))
        val r = bruceLinkClient.runSoakStress(transport, iterations, perStepSeconds)
        if (r.isSuccess) refreshTehLinkSystemInfo(transport)
        return r
    }

    private suspend fun refreshTehLinkSystemInfo(transport: TEmbedTransport) {
        var info = _systemInfo.value
        var gotData = false
        bruceLinkClient.getInfo(transport).onSuccess { device ->
            gotData = true
            info = info.copy(
                firmware = "${device.product} v${device.version} (${device.codename})",
                codename = device.codename,
                channel = device.channel,
                profile = FirmwareProfile.BRUCE,
                brucePlugins = device.plugins,
                hardening = device.hardening,
                battery = device.battery?.let { "${it.percentage}% · ${"%.2f".format(it.voltage)}V" }
                    ?: info.battery,
                sdMounted = when (device.sdStatus) {
                    "mounted" -> "OK"
                    "error" -> "MISSING"
                    else -> info.sdMounted
                }
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

        bruceLinkClient.getStatus(transport).onSuccess { status ->
            gotData = true
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
                profile = FirmwareProfile.BRUCE,
                simFlags = status.sim,
                bruceCapabilities = status.capabilities,
                battery = formatBatteryLine(status) ?: info.battery,
                freeHeap = heapLine,
                freeHeapBytes = status.heapFreeBytes,
                freePsramBytes = status.psramFreeBytes,
                coredumpPending = status.coredumpPresent,
                wdtPanicReason = status.wdtPanicReason,
                temperatureC = status.temperatureC?.let { "%.0f°C".format(it) } ?: info.temperatureC
            )
            _systemInfo.value = info
            _events.tryEmit(DeviceEvent.SystemInfoUpdate(info))
            _events.tryEmit(DeviceEvent.RawLine("[TEH-Link] UI: ${status.uiScreen}"))
        }.onFailure {
            _systemInfo.value = info
            _events.tryEmit(DeviceEvent.RawLine("[TEH-Link] get_status: ${it.message}"))
        }
        if (gotData) {
            _bruceLinkReady.value = true
            _detectedProfile.value = FirmwareProfile.BRUCE
        } else if (_connectionState.value is ConnectionState.Connected) {
            applySerialOnlyFallback()
        }
    }

    private fun applyUsbFlashOnlyHint() {
        _bruceLinkReady.value = false
        _detectedProfile.value = FirmwareProfile.UNKNOWN
        _systemInfo.value = _systemInfo.value.copy(
            firmware = appContext.getString(R.string.dash_firmware_usb_flash),
            battery = "",
            temperatureC = "",
            uptime = "",
            freeHeap = "",
            channel = "USB"
        )
        _events.tryEmit(DeviceEvent.SystemInfoUpdate(_systemInfo.value))
        _events.tryEmit(DeviceEvent.TehLinkNotice(appContext.getString(R.string.dash_usb_flash_hint)))
    }

    private fun applySerialOnlyFallback() {
        _bruceLinkReady.value = false
        _detectedProfile.value = FirmwareProfile.UNKNOWN
        val detail = (_connectionState.value as? ConnectionState.Connected)?.detail.orEmpty()
        _systemInfo.value = _systemInfo.value.copy(
            firmware = appContext.getString(R.string.dash_firmware_no_bruce),
            battery = "",
            temperatureC = "",
            uptime = "",
            freeHeap = "",
            channel = detail.ifBlank { _activeTransportType.value.name }
        )
        _events.tryEmit(DeviceEvent.SystemInfoUpdate(_systemInfo.value))
        _events.tryEmit(DeviceEvent.TehLinkNotice(appContext.getString(R.string.bruce_no_response_notice)))
    }

    private suspend fun detectFirmwareProfile(transport: TEmbedTransport): FirmwareProfile {
        delay(if (transport.type == TransportType.BLE) 400 else 200)
        var pingOk = bruceLinkClient.ping(transport).getOrElse { false }
        if (!pingOk) {
            delay(800)
            pingOk = bruceLinkClient.ping(transport).getOrElse { false }
        }
        if (!pingOk) return FirmwareProfile.UNKNOWN
        bruceLinkClient.getInfo(transport).getOrNull()?.capabilityList?.let {
            com.embedsuite.app.core.CapabilityGate.updateFromList(it)
        }
        return FirmwareProfile.BRUCE
    }

    suspend fun tehLinkOpenPlugin(pluginId: String): Result<TehLinkScreenInfo> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("TEH-Link requiere T-Embed Bruce conectado."))
        }
        return bruceLinkClient.openPlugin(transport, pluginId).onSuccess { screen ->
            _systemInfo.value = _systemInfo.value.copy(uiScreen = screen.uiScreen)
            _events.tryEmit(DeviceEvent.RawLine("[TEH-Link] open_plugin: ${screen.openedPluginId.ifBlank { pluginId }} → ${screen.uiScreen}"))
        }
    }

    suspend fun tehLinkBackToMenu(): Result<TehLinkScreenInfo> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("TEH-Link requiere T-Embed Bruce conectado."))
        }
        return bruceLinkClient.backToMenu(transport).onSuccess { screen ->
            _systemInfo.value = _systemInfo.value.copy(uiScreen = screen.uiScreen)
            _events.tryEmit(DeviceEvent.RawLine("[TEH-Link] back_to_menu → ${screen.uiScreen}"))
        }
    }

    suspend fun tehLinkListActions(): Result<List<TehLinkActionInfo>> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("TEH-Link requiere T-Embed Bruce conectado."))
        }
        return bruceLinkClient.listActions(transport).onSuccess { actions ->
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
        ensureBruceProfile()
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("Conecta T-Embed (USB, BLE o WiFi) con firmware Bruce."))
        }
        return bruceLinkClient.runAction(transport, pluginId, action, params).onSuccess { result ->
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
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("TEH-Link requiere T-Embed Bruce conectado."))
        }
        return bruceLinkClient.getActionState(transport, pluginId, action)
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
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("TEH-Link requiere T-Embed Bruce conectado."))
        }
        locationTracker?.startTracking()
        val lat = latitude ?: locationTracker?.location?.value?.latitude
        val lon = longitude ?: locationTracker?.location?.value?.longitude
        val alt = altitudeM ?: locationTracker?.location?.value?.altitude
        return bruceLinkClient.runWardrivingStart(transport, lat, lon, alt).onSuccess { result ->
            _events.tryEmit(
                DeviceEvent.RawLine(
                    "[TEH-Link] wardriving/start → ${result.state.state.ifBlank { result.state.message }}"
                )
            )
        }
    }

    suspend fun tehLinkRunWardrivingGpsUpdate(): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("TEH-Link requiere T-Embed Bruce conectado."))
        }
        val loc = locationTracker?.location?.value
            ?: return Result.failure(Exception("GPS del teléfono no disponible."))
        return bruceLinkClient.runWardrivingGpsUpdate(
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
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("TEH-Link requiere T-Embed Bruce conectado."))
        }
        return bruceLinkClient.runIrSend(transport, protocol, address, command).onSuccess { result ->
            _events.tryEmit(
                DeviceEvent.RawLine(
                    "[TEH-Link] ir_toolkit/send → ${result.state.message.ifBlank { result.state.state }}"
                )
            )
        }
    }

    suspend fun tehLinkRunWardrivingStop(): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("TEH-Link requiere T-Embed Bruce conectado."))
        }
        return bruceLinkClient.runAction(transport, "wardriving", "stop").onSuccess { result ->
            locationTracker?.stopTracking()
            _events.tryEmit(
                DeviceEvent.RawLine(
                    "[TEH-Link] wardriving/stop → ${result.state.state.ifBlank { result.state.message }}"
                )
            )
        }
    }

    fun hasBruceCapability(key: String): Boolean {
        if (!_detectedProfile.value.supportsBruce()) return false
        val caps = _systemInfo.value.bruceCapabilities
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
        return _systemInfo.value.brucePlugins.any { it.id == pluginId }
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
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("Sub-GHz TX TEH-Link solo en Bruce/TEH-Link."))
        }
        return bruceLinkClient.runSubGhzTx(transport, rawHex, freqMhz).onSuccess { result ->
            _events.tryEmit(
                DeviceEvent.RawLine(
                    "[TEH-Link] subghz_tx → ${result.state.message.ifBlank { result.state.state }}"
                )
            )
        }
    }

    suspend fun tehLinkRunSubGhzReplay(devicePath: String): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("Sub-GHz replay TEH-Link solo en Bruce/TEH-Link."))
        }
        return bruceLinkClient.runSubGhzReplay(transport, devicePath).onSuccess { result ->
            _events.tryEmit(
                DeviceEvent.RawLine(
                    "[TEH-Link] subghz_replay → ${result.state.message.ifBlank { result.state.state }}"
                )
            )
        }
    }

    suspend fun tehLinkRunIrRx(seconds: Int = 10): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("IR RX TEH-Link solo en Bruce/TEH-Link."))
        }
        return bruceLinkClient.runIrRxStart(transport, seconds)
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
        return bruceLinkClient.syncTime(transport, timestampNs).onSuccess { data ->
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
        return bruceLinkClient.audioBeep(transport, freqHz, durationMs)
    }

    suspend fun sdCardStatus(): Result<JSONObject> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        return bruceLinkClient.sdStatus(transport)
    }

    suspend fun sdCardList(path: String = "/bruce"): Result<List<String>> =
        listStorageEntries(path).map { entries -> entries.map { it.displayName } }

    suspend fun listStorageEntries(path: String = "/bruce"): Result<List<com.embedsuite.app.core.bruce.BruceStorageParser.Entry>> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        return bruceLinkClient.listFiles(transport, path).recoverCatching {
            bruceLinkClient.sdList(transport, path).getOrThrow()
        }.map { data ->
            val raw = data.optString("raw").ifBlank { data.toString() }
            com.embedsuite.app.core.bruce.BruceStorageParser.parseListResponse(raw, path)
        }
    }

    suspend fun readStorageFile(path: String): Result<String> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        return bruceLinkClient.storageRead(transport, path).map { raw ->
            com.embedsuite.app.core.bruce.BruceStorageParser.extractFileContent(raw)
        }
    }

    suspend fun uploadFileToDevice(localFile: File, remotePath: String): Result<String> {
        if (!localFile.exists()) {
            return Result.failure(IllegalArgumentException("Archivo local no existe"))
        }
        wifiApManager?.bindToWifiTransport()
        if (_connectionState.value !is ConnectionState.Connected ||
            _activeTransportType.value != TransportType.WIFI
        ) {
            connect(TransportType.WIFI).getOrElse {
                return Result.failure(
                    Exception("${com.embedsuite.app.core.bruce.BruceLimits.WIFI_UPLOAD_HINT} (${it.message})")
                )
            }
        }
        return com.embedsuite.app.core.wifi.WifiFileTransfer(host = wifiHost())
            .uploadFile(localFile, remotePath.trim())
    }

    suspend fun runBadUsbFromFile(devicePath: String): Result<String> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        return bruceLinkClient.runBadUsbFromFile(transport, devicePath).map { result ->
            result.state.message.ifBlank { "badusb OK" }
        }
    }

    suspend fun sendNavCommand(command: String): Result<String> {
        val allowed = setOf("nav esc", "nav up", "nav down", "nav select", "nav next", "nav prev")
        val cmd = command.trim().lowercase()
        if (cmd !in allowed) {
            return Result.failure(IllegalArgumentException("Comando nav no permitido: $command"))
        }
        return sendBruceCliLine(cmd)
    }

    suspend fun connectBleDevice(device: android.bluetooth.BluetoothDevice): Result<String> {
        _connectionState.value = ConnectionState.Connecting
        activeTransport?.disconnect()
        val result = bleTransport.connectToDevice(device)
        result.fold(
            onSuccess = { detail ->
                activeTransport = bleTransport
                _activeTransportType.value = TransportType.BLE
                _connectionState.value = ConnectionState.Connected(TransportType.BLE, detail)
                SoundFeedback.playConnect()
                scope.launch {
                    _detectedProfile.value = detectFirmwareProfile(bleTransport)
                    _bruceLinkReady.value = _detectedProfile.value.supportsBruce()
                    if (_bruceLinkReady.value) refreshSystemInfo()
                }
            },
            onFailure = { _connectionState.value = ConnectionState.Error(it.message ?: "BLE falló") }
        )
        return result
    }

    suspend fun downloadDeviceFile(path: String): Result<ByteArray> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        return bruceLinkClient.downloadFile(transport, path)
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
            val result = bruceLinkClient.sdSave(transport, filename, chunk, append = !first)
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
        return bruceLinkClient.rfScanStart(
            transport, freqStart, freqEnd, step, rssiThreshold, dwellMs, maxHz
        ).onSuccess {
            _events.tryEmit(DeviceEvent.RawLine("[TEH-Link] rf.scan.start ${freqStart}-${freqEnd}"))
        }
    }

    suspend fun rfScanStop(): Result<JSONObject> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        return bruceLinkClient.rfScanStop(transport)
    }

    suspend fun rfJammerStart(
        freqMhz: Double,
        power: Int = 10,
        mode: String = "continuous",
        burstInterval: Int? = null,
        maxSeconds: Int = 30
    ): Result<JSONObject> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        return bruceLinkClient.rfJammerStart(
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
        return bruceLinkClient.rfJammerStop(transport).onSuccess {
            scope.launch { deviceAudioBeep(400, 60) }
        }
    }

    suspend fun rfJammerStatus(): Result<JSONObject> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        return bruceLinkClient.rfJammerStatus(transport)
    }

    // ===== BRUCE v0.19+ (Evil Portal API) =====
    suspend fun tehLinkRunEvilPortalStart(
        ssid: String,
        templateId: String = "generic",
        channel: Int = 6
    ): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("Evil Portal solo disponible con firmware Bruce."))
        }
        val params = JSONObject()
            .put("ssid", ssid)
            .put("template_id", templateId)
            .put("channel", channel.coerceIn(1, 11))
        return bruceLinkClient.runAction(transport, "evil_portal", "start", params).onSuccess { result ->
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

    // ===== BRUCE v0.19+ (Beacon Spam API) =====
    suspend fun tehLinkRunBeaconSpamStart(
        spec: String = "random:50",
        hz: Int = 10,
        channel: Int = 0
    ): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("Beacon Spam solo disponible con firmware Bruce."))
        }
        val params = JSONObject()
            .put("spec", spec)
            .put("hz", hz.coerceIn(1, 100))
            .put("channel", channel.coerceIn(0, 11))
        return bruceLinkClient.runAction(transport, "beacon_spam", "start", params).onSuccess { result ->
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
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("TEH-Link requiere T-Embed Bruce conectado."))
        }
        return bruceLinkClient.runCryptoHash(transport, input, algo).onSuccess { result ->
            _events.tryEmit(
                DeviceEvent.RawLine("[TEH-Link] crypto_toolkit/hash → [REDACTED]")
            )
        }
    }

    suspend fun tehLinkRunCryptoBase64Encode(input: String): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("TEH-Link requiere T-Embed Bruce conectado."))
        }
        return bruceLinkClient.runCryptoBase64Encode(transport, input).onSuccess {
            _events.tryEmit(
                DeviceEvent.RawLine("[TEH-Link] crypto_toolkit/base64_encode → [REDACTED]")
            )
        }
    }

    suspend fun tehLinkRunGenPassword(length: Int = 16): Result<TehLinkActionResult> {
        val transport = activeTransport ?: return Result.failure(Exception("No hay transporte activo."))
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("TEH-Link requiere T-Embed Bruce conectado."))
        }
        return bruceLinkClient.runGenPassword(transport, length).onSuccess {
            _events.tryEmit(
                DeviceEvent.RawLine("[TEH-Link] crypto_toolkit/gen_password → [REDACTED]")
            )
        }
    }

    suspend fun startSubGhzRawCapture(seconds: Int = 10): Result<String> {
        return captureSubGhzCompanion(seconds).onFailure {
            _events.tryEmit(DeviceEvent.TehLinkNotice(it.message ?: "Captura Sub-GHz falló"))
        }
    }

    /** Captura Sub-GHz remota vía TEH-Link (Bruce / CC1101 Plus). */
    suspend fun startSubGhzTehLinkCapture(seconds: Int = 15): Result<TehLinkActionResult> {
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("Captura TEH-Link solo disponible con firmware Bruce."))
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
        if (!_detectedProfile.value.supportsBruce()) {
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
        onProgress(0)
        return Result.failure(
            Exception(
                "OTA inalámbrica no disponible. Flashea el firmware vía USB con esptool."
            )
        )
    }

    fun simulateMockLongPress(): Result<Unit> {
        if (activeTransport !is MockTransport) {
            return Result.failure(Exception("Solo disponible con transporte mock."))
        }
        mockTransport.armBadusbRemote(120)
        _events.tryEmit(DeviceEvent.TehLinkNotice("Mock: BadUSB arm (120 s)."))
        return Result.success(Unit)
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

    fun activeTransportOrNull(): TEmbedTransport? = activeTransport

    /** Bruce CLI sobre transporte explícito (orquestador multi-canal). */
    suspend fun executeBruceCliOn(transport: TEmbedTransport, line: String): Result<String> {
        ensureBruceProfile()
        if (!_detectedProfile.value.supportsBruce()) {
            return Result.failure(Exception("Conecta T-Embed (USB, BLE o WiFi) con firmware Bruce."))
        }
        val trimmed = line.trim()
        if (trimmed.startsWith("{")) {
            return Result.failure(
                Exception("Comando JSON obsoleto. Usa líneas CLI Bruce (info, subghz rx, …).")
            )
        }
        return try {
            withTimeout(320_000L) {
                sendBruceCliLineOnce(transport, trimmed)
            }
        } catch (_: TimeoutCancellationException) {
            Result.failure(Exception("Timeout Bruce CLI: sin respuesta."))
        }
    }

    fun bruceLinkClientForHeadless(): BruceLinkClient = bruceLinkClient

    fun activeTransportForHeadless(): TEmbedTransport? = activeTransport

    companion object {
        private const val SIGNAL_LOG_MAX = 500
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
