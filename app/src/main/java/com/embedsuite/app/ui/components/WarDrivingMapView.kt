package com.embedsuite.app.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.map.DarkMapTileSource
import com.embedsuite.app.map.OsmdroidConfig
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun WarDrivingMapView(
    signals: List<CapturedSignalEntity>,
    currentLat: Double?,
    currentLng: Double?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        OsmdroidConfig.init(context)
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { mapView?.onDetach() }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setTileSource(DarkMapTileSource)
                setMultiTouchControls(true)
                isHorizontalMapRepetitionEnabled = false
                isVerticalMapRepetitionEnabled = false
                mapView = this
            }
        },
        update = { map ->
            map.overlays.clear()

            signals.filter { it.latitude != null && it.longitude != null }.forEach { signal ->
                val lat = signal.latitude ?: return@forEach
                val lng = signal.longitude ?: return@forEach
                val marker = Marker(map).apply {
                    position = GeoPoint(lat, lng)
                    title = "${signal.signalType}: ${signal.name.ifBlank { signal.protocol }}"
                    snippet = "RSSI: ${signal.rssi} // ${signal.detail}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(marker)
            }

            val center = when {
                currentLat != null && currentLng != null -> GeoPoint(currentLat, currentLng)
                signals.any { it.latitude != null } -> {
                    val s = signals.first { it.latitude != null && it.longitude != null }
                    GeoPoint(s.latitude ?: 0.0, s.longitude ?: 0.0)
                }
                else -> GeoPoint(19.4326, -99.1332)
            }

            if (currentLat != null && currentLng != null) {
                val userMarker = Marker(map).apply {
                    position = GeoPoint(currentLat, currentLng)
                    title = "Posición actual"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
                map.overlays.add(userMarker)
            }

            map.controller.setZoom(15.0)
            map.controller.setCenter(center)
            map.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    )
}
