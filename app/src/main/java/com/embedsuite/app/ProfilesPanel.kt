package com.embedsuite.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.data.ProfileEntity
import com.embedsuite.app.data.ProfileRepository
import com.embedsuite.app.macro.MacroEngine
import com.embedsuite.app.ui.components.*
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfilesPanel(
    profileRepository: ProfileRepository,
    macroEngine: MacroEngine,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val profiles by profileRepository.allProfiles.collectAsState(initial = emptyList())
    var filter by remember { mutableStateOf("ALL") }
    var status by remember { mutableStateOf("${profiles.size} perfiles Bruce listos") }

    val categories = listOf("ALL", "RF", "IR", "NFC", "RECON", "SCENARIO")
    val filtered = if (filter == "ALL") profiles else profiles.filter { it.category == filter }

    GlassCard(accent = KaliBlue, modifier = modifier.fillMaxWidth()) {
        HackerSectionHeader("BRUCE PROFILES // CC1101", accent = KaliBlue)
        Text(status, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 6.dp)) {
            categories.forEach { cat ->
                GlassChip(label = cat, selected = filter == cat, onClick = { filter = cat }, accent = KaliBlue)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.heightIn(max = 280.dp)
        ) {
            items(filtered, key = { it.id }) { profile ->
                ProfileRow(
                    profile = profile,
                    enabled = isConnected,
                    onRun = {
                        scope.launch {
                            macroEngine.execute(
                                com.embedsuite.app.data.MacroEntity(
                                    name = profile.name,
                                    commands = profile.commands
                                )
                            ).fold(
                                onSuccess = { status = "OK: ${profile.name} ($it cmds)" },
                                onFailure = { status = "Error: ${it.message}" }
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(profile: ProfileEntity, enabled: Boolean, onRun: () -> Unit) {
    val accent = when (profile.category) {
        "RF" -> MatrixGreen
        "IR" -> NeonOrange
        "NFC" -> NeonCyan
        "SCENARIO" -> NeonRed
        else -> KaliBlue
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("[${profile.category}] ${profile.name}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = accent)
            Text(profile.description, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
        }
        androidx.compose.material3.IconButton(onClick = onRun, enabled = enabled) {
            Icon(Icons.Default.PlayArrow, "Run", tint = accent, modifier = Modifier.size(20.dp))
        }
    }
}
