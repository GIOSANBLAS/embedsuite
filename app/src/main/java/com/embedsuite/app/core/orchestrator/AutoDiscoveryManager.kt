package com.embedsuite.app.core.orchestrator

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.connection.TransportType
import com.embedsuite.app.core.bruce.BruceLimits
import com.embedsuite.app.core.wifi.WifiApManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Descubrimiento automático T-Embed:
 * 1) mDNS _bruce._tcp  2) SSID Bruce_* → 192.168.4.1  3) USB fallback
 */
class AutoDiscoveryManager(
    private val context: Context,
    private val connectionManager: DeviceConnectionManager
) {
    data class DiscoveryState(
        val host: String?,
        val recommended: TransportType,
        val source: String,
        val message: String
    )

    data class Hint(
        val recommended: TransportType,
        val message: String
    )

    private val _discoveryState = MutableStateFlow(
        DiscoveryState(null, TransportType.USB, "idle", "Sin descubrimiento")
    )
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    suspend fun discover(timeoutMs: Long = 5_000L): DiscoveryState {
        resolveMdns(timeoutMs)?.let { state ->
            _discoveryState.value = state
            return state
        }
        resolveSsid()?.let { state ->
            _discoveryState.value = state
            return state
        }
        val usb = DiscoveryState(
            host = null,
            recommended = TransportType.USB,
            source = "usb_fallback",
            message = "USB serial — conecta cable o activa Wizard WiFi/BLE"
        )
        _discoveryState.value = usb
        return usb
    }

    suspend fun hintForFileUpload(): Hint {
        val discovered = discover()
        if (discovered.recommended == TransportType.WIFI && discovered.host != null) {
            return Hint(TransportType.WIFI, "WiFi Bruce @ ${discovered.host} — listo para upload.")
        }
        val state = connectionManager.connectionState.first()
        if (state !is ConnectionState.Connected) {
            return Hint(TransportType.WIFI, BruceLimits.WIFI_UPLOAD_HINT)
        }
        return when (connectionManager.activeTransportType.value) {
            TransportType.WIFI -> Hint(TransportType.WIFI, "WiFi activo — listo para upload.")
            TransportType.USB -> Hint(TransportType.WIFI, "USB OK para CLI; subida pesada: AP Bruce.")
            TransportType.BLE -> Hint(TransportType.WIFI, "BLE OK para CLI; subida: Wizard WiFi.")
        }
    }

    suspend fun ensureCliReady(timeoutMs: Long = 8_000L): Result<Unit> {
        if (connectionManager.bruceLinkReady.value) return Result.success(Unit)
        withTimeoutOrNull(timeoutMs) {
            connectionManager.refreshSystemInfo()
        }
        return if (connectionManager.bruceLinkReady.value) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Bruce no respondió a info. Revisa transporte."))
        }
    }

    private suspend fun resolveMdns(timeoutMs: Long): DiscoveryState? {
        val nsd = context.applicationContext.getSystemService(Context.NSD_SERVICE) as? NsdManager
            ?: return null
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val listener = object : NsdManager.DiscoveryListener {
                    override fun onStartDiscoveryFailed(type: String?, code: Int) {
                        if (cont.isActive) cont.resume(null)
                    }
                    override fun onStopDiscoveryFailed(type: String?, code: Int) {}
                    override fun onDiscoveryStarted(type: String?) {}
                    override fun onDiscoveryStopped(type: String?) {}
                    override fun onServiceFound(service: NsdServiceInfo) {
                        if (!service.serviceName.contains("bruce", ignoreCase = true)) return
                        nsd.resolveService(service, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(info: NsdServiceInfo?, code: Int) {}
                            override fun onServiceResolved(info: NsdServiceInfo) {
                                val host = info.host?.hostAddress ?: return
                                if (cont.isActive) {
                                    cont.resume(
                                        DiscoveryState(
                                            host = host,
                                            recommended = TransportType.WIFI,
                                            source = "mdns",
                                            message = "mDNS ${info.serviceName} @ $host"
                                        )
                                    )
                                }
                            }
                        })
                    }
                    override fun onServiceLost(service: NsdServiceInfo) {}
                }
                runCatching {
                    nsd.discoverServices("_bruce._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
                }.onFailure {
                    if (cont.isActive) cont.resume(null)
                }
                cont.invokeOnCancellation {
                    runCatching { nsd.stopServiceDiscovery(listener) }
                }
            }
        }
    }

    private fun resolveSsid(): DiscoveryState? {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null
        @Suppress("DEPRECATION")
        val ssid = wm.connectionInfo?.ssid?.trim('"') ?: return null
        if (ssid.startsWith("Bruce_", ignoreCase = true) || ssid.contains("Bruce", ignoreCase = true)) {
            return DiscoveryState(
                host = WifiApManager.DEFAULT_HOST,
                recommended = TransportType.WIFI,
                source = "ssid",
                message = "SSID $ssid → ${WifiApManager.DEFAULT_HOST}"
            )
        }
        return null
    }
}
