package com.embedsuite.app.ui.screen

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.ProbeSnifferViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProbeSnifferScreen(
    connectionManager: DeviceConnectionManager,
    factory: androidx.lifecycle.ViewModelProvider.Factory,
    onBack: () -> Unit
) {
    val vm: ProbeSnifferViewModel = viewModel(factory = factory)
    val probes by vm.probes.collectAsStateWithLifecycle()
    val running by vm.running.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var chField by remember { mutableStateOf("1,6,11") }

    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    SideEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perm.launch(arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    LaunchedEffect(Unit) {
        vm.toast.collect { t ->
            Toast.makeText(ctx, t, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = BlackAMOLED,
        topBar = {
            TopAppBar(
                title = {
                    Text("WiFi Probe Sniffer", fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MatrixGreen)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = chField,
                    onValueChange = { chField = it },
                    label = { Text("Hopping (CSV ints)",
                        fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MatrixGreen, unfocusedBorderColor = DarkSurface,
                        focusedTextColor = MatrixGreen, unfocusedTextColor = TextGray
                    )
                )
                Button(onClick = { vm.start(chField) }, enabled = !running,
                    modifier = Modifier.height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen,
                        contentColor = BlackAMOLED)) {
                    Icon(Icons.Default.PlayArrow, null)
                }
                Spacer(Modifier.width(6.dp))
                Button(onClick = { vm.stop() }, enabled = running,
                    modifier = Modifier.height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed,
                        contentColor = Color.White)) {
                    Icon(Icons.Default.Stop, null)
                }
                Spacer(Modifier.width(6.dp))
                OutlinedButton(onClick = { vm.flush() }, modifier = Modifier.height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)) {
                    Icon(Icons.Default.Refresh, null)
                }
                Spacer(Modifier.width(6.dp))
                OutlinedButton(onClick = { vm.clear() }, modifier = Modifier.height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonOrange)) {
                    Icon(Icons.Default.DeleteSweep, null)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                Badge(containerColor = if (running) MatrixGreen else TextGray)
                Spacer(Modifier.width(8.dp))
                Text(if (running) "CAPTURANDO · auto-off 5 min" else "IDLE",
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                    color = if (running) MatrixGreen else TextGray,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("Unique: ${probes.groupBy { it.mac }.keys.size} · Total: ${probes.sumOf { it.count }}",
                    fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray)
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = DarkSurface)
            Spacer(Modifier.height(6.dp))
            if (probes.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("No probes capturadas aún. Pulsa PLAY para empezar.",
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextGray)
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(probes, key = { it.mac + it.ssid }) { p ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Row {
                                Icon(Icons.Default.Wifi, null, tint = when {
                                    p.rssi >= -55 -> MatrixGreen
                                    p.rssi >= -70 -> NeonCyan
                                    p.rssi >= -85 -> NeonOrange
                                    else -> NeonRed
                                }, modifier = Modifier.size(18.dp).align(Alignment.CenterVertically))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = p.ssid,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White, fontSize = 12.sp
                                )
                                Spacer(Modifier.weight(1f))
                                Text("×${p.count}",
                                    fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TagChip("MAC", p.mac.uppercase(), MatrixGreen)
                                TagChip("Ch", p.channel.toString(), NeonCyan)
                                TagChip("RSSI", "${p.rssi} dBm", NeonOrange)
                                TagChip("OUI", p.vendor, NeonPurple)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(label: String, value: String, color: Color) {
    AssistChip(
        onClick = {},
        leadingIcon = {
            Text(label, fontFamily = FontFamily.Monospace,
                fontSize = 8.sp, color = color, fontWeight = FontWeight.Bold)
        },
        label = {
            Text(value, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White,
                maxLines = 1)
        },
        shape = RoundedCornerShape(40),
        colors = AssistChipDefaults.assistChipColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    )
}
