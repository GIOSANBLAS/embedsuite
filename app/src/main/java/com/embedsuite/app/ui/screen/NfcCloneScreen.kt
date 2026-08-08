package com.embedsuite.app.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.embedsuite.app.ui.viewmodel.NfcCloneViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcCloneScreen(
    connectionManager: DeviceConnectionManager,
    factory: androidx.lifecycle.ViewModelProvider.Factory,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm: NfcCloneViewModel = viewModel(factory = factory)
    val s by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.toast.collect { t -> Toast.makeText(ctx, t, Toast.LENGTH_SHORT).show() }
    }

    var tabIdx by remember { mutableIntStateOf(0) }
    val tabs = listOf("Mifare 1K", "NTAG URL", "NTAG WiFi")

    Scaffold(
        containerColor = BlackAMOLED,
        topBar = {
            TopAppBar(
                title = { Text("NFC Clone / Write", fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MatrixGreen) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    AssistChip(
                        onClick = { scope.launch {
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("dump", s.dumpHex))
                            Toast.makeText(ctx, "Dump HEX copiado (${s.dumpHex.length} chars)",
                                Toast.LENGTH_SHORT).show()
                        } },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = NeonCyan) },
                        label = { Text("Copy dump", fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp, color = NeonCyan) }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp)) {
            AssistChip(
                onClick = {},
                leadingIcon = {
                    val c = when (s.step) {
                        "reading", "writing", "writing_url", "writing_wifi" -> NeonOrange
                        "read_done", "write_done" -> MatrixGreen
                        "error" -> NeonRed
                        else -> TextGray
                    }
                    Badge(containerColor = c)
                },
                label = {
                    val suffix = buildString {
                        s.sectorsRead.takeIf { it > 0 }?.let { append(" · $it/16 sectors") }
                        s.blocksWritten.takeIf { it > 0 }?.let { append(" · $it blocks w") }
                        s.lastUid.takeIf { it.isNotBlank() }?.let { append(" · UID $it") }
                    }
                    Text("Step: ${s.step.uppercase()}$suffix",
                        fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            TabRow(
                selectedTabIndex = tabIdx,
                containerColor = DarkSurface,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tabIdx]),
                        color = MatrixGreen
                    )
                }
            ) {
                tabs.forEachIndexed { i, t ->
                    Tab(
                        selected = tabIdx == i, onClick = { tabIdx = i },
                        selectedContentColor = MatrixGreen,
                        unselectedContentColor = TextGray,
                        text = { Text(t, fontFamily = FontFamily.Monospace, fontSize = 10.sp) }
                    )
                }
            }

            when (tabIdx) {
                0 -> MifareTab(vm, s, ctx)
                1 -> NtagUrlTab(vm, ctx)
                2 -> NtagWifiTab(vm, ctx)
            }
        }
    }
}

@Composable
private fun MifareTab(
    vm: NfcCloneViewModel,
    s: com.embedsuite.app.ui.viewmodel.NfcCloneUiState,
    ctx: Context
) {
    var keys by remember { mutableStateOf(
        "FFFFFFFFFFFF,A0A1A2A3A4A5,D3F7D3F7D3F7,000000000000,B0B1B2B3B4B5,4D3A99C351DD") }
    var dumpWrite by remember(s.dumpHex) { mutableStateOf(s.dumpHex) }
    var forceUid by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(10.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(10.dp)) {
                Text("READ MIFARE 1K", fontFamily = FontFamily.Monospace,
                    color = MatrixGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = keys, onValueChange = { keys = it },
                    label = { Text("Keys (CSV 12 hex c/u)",
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    minLines = 2, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MatrixGreen, focusedTextColor = Color.White,
                        unfocusedTextColor = TextGray
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { vm.readMifare(keys) },
                        colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen,
                            contentColor = BlackAMOLED)) {
                        Icon(Icons.Default.Savings, null)
                        Spacer(Modifier.width(6.dp))
                        Text("READ TAG", fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("UID: ${s.lastUid.ifBlank { "—" }}",
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(10.dp)) {
                Text("WRITE MIFARE 1K", fontFamily = FontFamily.Monospace,
                    color = NeonRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = dumpWrite, onValueChange = { dumpWrite = it },
                    label = { Text("dump_hex (1024 bytes = 2048 chars)",
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonRed, focusedTextColor = Color.White,
                        unfocusedTextColor = TextGray
                    )
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = forceUid, onCheckedChange = { forceUid = it })
                    Spacer(Modifier.width(6.dp))
                    Text("Sobrescribir UID block 0 (solo tarjetas CUID magic)",
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(onClick = { vm.writeMifare(dumpWrite, forceUid) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed,
                            contentColor = Color.White)) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(6.dp))
                        Text("WRITE TAG", fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("dump", dumpWrite))
                        Toast.makeText(ctx, "Clipboard: ${dumpWrite.length} chars",
                            Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentPaste, null, tint = NeonCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun NtagUrlTab(vm: NfcCloneViewModel, ctx: Context) {
    var url by remember { mutableStateOf("https://example.com") }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(10.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(10.dp)) {
                Text("WRITE NTAG NDEF URL", fontFamily = FontFamily.Monospace,
                    color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("URL (https://...)",
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan, focusedTextColor = Color.White,
                        unfocusedTextColor = TextGray
                    )
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = { vm.writeNtagUrl(url) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan,
                        contentColor = BlackAMOLED)) {
                    Icon(Icons.Default.Link, null)
                    Spacer(Modifier.width(6.dp))
                    Text("WRITE NTAG", fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Preview URL card (tap para abrir en Android):",
            fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
        Spacer(Modifier.height(4.dp))
        Card(shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NeonCyan.copy(alpha = 0.1f))) {
            Column(Modifier.padding(18.dp)) {
                Text(url, fontFamily = FontFamily.Monospace, color = Color.White,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NtagWifiTab(vm: NfcCloneViewModel, ctx: Context) {
    var ssid by remember { mutableStateOf("EmbedSuite") }
    var pass by remember { mutableStateOf("12345678") }
    var authIdx by remember { mutableIntStateOf(1) }
    val auths = listOf("Abierto", "WPA2 PSK", "WPA3 SAE")

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(10.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(10.dp)) {
                Text("WRITE NTAG WiFi QR (WSC)", fontFamily = FontFamily.Monospace,
                    color = NeonPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = ssid, onValueChange = { ssid = it },
                    label = { Text("SSID", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple, focusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = pass, onValueChange = { pass = it },
                    label = { Text("Password (8..63)",
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple, focusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(6.dp))
                Text("Auth:", fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp, color = TextGray)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    auths.forEachIndexed { i, a ->
                        FilterChip(
                            selected = authIdx == i, onClick = { authIdx = i },
                            label = { Text(a, fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPurple.copy(alpha = 0.2f),
                                selectedLabelColor = NeonPurple
                            )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = { vm.writeNtagWifi(ssid, pass, authIdx) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple,
                        contentColor = Color.White)) {
                    Icon(Icons.Default.QrCodeScanner, null)
                    Spacer(Modifier.width(6.dp))
                    Text("WRITE NTAG WiFi", fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("WiFi QR string (cámara Android 10+ lo lee como QR/texto):",
            fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
        Spacer(Modifier.height(4.dp))
        Card(shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NeonPurple.copy(alpha = 0.12f))) {
            val t = when (authIdx) {
                0 -> "WIFI:T:nopass;S:$ssid;;"
                1 -> "WIFI:T:WPA;S:$ssid;P:$pass;;"
                else -> "WIFI:T:SAE;S:$ssid;P:$pass;;"
            }
            Column(Modifier.padding(16.dp)) {
                Text(t, fontFamily = FontFamily.Monospace, color = Color.White,
                    fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("wifiqr", t))
                    Toast.makeText(ctx, "WiFi QR string copiado.", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, null, tint = NeonPurple)
                    Spacer(Modifier.width(4.dp))
                    Text("Copiar QR string", fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp, color = NeonPurple)
                }
            }
        }
    }
}
