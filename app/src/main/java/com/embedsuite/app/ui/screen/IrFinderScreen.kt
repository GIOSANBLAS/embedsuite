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
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.IrFinderViewModel

@Composable
fun IrFinderScreen(viewModel: IrFinderViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Column(
        Modifier.fillMaxSize().background(BlackAMOLED).padding(12.dp).verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = MatrixGreen) }
            Text("IR FINDER", color = MatrixGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Text("Apunta el mando al T-Embed y pulsa escuchar (ir rx).", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        Spacer(Modifier.height(12.dp))

        Text("Escucha: ${state.listenSec}s", color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        Slider(
            value = state.listenSec.toFloat(),
            onValueChange = { viewModel.setListenSec(it.toInt()) },
            valueRange = 1f..60f,
            steps = 58,
            colors = SliderDefaults.colors(thumbColor = MatrixGreen, activeTrackColor = MatrixGreen)
        )

        Button(
            onClick = viewModel::listen,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen.copy(alpha = 0.2f))
        ) {
            if (state.busy) CircularProgressIndicator(Modifier.size(18.dp), color = MatrixGreen, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("ESCUCHAR (ir rx)", fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(12.dp))
        OrchestrationFeedback(state.lastResult)
    }
}
