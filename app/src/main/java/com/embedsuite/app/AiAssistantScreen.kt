package com.embedsuite.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.ai.AiMode
import com.embedsuite.app.ui.viewmodel.AiAssistantViewModel
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiAssistantScreen(
    viewModel: AiAssistantViewModel,
    activeTab: String = "ai",
    secureStoreAvailable: Boolean = true
) {
    val aiEngine = viewModel.aiEngine
    val contextualHint = remember(activeTab) { aiEngine.getContextualHint(activeTab) }
    var inputText by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf(aiEngine.preferences.getGeminiApiKey()) }
    var ollamaHostInput by remember { mutableStateOf(aiEngine.preferences.getOllamaHost()) }
    var ollamaModelInput by remember { mutableStateOf(aiEngine.preferences.getOllamaModel()) }
    var showSettings by remember { mutableStateOf(false) }

    val messages by aiEngine.messages.collectAsState()
    val isProcessing by aiEngine.isProcessing.collectAsState()
    val aiMode by aiEngine.preferences.mode.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            inputText = spoken
            scope.launch { aiEngine.processUserInput(spoken) }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Comando para T-Embed...")
            }
            voiceLauncher.launch(intent)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "EMBED AI CORE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixGreen
                )
                Text(
                    "Modo: ${aiMode.name} | ${if (connectionState is com.embedsuite.app.connection.ConnectionState.Connected) "LINK OK" else "OFFLINE"}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = TextGray
                )
                if (contextualHint.isNotBlank()) {
                    Text(contextualHint, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = NeonCyan)
                }
            }
            Row {
                IconButton(onClick = { showSettings = !showSettings }) {
                    Icon(Icons.Default.Settings, contentDescription = "Ajustes AI", tint = NeonCyan)
                }
                IconButton(onClick = { aiEngine.clearChat() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Limpiar", tint = NeonOrange)
                }
            }
        }

        if (!secureStoreAvailable) {
            Text(
                stringResource(R.string.ai_secure_store_warning),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = NeonOrange,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        if (showSettings) {
            AiSettingsPanel(
                apiKey = apiKeyInput,
                onApiKeyChange = { apiKeyInput = it },
                aiMode = aiMode,
                autoExecute = aiEngine.preferences.isAutoExecute(),
                ollamaHost = ollamaHostInput,
                onOllamaHostChange = { ollamaHostInput = it },
                ollamaModel = ollamaModelInput,
                onOllamaModelChange = { ollamaModelInput = it },
                onModeChange = { aiEngine.preferences.setMode(it) },
                onAutoExecuteChange = { aiEngine.preferences.setAutoExecute(it) },
                onSaveApiKey = { aiEngine.preferences.setGeminiApiKey(apiKeyInput) },
                onSaveOllama = {
                    aiEngine.preferences.setOllamaHost(ollamaHostInput)
                    aiEngine.preferences.setOllamaModel(ollamaModelInput)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickAiChip("Captura RF") {
                scope.launch { aiEngine.processUserInput("Captura subghz raw 15 segundos") }
            }
            QuickAiChip("Analizar") {
                scope.launch { aiEngine.analyzeLastSignal() }
            }
            QuickAiChip("Reporte") {
                scope.launch { aiEngine.generateSessionReport() }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(6.dp))
                .border(1.dp, MatrixGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                .padding(8.dp)
        ) {
            LazyColumn(state = listState) {
                items(messages) { msg ->
                    AiMessageBubble(msg.role, msg.content, msg.commandExecuted)
                }
                if (isProcessing) {
                    item {
                        Text(
                            "EMBED AI procesando...",
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Icon(Icons.Default.Mic, contentDescription = "Voz", tint = MatrixGreen)
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Orden para T-Embed...", color = TextGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MatrixGreen,
                    unfocusedBorderColor = TextGray,
                    focusedTextColor = MatrixGreen,
                    unfocusedTextColor = MatrixGreen
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank()) {
                        val text = inputText
                        inputText = ""
                        scope.launch { aiEngine.processUserInput(text) }
                    }
                })
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val text = inputText
                        inputText = ""
                        scope.launch { aiEngine.processUserInput(text) }
                    }
                },
                modifier = Modifier.background(MatrixGreen, RoundedCornerShape(6.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = BlackAMOLED)
            }
        }
    }
}

@Composable
private fun QuickAiChip(label: String, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MatrixGreen) },
        colors = FilterChipDefaults.filterChipColors(containerColor = DarkSurface)
    )
}

@Composable
private fun AiMessageBubble(role: String, content: String, command: String?) {
    val (color, prefix) = when (role) {
        "user" -> NeonCyan to "YOU>"
        "system" -> NeonOrange to "SYS>"
        else -> MatrixGreen to "AI>"
    }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$prefix $content",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = color,
            lineHeight = 16.sp
        )
        command?.let {
            Text(
                text = "CMD: $it",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = NeonOrange,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun AiSettingsPanel(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    aiMode: AiMode,
    autoExecute: Boolean,
    ollamaHost: String,
    onOllamaHostChange: (String) -> Unit,
    ollamaModel: String,
    onOllamaModelChange: (String) -> Unit,
    onModeChange: (AiMode) -> Unit,
    onAutoExecuteChange: (Boolean) -> Unit,
    onSaveApiKey: () -> Unit,
    onSaveOllama: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text("AJUSTES AI", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonCyan)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AiMode.entries.forEach { mode ->
                    FilterChip(
                        selected = aiMode == mode,
                        onClick = { onModeChange(mode) },
                        label = {
                            Text(
                                mode.name,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = if (aiMode == mode) BlackAMOLED else MatrixGreen
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MatrixGreen,
                            containerColor = BlackAMOLED
                        )
                    )
                }
            }
            if (aiMode == AiMode.GEMINI) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text("Gemini API Key", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        focusedTextColor = MatrixGreen,
                        unfocusedTextColor = MatrixGreen
                    )
                )
                TextButton(onClick = onSaveApiKey) {
                    Text("Guardar API Key", fontFamily = FontFamily.Monospace, color = NeonCyan, fontSize = 10.sp)
                }
            }
            if (aiMode == AiMode.OLLAMA) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = ollamaHost,
                    onValueChange = onOllamaHostChange,
                    label = { Text("Ollama URL (LAN)", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    placeholder = { Text("http://192.168.1.100:11434", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        focusedTextColor = MatrixGreen,
                        unfocusedTextColor = MatrixGreen
                    )
                )
                OutlinedTextField(
                    value = ollamaModel,
                    onValueChange = onOllamaModelChange,
                    label = { Text("Modelo Ollama", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    placeholder = { Text("llama3.2", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        focusedTextColor = MatrixGreen,
                        unfocusedTextColor = MatrixGreen
                    )
                )
                TextButton(onClick = onSaveOllama) {
                    Text("Guardar Ollama", fontFamily = FontFamily.Monospace, color = NeonCyan, fontSize = 10.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-ejecutar comandos", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
                Switch(
                    checked = autoExecute,
                    onCheckedChange = onAutoExecuteChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = MatrixGreen)
                )
            }
        }
    }
}
