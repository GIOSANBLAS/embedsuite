package com.embedsuite.app.scan

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationTracker(context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { _location.value = it }
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (_isTracking.value) return
        // 5s / min 4s: menos drenaje que 3s/2s; suficiente para war-drive y campo
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(4_000L)
            .build()

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        _isTracking.value = true

        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) _location.value = loc
        }
    }

    fun stopTracking() {
        fusedClient.removeLocationUpdates(callback)
        _isTracking.value = false
    }

    fun currentLatLng(): Pair<Double?, Double?> {
        val loc = _location.value
        return loc?.latitude to loc?.longitude
    }
}
