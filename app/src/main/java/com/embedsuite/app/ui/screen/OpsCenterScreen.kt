package com.embedsuite.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.ui.theme.*

private data class OpsSection(
    val title: String,
    val subtitle: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun OpsCenterScreen(onNavigate: (String) -> Unit) {
    val sections = listOf(
        OpsSection(
            "Herramientas CLI",
            "Orquestador Sub-GHz · BadUSB · IR · plantillas locales",
            "tools_hub",
            Icons.Default.Hub
        ),
        OpsSection(
            "Bruce Config",
            "Copia local bruce.json (sync remoto no disponible en Bruce stock)",
            "bruce_config",
            Icons.Default.Sync
        ),
        OpsSection(
            "Firmware Customizer",
            "Manifest local de módulos · sin flash remoto",
            "firmware_customizer",
            Icons.Default.Tune
        ),
        OpsSection(
            "Fleet",
            "Perfiles · nickname · dispositivo activo",
            "fleet",
            Icons.Default.Devices
        )
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(BlackAMOLED)
            .padding(12.dp)
    ) {
        Text(
            "// OPS CENTER",
            color = MatrixGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Herramientas locales · control remoto solo vía Bruce CLI",
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp
        )
        Spacer(Modifier.height(12.dp))
        sections.forEach { section ->
            OpsHubCard(section = section, onClick = { onNavigate(section.route) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OpsHubCard(section: OpsSection, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, MatrixGreen.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(section.icon, section.title, tint = NeonCyan, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                section.title.uppercase(),
                color = MatrixGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                section.subtitle,
                color = TextGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
    }
}
