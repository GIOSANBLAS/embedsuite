package com.embedsuite.app.ui.screen



import androidx.compose.foundation.layout.*

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.SpanStyle

import androidx.compose.ui.text.buildAnnotatedString

import androidx.compose.ui.text.font.FontFamily

import androidx.compose.ui.text.withStyle

import androidx.compose.ui.unit.dp

import com.embedsuite.app.AppContainer

import com.embedsuite.app.core.bruce.BruceLimits

import com.embedsuite.app.engine.payload.DuckyEditor

import com.embedsuite.app.engine.payload.PayloadTemplates

import com.embedsuite.app.ui.theme.*

import kotlinx.coroutines.launch



/** Módulo D — Forja DuckyScript (solo CLI Bruce documentado). */

@Composable

fun PayloadForgeScreen(
    container: AppContainer,
    developerMode: Boolean = false,
    onOpenScripts: () -> Unit,
    onOpenOps: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenBruceFiles: () -> Unit = {},
    onOpenTools: () -> Unit = {}
) {

    var duckyScript by remember { mutableStateOf("REM EmbedSuite payload\nSTRING Hello from Bruce\nENTER\n") }

    var badUsbPath by remember { mutableStateOf(PayloadTemplates.badUsbFilePath("payload.txt")) }

    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("") }

    val issues = remember(duckyScript) { DuckyEditor.validate(duckyScript) }



    Column(

        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),

        verticalArrangement = Arrangement.spacedBy(12.dp)

    ) {

        Text("Forja", style = MaterialTheme.typography.headlineSmall, color = EmbedGreen)

        Text(

            "DuckyScript vía CLI Bruce (badusb run_from_file). Evil Portal y BLE Spam solo en el menú del T-Embed.",

            color = TextMuted,

            style = MaterialTheme.typography.bodySmall

        )



        Text("DuckyScript", color = NeonCyan, style = MaterialTheme.typography.labelMedium)

        issues.take(3).forEach { issue ->

            Text("L${issue.line}: ${issue.message}", color = NeonOrange, fontSize = MaterialTheme.typography.bodySmall.fontSize)

        }

        OutlinedTextField(

            value = duckyScript,

            onValueChange = { duckyScript = it },

            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 200.dp),

            label = { Text("Script (editor local)") },

            colors = OutlinedTextFieldDefaults.colors(

                focusedTextColor = TextPrimary,

                unfocusedTextColor = TextPrimary,

                focusedBorderColor = EmbedGreen,

                cursorColor = EmbedCyan

            )

        )

        OutlinedTextField(

            value = badUsbPath,

            onValueChange = { badUsbPath = it },

            modifier = Modifier.fillMaxWidth(),

            label = { Text("Ruta en SD del T-Embed (.txt)") },

            supportingText = { Text(DuckyEditor.remoteExecutionHint, color = TextMuted) }

        )



        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            Button(
                onClick = {
                    scope.launch {
                        status = "Subiendo vía WiFi WebUI…"
                        val tmp = java.io.File.createTempFile("embed_ducky_", ".txt", container.connectionManager.applicationContext().cacheDir)
                        tmp.writeText(duckyScript)
                        container.connectionManager.uploadFileToDevice(tmp, badUsbPath).fold(
                            onSuccess = { status = "Subido: $badUsbPath — $it" },
                            onFailure = { status = it.message ?: "Upload falló" }
                        )
                        tmp.delete()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmbedCyan.copy(alpha = 0.2f), contentColor = EmbedCyan)
            ) {
                Icon(Icons.Default.CloudUpload, null)
                Spacer(Modifier.width(4.dp))
                Text("Subir WiFi")
            }

            Button(
                onClick = {
                    scope.launch {
                        status = "Subiendo y ejecutando…"
                        val tmp = java.io.File.createTempFile("embed_ducky_", ".txt", container.connectionManager.applicationContext().cacheDir)
                        tmp.writeText(duckyScript)
                        container.connectionManager.uploadFileToDevice(tmp, badUsbPath).fold(
                            onSuccess = {
                                container.connectionManager.runBadUsbFromFile(badUsbPath).fold(
                                    onSuccess = { msg -> status = "OK: $msg" },
                                    onFailure = { err -> status = err.message ?: "badusb falló" }
                                )
                            },
                            onFailure = { status = it.message ?: "Upload falló" }
                        )
                        tmp.delete()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmbedGreen.copy(alpha = 0.2f), contentColor = EmbedGreen)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(4.dp))
                Text("Subir + ejecutar")
            }

            OutlinedButton(onClick = onOpenTerminal) { Text("CLI") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenTools) { Text("Herramientas", color = MatrixGreen) }
            if (developerMode) {
                OutlinedButton(onClick = onOpenScripts) { Text("Scripts") }
                OutlinedButton(onClick = onOpenBruceFiles) { Text("Archivos SD", color = NeonCyan) }
                OutlinedButton(onClick = onOpenTerminal) { Text("CLI") }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(BruceLimits.WIFI_UPLOAD_HINT, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }



        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {

            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                Text("No disponible por CLI Bruce", color = NeonOrange, style = MaterialTheme.typography.titleSmall)

                Text(BruceLimits.NO_CLI, color = TextMuted, style = MaterialTheme.typography.bodySmall)

                Text("• Evil Portal — menú WiFi del T-Embed", color = TextMuted, style = MaterialTheme.typography.bodySmall)

                Text("• BLE Spam — menú BLE del T-Embed", color = TextMuted, style = MaterialTheme.typography.bodySmall)

            }

        }



        if (status.isNotBlank()) {

            Text(status, color = NeonCyan, style = MaterialTheme.typography.bodySmall)

        }

    }

}


