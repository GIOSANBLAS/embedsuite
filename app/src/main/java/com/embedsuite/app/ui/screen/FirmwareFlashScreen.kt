package com.embedsuite.app.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.R
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.flash.FirmwareFlashCoordinator
import com.embedsuite.app.ui.components.FirmwareFlashCard
import com.embedsuite.app.ui.components.HackerSectionHeader
import com.embedsuite.app.ui.theme.*
import com.embedsuite.app.ui.viewmodel.MapToolsViewModel
import kotlinx.coroutines.launch

@Composable
fun FirmwareFlashScreen(
    viewModel: MapToolsViewModel,
    connectionManager: DeviceConnectionManager,
    flashCoordinator: FirmwareFlashCoordinator,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackAMOLED)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = MatrixGreen)
            }
            Column {
                Text(
                    stringResource(R.string.firmware_flash_title),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonOrange
                )
                Text(
                    stringResource(R.string.firmware_flash_screen_sub),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = TextGray
                )
            }
        }
        HorizontalDivider(color = NeonOrange.copy(alpha = 0.35f))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            FirmwareFlashSection(
                viewModel = viewModel,
                connectionManager = connectionManager,
                flashCoordinator = flashCoordinator
            )
        }
    }
}

@Composable
fun FirmwareFlashSection(
    viewModel: MapToolsViewModel,
    connectionManager: DeviceConnectionManager,
    flashCoordinator: FirmwareFlashCoordinator,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val systemInfo by viewModel.systemInfo.collectAsState()
    val otaProgress by flashCoordinator.otaProgress.collectAsState()
    val isFlashing by flashCoordinator.isFlashing.collectAsState()
    val flashStatus by flashCoordinator.flashStatus.collectAsState()

    val customFirmwareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importCustomFirmware(context, it) }
    }

    Column(modifier = modifier) {
        HackerSectionHeader(
            stringResource(R.string.firmware_flash_section_header),
            accent = NeonOrange
        )
        Text(
            stringResource(R.string.firmware_flash_screen_hint),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        FirmwareFlashCard(
            otaProgress = otaProgress,
            flashStatus = flashStatus.ifBlank { stringResource(R.string.firmware_status_ready) },
            lastOta = systemInfo.lastOta,
            hardening = systemInfo.hardening,
            firmwareOptions = uiState.allFirmwareOptions,
            selectedRelease = uiState.selectedRelease,
            recommendedRelease = uiState.recommendedRelease,
            isLoadingReleases = uiState.isLoadingReleases,
            isFlashing = isFlashing,
            onLoadReleases = {
                viewModel.loadReleases()
                flashCoordinator.setStatusMessage(context.getString(R.string.firmware_status_fetching))
            },
            onSelectRelease = { viewModel.selectRelease(it) },
            onPickCustomBin = { customFirmwareLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
            onClearCustom = { viewModel.clearCustomFirmware() },
            onFlashOta = { release ->
                val previous = uiState.recommendedRelease ?: uiState.releases.firstOrNull()
                scope.launch {
                    val result = flashCoordinator.flashWithRollback(
                        release = release,
                        previousRelease = if (release.identityKey() != previous?.identityKey()) previous else null
                    )
                    flashCoordinator.setStatusMessage(result.message)
                }
            },
            onFlashUsb = { release -> flashCoordinator.flashUsb(context, release) }
        )
    }
}
