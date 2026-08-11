package com.embedsuite.app.scan

import com.embedsuite.app.connection.XibalbaAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * HybridLocationProvider — geolocalización híbrida Android (GPS) + T-Embed (RF).
 *
 * El T-Embed no tiene GPS propio: cada muestra RSSI del escáner CC1101 se cruza
 * con la última posición del teléfono. Antes de arrancar se sincroniza el reloj
 * del dispositivo (`time_sync`) para que los eventos compartan eje temporal.
 */
class HybridLocationProvider(
    private val locationTracker: LocationTracker,
    private val xibalba: XibalbaAdapter,
    private val scope: CoroutineScope
) {
    data class GeoRfSample(
        val freqMhz: Double,
        val rssi: Int,
        val latitude: Double?,
        val longitude: Double?,
        val altitudeM: Double? = null,
        val timestampMs: Long = System.currentTimeMillis(),
        val deviceTimestampMs: Long = 0
    )

    private val _geoSamples = MutableSharedFlow<GeoRfSample>(extraBufferCapacity = 256)
    val geoSamples: SharedFlow<GeoRfSample> = _geoSamples.asSharedFlow()

    private val _tracking = MutableStateFlow(false)
    val tracking: StateFlow<Boolean> = _tracking.asStateFlow()

    /** Offset de reloj reportado por el firmware tras time_sync (0 = sin sync). */
    @Volatile
    var lastTimeOffsetMs: Long = 0
        private set

    fun startHybridTracking() {
        if (_tracking.value) return
        _tracking.value = true
        locationTracker.startTracking()

        scope.launch {
            // Best-effort: si el firmware es < 0.20 el comando no existe y se sigue sin sync
            xibalba.syncTime().onSuccess { lastTimeOffsetMs = it.offsetMs }
        }

        scope.launch {
            xibalba.observeRfSamples().collect { sample ->
                val loc = locationTracker.location.value
                _geoSamples.tryEmit(
                    GeoRfSample(
                        freqMhz = sample.freqMhz,
                        rssi = sample.rssi,
                        latitude = loc?.latitude,
                        longitude = loc?.longitude,
                        altitudeM = loc?.altitude,
                        deviceTimestampMs = sample.timestampMs
                    )
                )
            }
        }
    }

    fun stopHybridTracking() {
        _tracking.value = false
        locationTracker.stopTracking()
    }
}
