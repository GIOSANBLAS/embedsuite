package com.embedsuite.app

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.embedsuite.app.BuildConfig
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.connection.TransportType
import com.embedsuite.app.connection.DeviceConnectionManager
import com.embedsuite.app.core.AppLanguage
import com.embedsuite.app.core.AppPreferences
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
    onResetOnboarding: () -> Unit = {},
    onLanguageChanged: () -> Unit = {}
) {
    val soundEnabled by preferences.soundEnabled.collectAsState()
    val hapticsEnabled by preferences.hapticsEnabled.collectAsState()
    val scanlinesEnabled by preferences.scanlinesEnabled.collectAsState()
    val autoReconnect by preferences.autoReconnect.collectAsState()
    val defaultTransport by preferences.defaultTransport.collectAsState()
    val firmwareProfile by preferences.firmwareProfile.collectAsState()
    val glassIntensity by preferences.glassIntensity.collectAsState()
    val fieldFrequency by preferences.fieldFrequencyMhzFlow.collectAsState()
    val appLanguage by preferences.appLanguage.collectAsState()
    var useMockTransport by remember { mutableStateOf(preferences.useMockTransport) }
    var repairMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
                Text(stringResource(R.string.settings_language), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonCyan)
                Text(stringResource(R.string.settings_language_sub), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        GlassChip(
                            label = stringResource(lang.labelRes),
                            selected = appLanguage == lang,
                            onClick = {
                                if (appLanguage != lang) {
                                    preferences.setAppLanguage(lang)
                                    onLanguageChanged()
                                }
                            },
                            accent = NeonCyan
                        )
                    }
                }
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
                Text(stringResource(R.string.settings_default_transport), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TransportType.entries.forEach { type ->
                        GlassChip(
                            label = type.name,
                            selected = defaultTransport == type,
                            onClick = { preferences.setDefaultTransport(type) },
                            accent = NeonOrange
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(stringResource(R.string.settings_firmware_profile), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray)
                Text(stringResource(R.string.settings_firmware_profile_sub), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FirmwareProfile.entries.filter { it != FirmwareProfile.UNKNOWN }.forEach { profile ->
                        GlassChip(
                            label = profile.label,
                            selected = firmwareProfile == profile,
                            onClick = { preferences.setFirmwareProfile(profile) },
                            accent = NeonOrange
                        )
                    }
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
                    if (useMockTransport) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                connectionManager.simulateMockLongPress().fold(
                                    onSuccess = { repairMessage = "Mock: pairing + BadUSB arm OK" },
                                    onFailure = { repairMessage = it.message }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.settings_mock_long_press),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = NeonCyan
                            )
                        }
                        Text(
                            stringResource(R.string.settings_mock_long_press_sub),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = TextGray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = {
                        scope.launch {
                            connectionManager.rePairTehLink().fold(
                                onSuccess = { repairMessage = "TEH-Link re-emparejado" },
                                onFailure = { repairMessage = it.message }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.settings_repair_teh_link),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MatrixGreen
                    )
                }
                Text(
                    stringResource(R.string.settings_repair_teh_link_sub),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = TextGray
                )
                repairMessage?.let { msg ->
                    Text(
                        msg,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = NeonOrange,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            GlassCard(accent = NeonCyan, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
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
