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
import com.embedsuite.app.core.bruce.BruceLimits
import com.embedsuite.app.core.orchestrator.IntentPhase
import com.embedsuite.app.core.orchestrator.OrchestrationResult
import com.embedsuite.app.ui.theme.*

@Composable
fun OrchestrationFeedback(result: OrchestrationResult?) {
    if (result == null) return
    val color = if (result.success) MatrixGreen else NeonRed
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(
            if (result.success) "OK" else "ERROR",
            color = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
        Text(result.message, color = TextGray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        if (result.phase != IntentPhase.DONE) {
            Text("Fase: ${result.phase}", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 8.sp)
        }
        result.cliCommand?.let {
            Text("CLI: $it", color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        }
        if (result.cliResponse.isNotBlank()) {
            Text(
                result.cliResponse.take(600),
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private data class ToolEntry(val title: String, val subtitle: String, val route: String, val icon: ImageVector)

@Composable
fun ToolsHubScreen(
    onNavigate: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    isRootTab: Boolean = false
) {
    val tools = listOf(
        ToolEntry("Flash Bruce", "USB · releases oficiales o .bin custom", "firmware_flash", Icons.Default.SystemUpdate),
        ToolEntry("Capturar Sub-GHz", "Async · forma de onda · replay", "subghz_analyzer", Icons.Default.GraphicEq),
        ToolEntry("BadUSB", "Bloques visuales → pipeline automático", "badusb_forge", Icons.Default.Usb),
        ToolEntry("Buscar IR", "IRDB · ir tx_from_file", "ir_search", Icons.Default.Search),
        ToolEntry("Escuchar IR", "ir rx → biblioteca", "ir_finder", Icons.Default.Sensors),
        ToolEntry("WiFi / BLE en device", "Plantillas + abrir menú T-Embed", "spam_generator", Icons.Default.Wifi)
    )

    Column(
        Modifier.fillMaxSize().background(BlackAMOLED).padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isRootTab && onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = MatrixGreen)
                }
            }
            Text(
                if (isRootTab) "// HERRAMIENTAS" else "// HERRAMIENTAS",
                color = MatrixGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            "Intenciones Bruce — sin escribir CLI manualmente",
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        tools.forEach { tool ->
            ToolHubCard(tool) { onNavigate(tool.route) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ToolHubCard(tool: ToolEntry, onClick: () -> Unit) {
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
        Icon(tool.icon, tool.title, tint = NeonCyan, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tool.title.uppercase(), color = MatrixGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(tool.subtitle, color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextGray)
    }
}
