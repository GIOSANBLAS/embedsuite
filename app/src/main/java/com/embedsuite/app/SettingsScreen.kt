package com.embedsuite.app

/**
 * Settings screen — restructured for production use.
 *
 * REMOVED (non-functional or superseded):
 * - Manual language selector — app follows device locale only (see [com.embedsuite.app.core.LocaleManager]).
 * - WiFi/BLE default transport chips — experimental transports removed from settings; USB is the supported path.
 * - Activity recreate on language change — no longer applicable.
 *
 * KEPT (working):
 * - Audio (sound, haptics, device beep when connected)
 * - SD storage queries via Bruce CLI (when device connected)
 * - UI (scanlines, glass intensity)
 * - Field RF frequency preset
 * - Connection (auto-reconnect, USB transport info, firmware detection, mock transport in debug builds)
 * - Developer mode gate (CLI, SD files, scripts, RF hub, map tools)
 * - System (about, reset onboarding)
 */
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
import com.embedsuite.app.BuildConfig
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.connection.TransportType
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.core.AppPreferences
import com.embedsuite.app.core.LocaleManager
import com.embedsuite.app.core.SoundFeedback
import com.embedsuite.app.ui.components.*
import com.embedsuite.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    connectionManager: DeviceConnectionManager,
    onBack: () -> Unit,
    onNavigateAbout: () -> Unit = {},
    onNavigateDeveloper: (String) -> Unit = {},
    onResetOnboarding: () -> Unit = {}
) {
    val soundEnabled by preferences.soundEnabled.collectAsState()
    val hapticsEnabled by preferences.hapticsEnabled.collectAsState()
    val scanlinesEnabled by preferences.scanlinesEnabled.collectAsState()
    val autoReconnect by preferences.autoReconnect.collectAsState()
    val detectedProfile by connectionManager.detectedProfile.collectAsState()
    val glassIntensity by preferences.glassIntensity.collectAsState()
    val fieldFrequency by preferences.fieldFrequencyMhzFlow.collectAsState()
    val developerMode by preferences.developerMode.collectAsState()
    var useMockTransport by remember { mutableStateOf(preferences.useMockTransport) }
    var repairMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val deviceLocale = remember {
        LocaleManager.resolveLanguage(context).tag
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = MatrixGreen)
            }
            Column {
                Text(
                    stringResource(R.string.settings_title),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixGreen
                )
                Text(
                    stringResource(R.string.settings_subtitle),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextGray
                )
            }
        }
        HorizontalDivider(color = MatrixGreen.copy(alpha = 0.35f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            GlassCard(accent = NeonCyan, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(stringResource(R.string.settings_locale_auto), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonCyan)
                Text(stringResource(R.string.settings_locale_auto_sub), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                Text(
                    stringResource(R.string.settings_locale_detected, deviceLocale),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = MatrixGreen,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            GlassCard(accent = KaliBlue, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(stringResource(R.string.settings_audio), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonCyan)
                SettingToggle(stringResource(R.string.settings_sound), soundEnabled) {
                    preferences.setSoundEnabled(it)
                    SoundFeedback.setEnabled(it)
                    if (it) SoundFeedback.playSuccess()
                }
                SettingToggle(stringResource(R.string.settings_haptics), hapticsEnabled) {
                    preferences.setHapticsEnabled(it)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        scope.launch {
                            connectionManager.deviceAudioBeep(1000, 120).fold(
                                onSuccess = { repairMessage = "audio.beep OK" },
                                onFailure = { repairMessage = "audio.beep: ${it.message}" }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.settings_device_beep),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NeonCyan
                    )
                }
            }

            GlassCard(accent = NeonCyan, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(stringResource(R.string.settings_storage), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonCyan)
                Text(stringResource(R.string.settings_storage_sub), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        scope.launch {
                            connectionManager.sdCardStatus().fold(
                                onSuccess = { d ->
                                    repairMessage = if (d.optBoolean("mounted")) {
                                        "SD OK · used=${d.optLong("used_bytes") / 1024}KB / total=${d.optLong("total_bytes") / 1024}KB"
                                    } else {
                                        "SD no montada"
                                    }
                                },
                                onFailure = { repairMessage = "sd.status: ${it.message}" }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_sd_status), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            connectionManager.sdCardList("/bruce").fold(
                                onSuccess = { files ->
                                    repairMessage = if (files.isEmpty()) {
                                        "SD /bruce vacío o sin carpeta"
                                    } else {
                                        "SD /bruce:\n" + files.take(12).joinToString("\n")
                                    }
                                },
                                onFailure = { repairMessage = "sd.list: ${it.message}" }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_sd_list), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
                }
            }

            GlassCard(accent = MatrixGreen, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(stringResource(R.string.settings_ui), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MatrixGreen)
                SettingToggle(stringResource(R.string.settings_scanlines), scanlinesEnabled) {
                    preferences.setScanlinesEnabled(it)
                }
                Text(
                    stringResource(R.string.settings_glass, (glassIntensity * 100).toInt()),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextGray
                )
                Slider(
                    value = glassIntensity,
                    onValueChange = { preferences.setGlassIntensity(it) },
                    valueRange = 0.3f..1f,
                    colors = SliderDefaults.colors(thumbColor = MatrixGreen, activeTrackColor = MatrixGreen)
                )
            }

            GlassCard(accent = NeonRed, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(stringResource(R.string.settings_field), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonRed)
                Text(stringResource(R.string.settings_field_freq_hint), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                RfFrequencyPicker(
                    selectedMhz = fieldFrequency,
                    onSelected = { preferences.fieldFrequencyMhz = it },
                    label = stringResource(R.string.settings_field_mhz)
                )
            }

            GlassCard(accent = NeonOrange, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(stringResource(R.string.settings_connection), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonOrange)
                SettingToggle(stringResource(R.string.settings_auto_reconnect), autoReconnect) {
                    preferences.setAutoReconnect(it)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_transport_usb_only),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = MatrixGreen,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(stringResource(R.string.settings_firmware_profile), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray)
                Text(
                    stringResource(R.string.settings_firmware_profile_bruce_recommended),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = MatrixGreen,
                    modifier = Modifier.padding(top = 4.dp)
                )
                detectedProfile.takeIf { it != FirmwareProfile.UNKNOWN }?.let { profile ->
                    Text(
                        stringResource(R.string.settings_firmware_detected, profile.label),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = NeonCyan,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (BuildConfig.ENABLE_MOCK_TRANSPORT) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingToggle(stringResource(R.string.settings_mock_transport), useMockTransport) { enabled ->
                        useMockTransport = enabled
                        preferences.useMockTransport = enabled
                    }
                    Text(
                        stringResource(R.string.settings_mock_transport_sub),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = TextGray
                    )
                }
            }

            GlassCard(accent = NeonOrange, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text("Modo desarrollador", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonOrange)
                Text("Consola CLI, archivos SD, RF hub legacy", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                SettingToggle("Activar", developerMode) { preferences.setDeveloperMode(it) }
                if (developerMode) {
                    listOf(
                        "Flash Bruce (USB)" to "firmware_flash",
                        "Consola CLI" to "terminal",
                        "Archivos SD" to "bruce_files",
                        "Scripts" to "scripts",
                        "RF Hub" to "rf",
                        "Mapa / herramientas" to "map_tools"
                    ).forEach { (label, route) ->
                        TextButton(onClick = { onNavigateDeveloper(route) }, modifier = Modifier.fillMaxWidth()) {
                            Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
                        }
                    }
                }
            }

            repairMessage?.let { msg ->
                GlassCard(accent = NeonCyan, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Text(msg, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MatrixGreen)
                }
            }

            GlassCard(accent = MatrixGreen, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(stringResource(R.string.settings_system), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonCyan)
                TextButton(onClick = onNavigateAbout, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.settings_about), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MatrixGreen)
                            Text(stringResource(R.string.settings_about_sub), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                        }
                        Text("→", fontFamily = FontFamily.Monospace, color = NeonCyan)
                    }
                }
                TextButton(onClick = {
                    preferences.onboardingComplete = false
                    preferences.permissionsComplete = false
                    onResetOnboarding()
                }) {
                    Text(stringResource(R.string.settings_reset_onboarding), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NeonOrange)
                }
            }
        }
    }
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextGray)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MatrixGreen,
                checkedTrackColor = MatrixGreen.copy(alpha = 0.3f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkSurfaceElevated
            )
        )
    }
}
