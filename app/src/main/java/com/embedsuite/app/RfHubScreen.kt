package com.embedsuite.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.ai.EmbedAiEngine
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.data.SignalRepository
import com.embedsuite.app.rf.RfReplayEngine
import com.embedsuite.app.ui.components.GlassChip
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.RfHubViewModel

@Composable
fun RfHubScreen(
    viewModel: RfHubViewModel,
    connectionManager: DeviceConnectionManager,
    signalRepository: SignalRepository,
    rfReplayEngine: RfReplayEngine,
    aiEngine: EmbedAiEngine,
    initialTab: Int? = null,
    highlightSignalId: Long? = null
) {
    val tab by viewModel.selectedTab.collectAsState()
    val lastDecoded by viewModel.lastDecoded.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(initialTab) {
        initialTab?.let { viewModel.selectTab(it.coerceIn(0, 2)) }
    }

    LaunchedEffect(highlightSignalId) {
        highlightSignalId?.let {
            viewModel.selectTab(1)
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.tools_rf_highlight, it),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GlassChip(stringResource(R.string.rf_tab_spectrum), tab == 0, { viewModel.selectTab(0) }, MatrixGreen)
            GlassChip(stringResource(R.string.rf_tab_library), tab == 1, { viewModel.selectTab(1) }, NeonCyan)
            GlassChip(stringResource(R.string.rf_tab_analysis), tab == 2, { viewModel.selectTab(2) }, NeonOrange)
        }

        lastDecoded?.let { decoded ->
            Text(
                "DECODED: ${decoded.lineSequence().firstOrNull() ?: decoded.take(50)}",
                fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonCyan,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        when (tab) {
            0 -> SubGhzScreen(connectionManager = connectionManager)
            1 -> SignalLibraryScreen(
                signalRepository = signalRepository,
                connectionManager = connectionManager,
                rfReplayEngine = rfReplayEngine,
                highlightSignalId = highlightSignalId
            )
            2 -> RfAnalysisScreen(signalRepository, rfReplayEngine, aiEngine)
        }
    }
}
