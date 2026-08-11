package com.embedsuite.app.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.embedsuite.app.R
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.ui.components.GlassCard
import com.embedsuite.app.ui.theme.BlackAMOLED
import com.embedsuite.app.ui.theme.DarkSurface
import com.embedsuite.app.ui.theme.MatrixGreen
import com.embedsuite.app.ui.theme.NeonOrange
import com.embedsuite.app.ui.theme.NeonRed
import com.embedsuite.app.ui.theme.TextGray
import com.embedsuite.app.ui.viewmodel.JammerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JammerScreen(
    connectionManager: DeviceConnectionManager,
    factory: androidx.lifecycle.ViewModelProvider.Factory,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val vm: JammerViewModel = viewModel(factory = factory)
    val s by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.toast.collect { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
    }

    Scaffold(
        containerColor = BlackAMOLED,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.jammer_title),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatrixGreen
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = MatrixGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            GlassCard(accent = NeonRed, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.jammer_legal),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = NeonOrange
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                stringResource(R.string.jammer_freq, "%.2f".format(s.frequencyMhz)),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MatrixGreen
            )
            Slider(
                value = s.frequencyMhz,
                onValueChange = vm::setFrequency,
                valueRange = 300f..928f,
                enabled = !s.active
            )

            Text(
                stringResource(R.string.jammer_power, s.power),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MatrixGreen
            )
            Slider(
                value = s.power.toFloat(),
                onValueChange = { vm.setPower(it.toInt()) },
                valueRange = 1f..12f,
                steps = 10,
                enabled = !s.active
            )

            Text(
                stringResource(R.string.jammer_max_s, s.maxSeconds),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MatrixGreen
            )
            Slider(
                value = s.maxSeconds.toFloat(),
                onValueChange = { vm.setMaxSeconds(it.toInt()) },
                valueRange = 1f..30f,
                steps = 28,
                enabled = !s.active
            )

            Text(
                stringResource(R.string.jammer_mode),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = TextGray
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = s.mode == "continuous",
                    onClick = { vm.setMode("continuous") },
                    enabled = !s.active
                )
                Text(stringResource(R.string.jammer_mode_continuous), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextGray)
                RadioButton(
                    selected = s.mode == "burst",
                    onClick = { vm.setMode("burst") },
                    enabled = !s.active
                )
                Text(stringResource(R.string.jammer_mode_burst), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextGray)
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { if (s.active) vm.stop() else vm.start() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (s.active) NeonRed else MatrixGreen
                )
            ) {
                Text(
                    if (s.active) stringResource(R.string.jammer_stop) else stringResource(R.string.jammer_start),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            if (s.lastStatus.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    s.lastStatus,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextGray
                )
            }

            if (s.active) {
                Spacer(Modifier.height(12.dp))
                GlassCard(accent = NeonRed, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.jammer_active_warn, "%.2f".format(s.frequencyMhz)),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = NeonRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
