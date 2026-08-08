package com.embedsuite.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.engine.autopilot.AutopilotEvent
import com.embedsuite.app.engine.autopilot.AutopilotProfile
import com.embedsuite.app.engine.autopilot.TehLinkAutopilotEngine
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutopilotScreen(
    connectionManager: DeviceConnectionManager,
    appScope: CoroutineScope,
    onBack: () -> Unit
) {
    var selectedProfile by remember { mutableStateOf(AutopilotProfile.AUDIT) }
    val engine = remember(selectedProfile) {
        TehLinkAutopilotEngine(connectionManager, appScope, selectedProfile)
    }
    val eventLog = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    val running = engine.isRunning()

    DisposableEffect(engine) {
        onDispose { engine.stop() }
    }

    LaunchedEffect(engine) {
        engine.events.collect { event ->
            eventLog.add(formatAutopilotEvent(event))
            if (eventLog.size > 200) {
                eventLog.removeAt(0)
            }
        }
    }

    LaunchedEffect(eventLog.size) {
        if (eventLog.isNotEmpty()) {
            listState.animateScrollToItem(eventLog.lastIndex)
        }
    }

    Scaffold(
        containerColor = BlackAMOLED,
        topBar = {
            TopAppBar(
                title = {
                    Text("AUTOPILOT", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MatrixGreen)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            Text(
                "PERFIL",
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AutopilotProfile.entries.forEach { profile ->
                    FilterChip(
                        selected = selectedProfile == profile,
                        onClick = {
                            if (!running) {
                                selectedProfile = profile
                            }
                        },
                        enabled = !running,
                        label = {
                            Text(profile.name, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MatrixGreen.copy(alpha = 0.2f),
                            selectedLabelColor = MatrixGreen,
                            labelColor = TextGray
                        )
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { engine.start() },
                    enabled = !running,
                    colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = BlackAMOLED)
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("START", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = { engine.stop() },
                    enabled = running,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed)
                ) {
                    Icon(Icons.Default.Stop, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("STOP", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                Text(
                    if (running) "● RUNNING" else "○ IDLE",
                    color = if (running) MatrixGreen else TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "EVENT LOG",
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(DarkSurfaceElevated)
                    .padding(8.dp)
            ) {
                if (eventLog.isEmpty()) {
                    Text(
                        "Sin eventos — inicia autopilot",
                        color = TextGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                } else {
                    LazyColumn(state = listState) {
                        items(eventLog) { line ->
                            Text(
                                line,
                                color = when {
                                    line.contains("RISK", ignoreCase = true) -> NeonOrange
                                    line.contains("FAIL", ignoreCase = true) -> NeonRed
                                    line.contains("STOP", ignoreCase = true) -> TextMuted
                                    else -> MatrixGreen
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatAutopilotEvent(event: AutopilotEvent): String {
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    return when (event) {
        is AutopilotEvent.Tick ->
            "[$time] TICK · ${event.profile.name} → ${event.action}"
        is AutopilotEvent.RiskDetected ->
            "[$time] RISK · score=${event.score} · ${event.label}"
        is AutopilotEvent.SoftFailure ->
            "[$time] FAIL · ${event.action}: ${event.message}"
        is AutopilotEvent.Stopped ->
            "[$time] STOP · ${event.profile.name}"
    }
}
