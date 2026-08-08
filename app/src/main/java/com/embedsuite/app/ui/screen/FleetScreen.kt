package com.embedsuite.app.ui.screen

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.engine.fleet.FleetRegistry
import com.embedsuite.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetScreen(
    fleetRegistry: FleetRegistry,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var devices by remember { mutableStateOf(fleetRegistry.listDevices()) }
    val activeId = fleetRegistry.getActive()?.id
    var editingNickname by remember { mutableStateOf<String?>(null) }
    var nicknameDraft by remember { mutableStateOf("") }

    Scaffold(
        containerColor = BlackAMOLED,
        topBar = {
            TopAppBar(
                title = {
                    Text("FLEET", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MatrixGreen)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = NeonCyan)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val json = fleetRegistry.exportInventoryJson()
                        context.startActivity(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_TEXT, json)
                            }
                        )
                    }) {
                        Icon(Icons.Default.Share, "Exportar", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            if (devices.isEmpty()) {
                Text(
                    "Sin perfiles — conecta un T-Embed para registrar",
                    color = TextGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(devices, key = { it.id }) { device ->
                        FleetDeviceRow(
                            device = device,
                            isActive = device.id == activeId,
                            onSetActive = {
                                fleetRegistry.setActive(device.id)
                                devices = fleetRegistry.listDevices()
                            },
                            onEditNickname = {
                                editingNickname = device.id
                                nicknameDraft = device.nickname
                            }
                        )
                    }
                }
            }
        }
    }

    if (editingNickname != null) {
        AlertDialog(
            onDismissRequest = { editingNickname = null },
            containerColor = DarkSurface,
            title = {
                Text("Nickname", color = MatrixGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            },
            text = {
                OutlinedTextField(
                    value = nicknameDraft,
                    onValueChange = { nicknameDraft = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MatrixGreen,
                        focusedTextColor = NeonCyan
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editingNickname?.let { id ->
                        fleetRegistry.setNickname(id, nicknameDraft)
                        devices = fleetRegistry.listDevices()
                    }
                    editingNickname = null
                    Toast.makeText(context, "Nickname actualizado", Toast.LENGTH_SHORT).show()
                }) {
                    Text("OK", color = MatrixGreen, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNickname = null }) {
                    Text("Cancelar", color = TextGray, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
private fun FleetDeviceRow(
    device: FleetRegistry.FleetDevice,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onEditNickname: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(DarkSurfaceElevated)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    device.nickname,
                    color = if (isActive) MatrixGreen else TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isActive) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.CheckCircle, null, tint = MatrixGreen, modifier = Modifier.size(14.dp))
                }
            }
            Text(
                "${device.hardwareKind} · ${device.firmwareVersion.ifBlank { "?" }}",
                color = TextGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
            Text(device.id, color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 8.sp)
        }
        TextButton(onClick = onEditNickname) {
            Text("NICK", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonCyan)
        }
        if (!isActive) {
            TextButton(onClick = onSetActive) {
                Text("ACTIVO", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MatrixGreen)
            }
        }
    }
}
