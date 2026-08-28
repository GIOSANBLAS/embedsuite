package com.embedsuite.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.data.IrdbEntryEntity
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.IrSearchViewModel

@Composable
fun IrSearchScreen(viewModel: IrSearchViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().background(BlackAMOLED).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = MatrixGreen) }
            Text("BUSCADOR IR", color = MatrixGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Text(
            "IRDB local · selecciona → upload WiFi → ir tx_from_file",
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MatrixGreen) },
            label = { Text("Ej: Samsung aire", fontFamily = FontFamily.Monospace) },
            singleLine = true
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::syncIndex, enabled = !state.syncing) {
                Text(if (state.syncing) "Sync…" else "Indexar IRDB (${state.indexedCount})", fontSize = 9.sp)
            }
        }
        if (state.status.isNotBlank()) {
            Text(state.status, color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(state.results, key = { it.path }) { entry ->
                IrSearchResultCard(entry, state.transmitting) { viewModel.transmit(entry) }
            }
        }
        OrchestrationFeedback(state.lastResult)
    }
}

@Composable
private fun IrSearchResultCard(entry: IrdbEntryEntity, busy: Boolean, onTransmit: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.device, color = MatrixGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${entry.brand} · ${entry.function}", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
            Button(onClick = onTransmit, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen.copy(0.2f))) {
                Text("TX", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }
    }
}
