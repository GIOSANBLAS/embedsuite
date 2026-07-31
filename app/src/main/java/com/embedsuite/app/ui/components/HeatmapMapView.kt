package com.embedsuite.app.ui.components

import android.graphics.Color
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.embedsuite.app.data.CapturedSignalEntity
import com.embedsuite.app.map.DarkMapTileSource
import com.embedsuite.app.map.OsmdroidConfig
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@Composable
fun HeatmapMapView(
    signals: List<CapturedSignalEntity>,
    currentLat: Double?,
    currentLng: Double?,
    layerFilter: String = "ALL",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        OsmdroidConfig.init(context)
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { mapView?.onDetach() }
    }

    val filtered = remember(signals, layerFilter) {
        signals.filter { s ->
            s.latitude != null && s.longitude != null &&
                (layerFilter == "ALL" || s.signalType.equals(layerFilter, ignoreCase = true))
        }
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
                mapView = this
            }
        },
        update = { map ->
            map.overlays.clear()

            val cells = buildHeatmapCells(filtered)
            cells.forEach { cell ->
                val polygon = Polygon(map).apply {
                    points = cell.corners
                    fillColor = heatColor(cell.count, cell.avgRssi)
                    strokeColor = Color.argb(80, 0, 255, 65)
                    strokeWidth = 1f
                    title = "${cell.count} señales // ${cell.avgRssi} dBm avg"
                }
                map.overlays.add(polygon)
            }

            filtered.forEach { signal ->
                val lat = signal.latitude ?: return@forEach
                val lng = signal.longitude ?: return@forEach
                val marker = Marker(map).apply {
                    position = GeoPoint(lat, lng)
                    title = "${signal.signalType}: ${signal.name.ifBlank { signal.protocol }}"
                    snippet = "RSSI: ${signal.rssi}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(marker)
            }

            if (currentLat != null && currentLng != null) {
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(currentLat, currentLng)
                    title = "Posición actual"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                })
            }

            val points = filtered.mapNotNull { s ->
                val lat = s.latitude ?: return@mapNotNull null
                val lng = s.longitude ?: return@mapNotNull null
                GeoPoint(lat, lng)
            }
            if (points.isNotEmpty()) {
                val lats = points.map { it.latitude }
                val lngs = points.map { it.longitude }
                map.zoomToBoundingBox(
                    BoundingBox(lats.max(), lngs.max(), lats.min(), lngs.min()),
                    true, 60
                )
            } else if (currentLat != null && currentLng != null) {
                map.controller.setZoom(15.0)
                map.controller.setCenter(GeoPoint(currentLat, currentLng))
            }
            map.invalidate()
        },
        modifier = modifier.fillMaxSize()
    )
}

private data class HeatCell(val corners: List<GeoPoint>, val count: Int, val avgRssi: Int)

private fun buildHeatmapCells(signals: List<CapturedSignalEntity>): List<HeatCell> {
    if (signals.isEmpty()) return emptyList()
    val gridSize = 0.002
    val buckets = mutableMapOf<String, MutableList<CapturedSignalEntity>>()
    signals.forEach { s ->
        val lat = s.latitude ?: return@forEach
        val lng = s.longitude ?: return@forEach
        val key = "${(lat / gridSize).toInt()}_${(lng / gridSize).toInt()}"
        buckets.getOrPut(key) { mutableListOf() }.add(s)
    }
    return buckets.map { (_, list) ->
        val lat = list.mapNotNull { it.latitude }.average()
        val lng = list.mapNotNull { it.longitude }.average()
        val half = gridSize / 2
        HeatCell(
            corners = listOf(
                GeoPoint(lat + half, lng - half),
                GeoPoint(lat + half, lng + half),
                GeoPoint(lat - half, lng + half),
                GeoPoint(lat - half, lng - half)
            ),
            count = list.size,
            avgRssi = list.map { it.rssi }.average().toInt()
        )
    }
}

private fun heatColor(count: Int, rssi: Int): Int {
    val intensity = (count.coerceIn(1, 10) / 10f)
    val r = (255 * intensity).toInt()
    val g = (255 * (1 - intensity * 0.5f)).toInt()
    return Color.argb((120 + intensity * 80).toInt(), r, g, 0)
}
