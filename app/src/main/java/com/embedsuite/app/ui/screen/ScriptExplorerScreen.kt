package com.embedsuite.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.scripting.Script
import com.embedsuite.app.scripting.ScriptCategory
import com.embedsuite.app.ui.components.GlassCard
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.EmbedViewModelFactory
import com.embedsuite.app.ui.viewmodel.ScriptRunState
import com.embedsuite.app.ui.viewmodel.ScriptExplorerViewModel

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = MatrixGreen,
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun ScriptExplorerScreen(
    factory: EmbedViewModelFactory,
    onNavigateTools: () -> Unit = {}
) {
    val vm: ScriptExplorerViewModel = viewModel(factory = factory)
    val scripts by vm.scripts.collectAsStateWithLifecycle()
    val search by vm.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by vm.selectedCategory.collectAsStateWithLifecycle()
    val selectedScript by vm.selectedScript.collectAsStateWithLifecycle()
    val running by vm.runningScripts.collectAsStateWithLifecycle()
    val connection by vm.connectionState.collectAsStateWithLifecycle()
    val auditLocked by vm.auditModeEnabled.collectAsStateWithLifecycle()
    val categories by vm.availableCategories.collectAsStateWithLifecycle()
    val paramOverrides by vm.paramOverrides.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(BlackAMOLED)) {
        OutlinedTextField(
            value = search,
            onValueChange = vm::search,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, "Buscar", tint = MatrixGreen) },
            label = { Text("Buscar scripts", color = TextGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MatrixGreen, unfocusedBorderColor = TextGray.copy(0.5f),
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
            )
        )

        Text(
            "Presets TEH-Link: cada script llama run_action en el firmware. Ofensivas: Ajustes → Modo Auditoría.",
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )

        AssistChipRow(
            modifier = Modifier.padding(horizontal = 12.dp),
            allLabel = "TODOS",
            values = categories,
            selected = selectedCategory,
            onSelect = { vm.pickCategory(it) }
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val (txt, color) = when (connection) {
                is ConnectionState.Connected -> "LINK OK" to MatrixGreen
                ConnectionState.Connecting -> "SYNC…" to NeonOrange
                ConnectionState.Disconnected -> "OFFLINE" to NeonRed
                is ConnectionState.Error -> "ERROR LINK" to NeonRed
            }
            BadgeBox { Text(txt, color = color, fontFamily = FontFamily.Monospace, fontSize = 10.sp) }
            if (!auditLocked) {
                BadgeBox { Text("AUDIT LOCK", color = NeonRed, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.weight(1f))
            Text("${scripts.size} scripts", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(scripts, key = { it.id }) { script ->
                ScriptCard(
                    script = script,
                    state = running[script.id] ?: ScriptRunState.Idle,
                    expanded = selectedScript?.id == script.id,
                    paramOverrides = paramOverrides,
                    onToggle = { vm.pickScript(script.id) },
                    onRun = { vm.run(script) },
                    onParamChange = { k, v -> vm.overrideParam(k, v) }
                )
            }
            item {
                Spacer(Modifier.height(80.dp))
                OutlinedButton(onClick = onNavigateTools, Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Map, "Macros", Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Abrir MACROS / Map Tools", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun AssistChipRow(
    modifier: Modifier,
    allLabel: String,
    values: List<ScriptCategory>,
    selected: ScriptCategory?,
    onSelect: (ScriptCategory?) -> Unit
) {
    var showScrim by rememberSaveable { mutableStateOf(false) }
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AssistChip(
            onClick = { onSelect(null) },
            label = { Text(allLabel, fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
            leadingIcon = if (selected == null) { { Icon(Icons.Default.Done, null, Modifier.size(14.dp)) } } else null,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (selected == null) MatrixGreen.copy(0.14f) else DarkSurface
            ),
            border = BorderStroke(1.dp, MatrixGreen.copy(0.35f))
        )
        values.forEach { cat ->
            AssistChip(
                onClick = { onSelect(cat); showScrim = !showScrim },
                label = { Text(cat.label, fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                leadingIcon = if (selected == cat) { { Icon(Icons.Default.Done, null, Modifier.size(14.dp)) } } else null,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selected == cat) MatrixGreen.copy(0.14f) else DarkSurface
                ),
                border = BorderStroke(1.dp, MatrixGreen.copy(0.35f))
            )
        }
    }
}

@Composable
private fun ScriptCard(
    script: Script,
    state: ScriptRunState,
    expanded: Boolean,
    paramOverrides: Map<String, String>,
    onToggle: () -> Unit,
    onRun: () -> Unit,
    onParamChange: (key: String, value: String) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val icon: ImageVector = when (script.category) {
                    ScriptCategory.EVIL_PORTAL -> Icons.Default.WifiTethering
                    ScriptCategory.BEACON_SPAM -> Icons.Default.BlurOn
                    ScriptCategory.RECON -> Icons.Default.Info
                    ScriptCategory.RF -> Icons.Default.SettingsInputAntenna
                    ScriptCategory.IR -> Icons.Default.Sensors
                    ScriptCategory.NFC -> Icons.Default.Nfc
                    ScriptCategory.CRYPTO -> Icons.Default.EnhancedEncryption
                    ScriptCategory.BADUSB -> Icons.Default.Usb
                    ScriptCategory.WIFI -> Icons.Default.Wifi
                    ScriptCategory.TEHLINK_JS -> Icons.Default.Code
                    ScriptCategory.BLE_SPAM -> Icons.Default.BluetoothSearching
                    ScriptCategory.WIFI_OFFENSIVE -> Icons.Default.WifiPassword
                    ScriptCategory.MOUSEJACK -> Icons.Default.Mouse
                    ScriptCategory.SUBGHZ_TOOLS -> Icons.Default.Tune
                    ScriptCategory.NFC_CLONE -> Icons.Default.ContentCopy
                }
                Icon(icon, script.category.label, Modifier.size(20.dp), tint = categoryTint(script.category))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        script.title,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis, maxLines = 1
                    )
                    Text(
                        "${script.category.label}  ·  ${script.dialect.name}",
                        color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp
                    )
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        "expand", tint = MatrixGreen
                    )
                }
                if (script.requiresAuditUnlock) {
                    Icon(Icons.Default.Lock, "Audit lock", Modifier.size(16.dp), tint = NeonRed)
                }
            }
            Text(
                script.summary,
                Modifier.fillMaxWidth().padding(top = 4.dp),
                color = TextGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                overflow = TextOverflow.Ellipsis, maxLines = if (expanded) 5 else 2
            )

            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                when (state) {
                    ScriptRunState.Idle -> {}
                    ScriptRunState.Running ->
                        Text("⟳ ejecutando…", color = NeonOrange, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    is ScriptRunState.Done ->
                        Text("✓ ${state.summary}", color = MatrixGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    is ScriptRunState.Error ->
                        Text("✗ ${state.message}", color = NeonRed, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    is ScriptRunState.Blocked ->
                        Text("🛡 ${state.message}", color = NeonOrange, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onRun,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = BlackAMOLED),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, "run", Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("RUN", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MatrixGreen.copy(alpha = 0.25f))
                SectionLabel("Parámetros")
                if (script.parameters.isEmpty()) {
                    Text("Sin parámetros expuestos; plugin/acción usan defaults internos.",
                        Modifier.padding(vertical = 6.dp),
                        color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                } else {
                    script.parameters.forEach { p ->
                        OutlinedTextField(
                            value = paramOverrides[p.key].orEmpty(),
                            onValueChange = { v -> onParamChange(p.key, v) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            label = { Text("${p.label}  [${p.key}]", color = TextGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                            placeholder = { Text(p.default.ifBlank { "default" }, fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MatrixGreen,
                                unfocusedBorderColor = TextGray.copy(0.5f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            isError = p.required && paramOverrides[p.key].isNullOrBlank() && p.default.isBlank()
                        )
                    }
                }

                if (state is ScriptRunState.Done && state.dataJson.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("Respuesta TEH-Link · data")
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(
                            BlackAMOLED.copy(alpha = 0.65f)
                        ).padding(8.dp)
                    ) {
                        Text(
                            state.dataJson,
                            Modifier.fillMaxWidth(),
                            fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MatrixGreen
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoChip("plugin", script.pluginId.ifBlank { "—" })
                    InfoChip("action", script.action.ifBlank { "sequence" })
                    if (script.defaultParams.isNotEmpty()) {
                        InfoChip("defaults", "${script.defaultParams.size} keys")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = DarkSurface,
        border = BorderStroke(0.7.dp, MatrixGreen.copy(0.35f))
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 8.sp, modifier = Modifier.padding(end = 4.dp))
            Text(value.take(20), color = MatrixGreen, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BadgeBox(content: @Composable RowScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurface,
        border = BorderStroke(0.6.dp, MatrixGreen.copy(0.3f)),
        content = {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                content()
            }
        }
    )
}

private fun categoryTint(cat: ScriptCategory) = when (cat) {
    ScriptCategory.EVIL_PORTAL -> NeonRed
    ScriptCategory.BEACON_SPAM -> NeonOrange
    ScriptCategory.RECON -> MatrixGreen
    ScriptCategory.RF -> MatrixGreen
    ScriptCategory.IR -> NeonCyan
    ScriptCategory.NFC -> NeonCyan
    ScriptCategory.CRYPTO -> NeonPurple
    ScriptCategory.BADUSB -> NeonOrange
    ScriptCategory.WIFI -> NeonCyan
    ScriptCategory.TEHLINK_JS -> MatrixGreen
    ScriptCategory.BLE_SPAM -> NeonPurple
    ScriptCategory.WIFI_OFFENSIVE -> NeonRed
    ScriptCategory.MOUSEJACK -> NeonOrange
    ScriptCategory.SUBGHZ_TOOLS -> KaliBlue
    ScriptCategory.NFC_CLONE -> MatrixGreen
}
