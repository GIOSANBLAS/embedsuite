package com.embedsuite.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.map.MapTileCacheManager
import com.embedsuite.app.map.OsmdroidConfig
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OfflineMapCard(
    mapTileCacheManager: MapTileCacheManager,
    currentLat: Double?,
    currentLng: Double?,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var cacheSize by remember { mutableStateOf(OsmdroidConfig.formatCacheSize(OsmdroidConfig.cacheSizeBytes())) }
    var status by remember { mutableStateOf("Tiles en caché para war-driving sin datos.") }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                "MAPAS OFFLINE",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = NeonCyan
            )
            Text(
                "Caché: $cacheSize",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = TextGray
            )
            Text(
                status,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = if (downloading) NeonOrange else TextGray
            )
            if (downloading && total > 0) {
                LinearProgressIndicator(
                    progress = { progress.toFloat() / total.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    color = MatrixGreen
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val lat = currentLat
                        val lng = currentLng
                        if (lat == null || lng == null) {
                            status = "GPS requerido para precachear zona."
                            return@Button
                        }
                        downloading = true
                        status = "Descargando tiles ~8 km..."
                        scope.launch {
                            mapTileCacheManager.downloadAreaAround(
                                lat = lat,
                                lng = lng,
                                onProgress = { p, t ->
                                    progress = p
                                    total = t
                                }
                            ).fold(
                                onSuccess = { tiles ->
                                    cacheSize = OsmdroidConfig.formatCacheSize(OsmdroidConfig.cacheSizeBytes())
                                    status = "Listo: $tiles tiles cacheados."
                                },
                                onFailure = { e ->
                                    status = "Error: ${e.message}"
                                }
                            )
                            downloading = false
                        }
                    },
                    enabled = !downloading,
                    colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = BlackAMOLED)
                ) {
                    Text("PRECACHEAR ZONA", fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
                OutlinedButton(
                    onClick = {
                        OsmdroidConfig.clearCache()
                        cacheSize = OsmdroidConfig.formatCacheSize(0)
                        status = "Caché de mapas eliminada."
                    },
                    enabled = !downloading
                ) {
                    Text("LIMPIAR", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonRed)
                }
            }
        }
    }
}
