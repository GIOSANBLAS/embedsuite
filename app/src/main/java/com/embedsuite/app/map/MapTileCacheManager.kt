package com.embedsuite.app.map

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.cos

class MapTileCacheManager(private val context: Context) {

    suspend fun downloadAreaAround(
        lat: Double,
        lng: Double,
        radiusKm: Double = 8.0,
        zoomMin: Int = 10,
        zoomMax: Int = 16,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Int> = withContext(Dispatchers.Main) {
        runCatching {
            OsmdroidConfig.init(context)
            val mapView = MapView(context)
            mapView.setTileSource(DarkMapTileSource)
            val cacheManager = CacheManager(mapView)

            val latDelta = radiusKm / 111.0
            val lngDelta = radiusKm / (111.0 * cos(lat * PI / 180.0).coerceAtLeast(0.1))
            val bbox = BoundingBox(
                lat + latDelta,
                lng + lngDelta,
                lat - latDelta,
                lng - lngDelta
            )

            val total = cacheManager.possibleTilesInArea(bbox, zoomMin, zoomMax)
            if (total <= 0) return@runCatching 0

            suspendCancellableCoroutine { cont ->
                cacheManager.downloadAreaAsync(
                    context,
                    bbox,
                    zoomMin,
                    zoomMax,
                    object : CacheManager.CacheManagerCallback {
                        override fun onTaskComplete() {
                            if (cont.isActive) cont.resume(total)
                        }

                        override fun updateProgress(
                            progress: Int,
                            currentZoomLevel: Int,
                            zoomMinLevel: Int,
                            zoomMaxLevel: Int
                        ) {
                            onProgress(progress, total)
                        }

                        override fun downloadStarted() = Unit

                        override fun setPossibleTilesInArea(totalTiles: Int) = Unit

                        override fun onTaskFailed(errors: Int) {
                            if (cont.isActive) {
                                cont.resumeWith(
                                    Result.failure(Exception("Precache falló ($errors errores de tile)."))
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    fun boundingBoxAround(lat: Double, lng: Double, radiusKm: Double = 8.0): BoundingBox {
        val latDelta = radiusKm / 111.0
        val lngDelta = radiusKm / (111.0 * cos(lat * PI / 180.0).coerceAtLeast(0.1))
        return BoundingBox(lat + latDelta, lng + lngDelta, lat - latDelta, lng - lngDelta)
    }

    fun centerPoint(lat: Double, lng: Double): GeoPoint = GeoPoint(lat, lng)
}
