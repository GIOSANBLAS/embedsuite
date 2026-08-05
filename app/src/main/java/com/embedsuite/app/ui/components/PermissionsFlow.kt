package com.embedsuite.app.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.R
import com.embedsuite.app.ui.theme.*

enum class EmbedPermission {
    LOCATION, BLUETOOTH, NOTIFICATIONS, MICROPHONE, USB
}

@Composable
fun PermissionsFlowScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val steps = listOf(
        Triple(EmbedPermission.USB, stringResource(R.string.perm_usb_title), stringResource(R.string.perm_usb_body)),
        Triple(EmbedPermission.LOCATION, stringResource(R.string.perm_location_title), stringResource(R.string.perm_location_body)),
        Triple(EmbedPermission.BLUETOOTH, stringResource(R.string.perm_bluetooth_title), stringResource(R.string.perm_bluetooth_body)),
        Triple(EmbedPermission.NOTIFICATIONS, stringResource(R.string.perm_notifications_title), stringResource(R.string.perm_notifications_body)),
        Triple(EmbedPermission.MICROPHONE, stringResource(R.string.perm_microphone_title), stringResource(R.string.perm_microphone_body))
    )

    val current = steps[step]
    var granted by remember(step) { mutableStateOf(false) }

    val permissions = remember(current.first) {
        when (current.first) {
            EmbedPermission.LOCATION -> arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            EmbedPermission.BLUETOOTH -> buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(Manifest.permission.BLUETOOTH_SCAN)
                    add(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }.toTypedArray()
            EmbedPermission.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            } else emptyArray()
            EmbedPermission.MICROPHONE -> arrayOf(Manifest.permission.RECORD_AUDIO)
            EmbedPermission.USB -> emptyArray()
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        granted = permissions.isEmpty() || results.values.all { it }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.perm_title), fontFamily = FontFamily.Monospace, fontSize = 20.sp, color = MatrixGreen)
        Text("${step + 1} / ${steps.size}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(16.dp))
        Text(current.second, fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = NeonCyan, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(current.third, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextGray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))

        if (current.first == EmbedPermission.USB) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { granted = true }, colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen)) {
                Text(stringResource(R.string.perm_usb_connected), fontFamily = FontFamily.Monospace, color = BlackAMOLED)
            }
        } else if (permissions.isEmpty()) {
            Button(onClick = { granted = true }, colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen)) {
                Text(stringResource(R.string.perm_continue), fontFamily = FontFamily.Monospace, color = BlackAMOLED)
            }
        } else {
            Button(onClick = { launcher.launch(permissions) }, colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen)) {
                Text(stringResource(R.string.perm_grant), fontFamily = FontFamily.Monospace, color = BlackAMOLED)
            }
            if (granted.not()) {
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                }) {
                    Text(stringResource(R.string.perm_open_settings), fontFamily = FontFamily.Monospace, color = TextGray, fontSize = 10.sp)
                }
            }
        }

        if (current.first == EmbedPermission.MICROPHONE) {
            TextButton(onClick = { granted = true }) {
                Text(stringResource(R.string.perm_skip_mic), fontFamily = FontFamily.Monospace, color = TextMuted, fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (step < steps.lastIndex) { step++; granted = false }
                else onComplete()
            },
            enabled = granted || current.first == EmbedPermission.MICROPHONE,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
        ) {
            Text(
                if (step < steps.lastIndex) stringResource(R.string.perm_next) else stringResource(R.string.perm_start),
                fontFamily = FontFamily.Monospace,
                color = BlackAMOLED
            )
        }
    }
}

@Composable
fun ScanPermissionsGate(
    onGranted: () -> Unit,
    content: @Composable () -> Unit
) {
    var granted by remember { mutableStateOf(false) }

    val permissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        granted = results.values.all { it }
        if (granted) onGranted()
    }

    if (granted) {
        content()
    } else {
        Button(
            onClick = { launcher.launch(permissions) },
            colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen)
        ) {
            Text(stringResource(R.string.perm_scan_gate), fontFamily = FontFamily.Monospace, color = BlackAMOLED)
        }
    }
}
