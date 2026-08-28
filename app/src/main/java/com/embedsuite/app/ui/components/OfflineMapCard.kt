package com.embedsuite.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.R
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheSize by remember { mutableStateOf(OsmdroidConfig.formatCacheSize(OsmdroidConfig.cacheSizeBytes())) }
    var status by remember { mutableStateOf(context.getString(R.string.offline_map_status_idle)) }
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
                stringResource(R.string.offline_map_title),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = NeonCyan
            )
            Text(
                stringResource(R.string.offline_map_cache, cacheSize),
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
                    color = MatrixGreen,
                    trackColor = DarkSurfaceElevated
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeonOutlinedButton(
                    text = stringResource(R.string.offline_map_precache),
                    onClick = {
                        val lat = currentLat
                        val lng = currentLng
                        if (lat == null || lng == null) {
                            status = context.getString(R.string.offline_map_status_gps_required)
                            return@NeonOutlinedButton
                        }
                        downloading = true
                        status = context.getString(R.string.offline_map_status_downloading)
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
                                    status = context.getString(R.string.offline_map_status_done, tiles)
                                },
                                onFailure = { e ->
                                    status = context.getString(
                                        R.string.offline_map_status_error,
                                        e.message ?: "?"
                                    )
                                }
                            )
                            downloading = false
                        }
                    },
                    enabled = !downloading,
                    modifier = Modifier.weight(1f)
                )
                NeonOutlinedButton(
                    text = stringResource(R.string.offline_map_clear),
                    onClick = {
                        OsmdroidConfig.clearCache()
                        cacheSize = OsmdroidConfig.formatCacheSize(0)
                        status = context.getString(R.string.offline_map_status_cleared)
                    },
                    color = NeonRed,
                    enabled = !downloading,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
