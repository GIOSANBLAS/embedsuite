package com.embedsuite.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.R
import com.embedsuite.app.nfc.MifareParser
import com.embedsuite.app.ui.theme.*

@Composable
fun MifareHexEditor(
    sectors: List<MifareParser.SectorInfo>,
    onBlockChanged: (sectorIndex: Int, blockIndex: Int, hex: String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (sectors.isEmpty()) {
        Text(
            stringResource(R.string.mifare_no_blocks),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = TextMuted,
            modifier = modifier.padding(8.dp)
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(sectors, key = { it.index }) { sector ->
            MifareSectorCard(
                sector = sector,
                enabled = enabled,
                onBlockChanged = onBlockChanged
            )
        }
    }
}

@Composable
private fun MifareSectorCard(
    sector: MifareParser.SectorInfo,
    enabled: Boolean,
    onBlockChanged: (sectorIndex: Int, blockIndex: Int, hex: String) -> Unit
) {
    var expanded by remember(sector.index) { mutableStateOf(sector.index < 2) }

    Card(
        colors = CardDefaults.cardColors(containerColor = BlackAMOLED),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MatrixGreen.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.mifare_sector_label, sector.index),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = NeonCyan,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MatrixGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (expanded) {
                sector.blocks.forEachIndexed { blockIdx, hex ->
                    MifareBlockRow(
                        sectorIndex = sector.index,
                        blockIndex = blockIdx,
                        hex = hex,
                        enabled = enabled,
                        onBlockChanged = onBlockChanged
                    )
                }
            } else {
                Text(
                    sector.blocks.firstOrNull()?.let { MifareParser.formatBlockDisplay(it).take(23) + "…" }
                        ?: "—",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = TextGray
                )
            }
        }
    }
}

@Composable
private fun MifareBlockRow(
    sectorIndex: Int,
    blockIndex: Int,
    hex: String,
    enabled: Boolean,
    onBlockChanged: (sectorIndex: Int, blockIndex: Int, hex: String) -> Unit
) {
    val isTrailer = MifareParser.isSectorTrailer(blockIndex)
    var localHex by remember(sectorIndex, blockIndex, hex) { mutableStateOf(hex) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(DarkSurfaceElevated, RoundedCornerShape(4.dp))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.mifare_block_label, blockIndex),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = if (isTrailer) NeonOrange else MatrixGreen
            )
            if (isTrailer) {
                Text(
                    stringResource(R.string.mifare_trailer_hint),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 7.sp,
                    color = NeonOrange.copy(alpha = 0.8f)
                )
            }
        }
        OutlinedTextField(
            value = MifareParser.formatBlockDisplay(localHex),
            onValueChange = { input ->
                val normalized = MifareParser.normalizeBlockHex(input)
                localHex = normalized
                onBlockChanged(sectorIndex, blockIndex, normalized)
            },
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = MatrixGreen
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isTrailer) NeonOrange else MatrixGreen,
                unfocusedBorderColor = TextMuted,
                focusedTextColor = MatrixGreen,
                unfocusedTextColor = MatrixGreen,
                disabledTextColor = TextGray
            )
        )
    }
}
