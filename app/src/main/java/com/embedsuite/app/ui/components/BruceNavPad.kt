package com.embedsuite.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.ui.theme.*

/** Pad mínimo de navegación CLI Bruce — no espejo de pantalla del T-Embed. */
@Composable
fun BruceNavPad(
    enabled: Boolean,
    onNav: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (!compact) {
            Text(
                "NAV REMOTO",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        NavIconButton(enabled, Icons.Default.KeyboardArrowUp, "nav up") { onNav("nav up") }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            NavIconButton(enabled, Icons.AutoMirrored.Filled.ArrowBack, "nav prev") { onNav("nav prev") }
            NavIconButton(enabled, Icons.Default.RadioButtonChecked, "nav select") { onNav("nav select") }
            NavIconButton(enabled, Icons.Default.KeyboardArrowRight, "nav next") { onNav("nav next") }
        }
        NavIconButton(enabled, Icons.Default.KeyboardArrowDown, "nav down") { onNav("nav down") }
        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = { onNav("nav esc") },
            enabled = enabled,
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text("ESC", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NeonOrange)
        }
    }
}

@Composable
private fun NavIconButton(
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Icon(icon, desc, tint = if (enabled) MatrixGreen else TextMuted)
    }
}
