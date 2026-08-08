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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.core.error.HumanErrorMapper
import com.embedsuite.app.engine.config.BruceConfigSync
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BruceConfigScreen(
    bruceConfigSync: BruceConfigSync,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var jsonText by remember { mutableStateOf(bruceConfigSync.getLocalShadow()) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BlackAMOLED,
        topBar = {
            TopAppBar(
                title = {
                    Text("BRUCE CONFIG", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MatrixGreen)
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
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OpsActionButton("PULL", Icons.Default.CloudDownload, busy) {
                    scope.launch {
                        busy = true
                        bruceConfigSync.pullFromDevice().fold(
                            onSuccess = {
                                jsonText = it
                                Toast.makeText(context, "bruce.json sincronizado", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = {
                                Toast.makeText(context, HumanErrorMapper.map(it), Toast.LENGTH_LONG).show()
                            }
                        )
                        busy = false
                    }
                }
                OpsActionButton("PUSH", Icons.Default.CloudUpload, busy) {
                    scope.launch {
                        busy = true
                        bruceConfigSync.pushToDevice(jsonText).fold(
                            onSuccess = { Toast.makeText(context, "Config enviada", Toast.LENGTH_SHORT).show() },
                            onFailure = {
                                Toast.makeText(context, HumanErrorMapper.map(it), Toast.LENGTH_LONG).show()
                            }
                        )
                        busy = false
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OpsActionButton("BACKUP", Icons.Default.Save, busy) {
                    bruceConfigSync.backupToFile(context).fold(
                        onSuccess = {
                            Toast.makeText(context, "Backup: ${it.name}", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = {
                            Toast.makeText(context, HumanErrorMapper.map(it), Toast.LENGTH_LONG).show()
                        }
                    )
                }
                OpsActionButton("RESTORE", Icons.Default.Restore, busy) {
                    bruceConfigSync.restoreFromString(jsonText).fold(
                        onSuccess = { Toast.makeText(context, "Shadow local restaurado", Toast.LENGTH_SHORT).show() },
                        onFailure = {
                            Toast.makeText(context, HumanErrorMapper.map(it), Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = jsonText,
                onValueChange = { jsonText = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp),
                label = { Text("bruce.json", color = TextGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = NeonCyan
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MatrixGreen,
                    unfocusedBorderColor = TextGray.copy(alpha = 0.4f)
                )
            )
        }
    }
}

@Composable
private fun OpsActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    busy: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !busy,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
    ) {
        Icon(icon, null, Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
    }
}
