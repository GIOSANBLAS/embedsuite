package com.embedsuite.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.connection.TehLinkHardeningInfo
import com.embedsuite.app.ui.theme.MatrixGreen
import com.embedsuite.app.ui.theme.NeonOrange
import com.embedsuite.app.ui.theme.NeonRed
import com.embedsuite.app.ui.theme.TextGray
import com.embedsuite.app.ui.theme.TextMuted

data class HardeningUiState(
    val secureBoot: Boolean = false,
    val flashEncryption: Boolean = false,
    val nvsIntegrity: Boolean = false,
    val twdt: Boolean = false,
    val bod: Boolean = false,
    val stackCanaries: Boolean = false
)

fun TehLinkHardeningInfo.toUiState(): HardeningUiState = HardeningUiState(
    secureBoot = secureBoot,
    flashEncryption = flashEncryption,
    nvsIntegrity = nvsEncryption,
    twdt = twdtEnabled,
    bod = bodEnabled,
    stackCanaries = stackCanaries || heapPoisoning
)

@Composable
fun HardeningPanel(
    state: HardeningUiState,
    modifier: Modifier = Modifier,
    title: String = "HARDENING"
) {
    val flags = listOf(
        "Secure Boot" to state.secureBoot,
        "Flash Enc" to state.flashEncryption,
        "NVS Enc" to state.nvsIntegrity,
        "TWDT" to state.twdt,
        "BOD" to state.bod,
        "Stack" to state.stackCanaries
    )
    val hints = buildCorrectiveHints(state)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (hints.isEmpty()) MatrixGreen else NeonOrange
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            flags.forEach { (label, enabled) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TrafficLightDot(enabled = enabled)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 7.sp,
                        color = TextMuted
                    )
                }
            }
        }
        hints.forEach { hint ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = hint,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = TextGray,
                lineHeight = 11.sp
            )
        }
    }
}

@Composable
private fun TrafficLightDot(enabled: Boolean) {
    val color = when {
        enabled -> MatrixGreen
        else -> NeonRed.copy(alpha = 0.85f)
    }
    Spacer(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = if (enabled) 0.9f else 0.5f))
    )
}

private fun buildCorrectiveHints(state: HardeningUiState): List<String> {
    val hints = mutableListOf<String>()
    if (!state.twdt) hints += "Enable Task Watchdog (TWDT) in firmware build flags."
    if (!state.bod) hints += "Enable Brownout Detector (BOD) to avoid flash corruption."
    if (!state.secureBoot) hints += "Secure Boot V2 is off — OTA images are not signature-enforced."
    if (!state.flashEncryption) hints += "Flash encryption disabled — sensitive data may be readable."
    if (!state.nvsIntegrity) hints += "NVS encryption off — WiFi credentials stored in plaintext NVS."
    if (!state.stackCanaries) hints += "Stack canaries / heap poisoning not reported active."
    return hints
}
