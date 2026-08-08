package com.embedsuite.app.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.core.error.HumanErrorMapper
import com.embedsuite.app.engine.workflow.*
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.launch

private data class WorkflowListItem(
    val workflow: Workflow,
    val isBuiltIn: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowScreen(
    workflowStore: WorkflowStore,
    workflowEngine: WorkflowEngine,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stored by remember { mutableStateOf(workflowStore.listStored()) }
    var runResult by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }

    val items = remember(stored) {
        WorkflowCatalog.builtIns().map { WorkflowListItem(it, true) } +
            stored.map { WorkflowListItem(it, false) }
    }

    var exportPending by remember { mutableStateOf<Workflow?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()?.let { raw ->
            workflowStore.importRaw(raw).fold(
                onSuccess = {
                    stored = workflowStore.listStored()
                    Toast.makeText(context, "Workflow importado: ${it.name}", Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    Toast.makeText(context, HumanErrorMapper.map(it), Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val pending = exportPending
        if (pending == null) return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(workflowStore.exportRaw(pending).toByteArray())
        }
        Toast.makeText(context, "Exportado: ${pending.name}", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = BlackAMOLED,
        topBar = {
            TopAppBar(
                title = {
                    Text("WORKFLOWS", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MatrixGreen)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = NeonCyan)
                    }
                },
                actions = {
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) {
                        Icon(Icons.Default.Upload, "Importar", tint = NeonCyan)
                    }
                    IconButton(onClick = { stored = workflowStore.listStored() }) {
                        Icon(Icons.Default.Refresh, "Refrescar", tint = MatrixGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
            runResult?.let { msg ->
                Text(
                    msg,
                    color = if (msg.contains("OK")) MatrixGreen else NeonOrange,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            if (running) {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth(),
                    color = MatrixGreen,
                    trackColor = DarkSurfaceElevated
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { "${it.isBuiltIn}-${it.workflow.id}" }) { item ->
                    WorkflowRow(
                        item = item,
                        running = running,
                        onRun = {
                            scope.launch {
                                running = true
                                runResult = null
                                val result = workflowEngine.run(item.workflow)
                                runResult = if (result.success) {
                                    "OK · ${item.workflow.name}: ${result.completedSteps}/${result.totalSteps} — ${result.message}"
                                } else {
                                    "FAIL · ${result.message}"
                                }
                                running = false
                            }
                        },
                        onExport = {
                            exportPending = item.workflow
                            exportLauncher.launch("${item.workflow.id}.ewf")
                        },
                        onDelete = if (!item.isBuiltIn) {
                            {
                                workflowStore.delete(item.workflow.id)
                                stored = workflowStore.listStored()
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkflowRow(
    item: WorkflowListItem,
    running: Boolean,
    onRun: () -> Unit,
    onExport: () -> Unit,
    onDelete: (() -> Unit)?
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
                    item.workflow.name,
                    color = MatrixGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (item.isBuiltIn) "BUILTIN" else "STORED",
                    color = if (item.isBuiltIn) NeonCyan else NeonOrange,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp
                )
            }
            Text(
                item.workflow.description.ifBlank { item.workflow.id },
                color = TextGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
            Text(
                "${item.workflow.steps.size} pasos · ${item.workflow.trigger.name}",
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp
            )
        }
        IconButton(onClick = onExport, enabled = !running) {
            Icon(Icons.Default.Download, "Exportar", tint = NeonCyan)
        }
        onDelete?.let { delete ->
            IconButton(onClick = delete, enabled = !running) {
                Icon(Icons.Default.Delete, "Eliminar", tint = NeonRed)
            }
        }
        IconButton(onClick = onRun, enabled = !running) {
            Icon(Icons.Default.PlayArrow, "Ejecutar", tint = MatrixGreen)
        }
    }
}
