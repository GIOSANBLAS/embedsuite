package com.embedsuite.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.connection.TransportType
import com.embedsuite.app.core.connection.TransportAvailability
import com.embedsuite.app.ui.theme.*

@Composable
fun TransportStatusChip(
    label: String,
    active: Boolean,
    available: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = when {
        active -> EmbedGreen
        available -> EmbedCyan.copy(alpha = 0.7f)
        else -> TextMuted.copy(alpha = 0.4f)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .background(if (active) color.copy(alpha = 0.15f) else DarkSurfaceElevated)
            .clickable(enabled = available, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = color, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TransportIndicatorRow(
    availability: TransportAvailability,
    onSelect: (TransportType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TransportStatusChip(
            "USB",
            availability.isActive(TransportType.USB),
            availability.usbAvailable,
            { onSelect(TransportType.USB) },
            Modifier.weight(1f)
        )
        TransportStatusChip(
            "BLE",
            availability.isActive(TransportType.BLE),
            availability.bleAvailable,
            { onSelect(TransportType.BLE) },
            Modifier.weight(1f)
        )
        TransportStatusChip(
            "WiFi",
            availability.isActive(TransportType.WIFI),
            availability.wifiAvailable,
            { onSelect(TransportType.WIFI) },
            Modifier.weight(1f)
        )
    }
}

@Composable
fun TransportTelemetryStrip(
    batteryText: String,
    temperatureText: String,
    transportLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface.copy(alpha = 0.95f))
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🔋 $batteryText", color = EmbedGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text("🌡 $temperatureText", color = EmbedCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(transportLabel, color = NeonOrange, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
