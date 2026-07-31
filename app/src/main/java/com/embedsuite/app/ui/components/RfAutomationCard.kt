package com.embedsuite.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.data.RfAutomationRepository
import com.embedsuite.app.data.RfAutomationRuleEntity
import com.embedsuite.app.rf.RfAutomationEngine
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun RfAutomationCard(
    repository: RfAutomationRepository,
    macroRepository: com.embedsuite.app.data.MacroRepository,
    modifier: Modifier = Modifier
) {
    val rules by repository.allRules.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NeonOrange.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "AUTOMATIZACIONES RF",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = NeonOrange
                )
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva regla", tint = MatrixGreen)
                }
            }
            Text(
                "Si protocolo/frecuencia coincide → acción automática",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (rules.isEmpty()) {
                Text(
                    "Sin reglas. Ej: PT2262 @ 433 → notificar",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextGray
                )
            } else {
                rules.forEach { rule ->
                    RfRuleRow(
                        rule = rule,
                        onToggle = { enabled ->
                            scope.launch {
                                repository.update(rule.copy(enabled = enabled))
                            }
                        },
                        onDelete = {
                            scope.launch { repository.delete(rule.id) }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddRfRuleDialog(
            macroRepository = macroRepository,
            onDismiss = { showAddDialog = false },
            onSave = { name, protocol, frequency, action, payload ->
                scope.launch {
                    repository.save(
                        RfAutomationRuleEntity(
                            name = name,
                            matchProtocol = protocol,
                            matchFrequency = frequency,
                            actionType = action,
                            actionPayload = payload
                        )
                    )
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun RfRuleRow(
    rule: RfAutomationRuleEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = rule.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = MatrixGreen)
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Text(rule.name, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
            val match = buildString {
                if (rule.matchProtocol.isNotBlank()) append(rule.matchProtocol)
                if (rule.matchFrequency.isNotBlank()) {
                    if (isNotEmpty()) append(" @ ")
                    append(rule.matchFrequency)
                }
                if (isEmpty()) append("cualquier señal")
            }
            Text(
                "$match → ${rule.actionType}${if (rule.actionPayload.isNotBlank()) ": ${rule.actionPayload}" else ""}",
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = TextGray
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = NeonRed)
        }
    }
}

@Composable
private fun AddRfRuleDialog(
    macroRepository: com.embedsuite.app.data.MacroRepository,
    onDismiss: () -> Unit,
    onSave: (name: String, protocol: String, frequency: String, action: String, payload: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var action by remember { mutableStateOf(RfAutomationEngine.ACTION_NOTIFY) }
    var payload by remember { mutableStateOf("") }
    val macros by macroRepository.allMacros.collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("Nueva regla RF", fontFamily = FontFamily.Monospace, color = NeonOrange)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = protocol,
                    onValueChange = { protocol = it },
                    label = { Text("Protocolo (vacío = cualquiera)", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    placeholder = { Text("PT2262, Keeloq, RAW...", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = { Text("Frecuencia contiene", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    placeholder = { Text("433.92", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        RfAutomationEngine.ACTION_NOTIFY to "Notify",
                        RfAutomationEngine.ACTION_TAG to "Tag",
                        RfAutomationEngine.ACTION_ALERT to "Alert",
                        RfAutomationEngine.ACTION_MACRO to "Macro"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = action == type,
                            onClick = { action = type },
                            label = { Text(label, fontFamily = FontFamily.Monospace, fontSize = 8.sp) }
                        )
                    }
                }
                if (action == RfAutomationEngine.ACTION_TAG) {
                    OutlinedTextField(
                        value = payload,
                        onValueChange = { payload = it },
                        label = { Text("Etiqueta a aplicar", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (action == RfAutomationEngine.ACTION_MACRO) {
                    if (macros.isEmpty()) {
                        Text(
                            "Crea un macro en Tools primero.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = NeonOrange
                        )
                    } else {
                        Text("Macro a ejecutar:", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                        macros.forEach { macro ->
                            FilterChip(
                                selected = payload == macro.name,
                                onClick = { payload = macro.name },
                                label = { Text(macro.name, fontFamily = FontFamily.Monospace, fontSize = 8.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onSave(name, protocol, frequency, action, payload)
                },
                enabled = name.isNotBlank() &&
                    (action != RfAutomationEngine.ACTION_MACRO || payload.isNotBlank())
            ) {
                Text("Guardar", color = MatrixGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextGray) }
        }
    )
}
