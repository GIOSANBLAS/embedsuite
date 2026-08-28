package com.embedsuite.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.core.bruce.BruceLimits
import com.embedsuite.app.core.orchestrator.BadUsbTemplates
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.BadUsbForgeViewModel

@Composable
fun BadUsbForgeScreen(viewModel: BadUsbForgeViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Column(
        Modifier.fillMaxSize().background(BlackAMOLED).padding(12.dp).verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = MatrixGreen) }
            Text("BADUSB", color = MatrixGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Text(BruceLimits.BADUSB_HINT, color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        Text(state.transportHint, color = NeonOrange, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BadUsbTemplates.all.take(3).forEach { tpl ->
                OutlinedButton(onClick = { viewModel.applyTemplate(tpl) }) { Text(tpl.name, fontSize = 9.sp) }
            }
        }

        BadUsbBlockEditor(blocks = state.blocks, onBlocksChange = viewModel::setBlocks, modifier = Modifier.padding(vertical = 8.dp))

        TextButton(onClick = viewModel::toggleAdvancedScript) {
            Text(if (state.showAdvancedScript) "Ocultar script" else "Ver script generado", fontSize = 9.sp)
        }
        if (state.showAdvancedScript) {
            Text(state.scriptPreview, color = TextGray, fontFamily = FontFamily.Monospace, fontSize = 8.sp)
        }

        state.validationIssues.forEach { issue ->
            Text("L${issue.line}: ${issue.message}", color = NeonRed, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        }

        Button(
            onClick = viewModel::runPipeline,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen.copy(alpha = 0.2f))
        ) {
            if (state.busy) CircularProgressIndicator(Modifier.size(18.dp), color = MatrixGreen, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("EJECUTAR EN T-EMBED", fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(12.dp))
        OrchestrationFeedback(state.lastResult)
    }
}
