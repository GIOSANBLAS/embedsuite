package com.embedsuite.app.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.engine.customizer.CustomBuildRequest
import com.embedsuite.app.engine.customizer.FirmwareCustomizer
import com.embedsuite.app.ui.theme.*

private val CUSTOMIZER_MODULES = listOf("wifi", "ble", "subghz", "nfc", "ir", "badusb", "rfid")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirmwareCustomizerScreen(
    firmwareCustomizer: FirmwareCustomizer,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedModules by remember { mutableStateOf(CUSTOMIZER_MODULES.toSet()) }
    var manifest by remember { mutableStateOf<String?>(null) }
    var jobStatus by remember { mutableStateOf<String?>(null) }

    val request = CustomBuildRequest(modules = selectedModules)
    val ramSavings = firmwareCustomizer.estimateRamSavings(request)

    Scaffold(
        containerColor = BlackAMOLED,
        topBar = {
            TopAppBar(
                title = {
                    Text("FW CUSTOMIZER", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MatrixGreen)
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "MÓDULOS",
                color = MatrixGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            CUSTOMIZER_MODULES.forEach { module ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(module.uppercase(), color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Checkbox(
                        checked = module in selectedModules,
                        onCheckedChange = { checked ->
                            selectedModules = if (checked) selectedModules + module else selectedModules - module
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MatrixGreen,
                            uncheckedColor = TextGray
                        )
                    )
                }
            }
            Text(
                "~${ramSavings} KB RAM ahorro estimado",
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { manifest = firmwareCustomizer.generateManifestJson(request) },
                    colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = BlackAMOLED)
                ) {
                    Icon(Icons.Default.Description, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("MANIFEST", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = {
                        val job = firmwareCustomizer.queueLocalBuild(request)
                        jobStatus = "${job.status} · ${job.jobId} — ${job.message}"
                        Toast.makeText(context, job.message, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                ) {
                    Icon(Icons.Default.Build, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("QUEUE BUILD", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
            jobStatus?.let {
                Text(it, color = NeonOrange, fontFamily = FontFamily.Monospace, fontSize = 9.sp, modifier = Modifier.padding(top = 8.dp))
            }
            manifest?.let { text ->
                Spacer(Modifier.height(10.dp))
                Text(
                    "MANIFEST JSON",
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceElevated)
                        .padding(8.dp)
                ) {
                    Text(text, color = MatrixGreen, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
            }
        }
    }
}
