package com.embedsuite.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.R
import com.embedsuite.app.connection.FirmwareRelease
import com.embedsuite.app.connection.FirmwareSource
import com.embedsuite.app.ui.theme.*

@Composable
fun FirmwareFlashCard(
    otaProgress: Int,
    flashStatus: String,
    firmwareOptions: List<FirmwareRelease>,
    selectedRelease: FirmwareRelease?,
    recommendedRelease: FirmwareRelease?,
    isLoadingReleases: Boolean,
    isFlashing: Boolean = false,
    onLoadReleases: () -> Unit,
    onSelectRelease: (FirmwareRelease) -> Unit,
    onPickCustomBin: () -> Unit,
    onClearCustom: () -> Unit,
    onFlashOta: (FirmwareRelease) -> Unit,
    onFlashUsb: (FirmwareRelease) -> Unit
) {
    var pendingFlash by remember { mutableStateOf<Pair<FirmwareRelease, FlashMethod>?>(null) }
    var disclaimerAccepted by remember { mutableStateOf(false) }

    pendingFlash?.let { (release, method) ->
        AlertDialog(
            onDismissRequest = {
                pendingFlash = null
                disclaimerAccepted = false
            },
            containerColor = DarkSurface,
            title = {
                Text(
                    stringResource(R.string.firmware_disclaimer_title),
                    fontFamily = FontFamily.Monospace,
                    color = NeonRed,
                    fontSize = 12.sp
                )
            },
            text = {
                Column {
                    Text(
                        stringResource(R.string.firmware_disclaimer_body),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = TextGray,
                        lineHeight = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        release.fileName,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = NeonOrange
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = disclaimerAccepted,
                            onCheckedChange = { disclaimerAccepted = it },
                            colors = CheckboxDefaults.colors(checkedColor = NeonOrange)
                        )
                        Text(
                            stringResource(R.string.firmware_disclaimer_accept),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = MatrixGreen
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (method) {
                            FlashMethod.OTA -> onFlashOta(release)
                            FlashMethod.USB -> onFlashUsb(release)
                        }
                        pendingFlash = null
                        disclaimerAccepted = false
                    },
                    enabled = disclaimerAccepted
                ) {
                    Text(stringResource(R.string.firmware_disclaimer_continue), color = NeonRed)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingFlash = null
                    disclaimerAccepted = false
                }) {
                    Text(stringResource(R.string.action_cancel), color = TextGray)
                }
            }
        )
    }

    fun requestFlash(release: FirmwareRelease, method: FlashMethod) {
        if (release.requiresDisclaimer) {
            pendingFlash = release to method
        } else {
            when (method) {
                FlashMethod.OTA -> onFlashOta(release)
                FlashMethod.USB -> onFlashUsb(release)
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, NeonOrange.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                stringResource(R.string.firmware_flash_title),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = NeonOrange
            )
            Text(
                stringResource(R.string.firmware_flash_sub),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = BlackAMOLED),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    .border(1.dp, MatrixGreen.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
            ) {
                Column(Modifier.padding(8.dp)) {
                    Text(
                        stringResource(R.string.firmware_section_official_xibalba),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatrixGreen
                    )
                    recommendedRelease?.let { rec ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "★ ${stringResource(R.string.firmware_recommended)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Text(
                            "${rec.tagName} — ${rec.fileName}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = MatrixGreen
                        )
                        Text(
                            stringResource(R.string.firmware_recommended_reason_xibalba),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            color = TextGray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Text(
                        flashStatus,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = TextGray,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    if (otaProgress > 0) {
                        LinearProgressIndicator(
                            progress = { otaProgress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MatrixGreen,
                            trackColor = DarkSurfaceElevated
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Button(
                        onClick = onLoadReleases,
                        enabled = !isLoadingReleases && !isFlashing,
                        colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.firmware_fetch_xibalba),
                            fontFamily = FontFamily.Monospace,
                            color = BlackAMOLED,
                            fontSize = 11.sp
                        )
                    }
                    val officialOptions = firmwareOptions.filter { it.source != FirmwareSource.CUSTOM_LOCAL }
                    if (officialOptions.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.firmware_available),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = NeonCyan
                        )
                        officialOptions.forEach { release ->
                            val selected = selectedRelease?.identityKey() == release.identityKey()
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { onSelectRelease(release) },
                                    colors = RadioButtonDefaults.colors(selectedColor = MatrixGreen)
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        buildString {
                                            append(release.displayLabel)
                                            when {
                                                release.isRecommended -> append(" ★")
                                                release.isPrerelease -> append(" [BETA]")
                                                release.source == FirmwareSource.OFFICIAL_XIBALBA -> append(" [XIBALBA]")
                                            }
                                        },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = if (selected) NeonCyan else MatrixGreen
                                    )
                                    Text(release.fileName, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                                    Text(
                                        stringResource(R.string.firmware_xibalba_desc),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = BlackAMOLED),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .border(1.dp, NeonRed.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            ) {
                Column(Modifier.padding(8.dp)) {
                    Text(
                        stringResource(R.string.firmware_section_custom_bin),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonRed
                    )
                    Text(
                        stringResource(R.string.firmware_pick_custom_hint),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = TextGray,
                        modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onPickCustomBin,
                            enabled = !isFlashing,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                stringResource(R.string.firmware_pick_custom),
                                fontFamily = FontFamily.Monospace,
                                color = BlackAMOLED,
                                fontSize = 10.sp
                            )
                        }
                        if (firmwareOptions.any { it.source == FirmwareSource.CUSTOM_LOCAL }) {
                            TextButton(onClick = onClearCustom) {
                                Text(
                                    stringResource(R.string.firmware_clear_custom),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }
                    firmwareOptions.filter { it.source == FirmwareSource.CUSTOM_LOCAL }.forEach { release ->
                        val selected = selectedRelease?.identityKey() == release.identityKey()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectRelease(release) },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonRed)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${release.displayLabel} [CUSTOM]",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = if (selected) NeonRed else MatrixGreen
                                )
                                Text(release.fileName, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray)
                                Text(
                                    stringResource(R.string.firmware_custom_desc),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            selectedRelease?.let { release ->
                Spacer(modifier = Modifier.height(8.dp))
                if (release.source == FirmwareSource.CUSTOM_LOCAL) {
                    Text(
                        stringResource(R.string.firmware_custom_usb_hint),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = NeonOrange,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { requestFlash(release, FlashMethod.OTA) },
                        enabled = release.source == FirmwareSource.OFFICIAL_XIBALBA && !isFlashing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MatrixGreen,
                            disabledContainerColor = DarkSurfaceElevated
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            stringResource(R.string.firmware_ota_teh_link),
                            fontFamily = FontFamily.Monospace,
                            color = BlackAMOLED,
                            fontSize = 10.sp
                        )
                    }
                    Button(
                        onClick = { requestFlash(release, FlashMethod.USB) },
                        enabled = !isFlashing,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.firmware_usb_flash), fontFamily = FontFamily.Monospace, color = BlackAMOLED, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

private enum class FlashMethod { OTA, USB }
