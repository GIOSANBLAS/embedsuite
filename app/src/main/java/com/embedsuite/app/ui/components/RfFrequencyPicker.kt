package com.embedsuite.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.rf.RfFrequencyPresets
import com.embedsuite.app.ui.theme.BlackAMOLED
import com.embedsuite.app.ui.theme.DarkSurface
import com.embedsuite.app.ui.theme.MatrixGreen
import com.embedsuite.app.ui.theme.TextGray

@Composable
fun RfFrequencyPicker(
    selectedMhz: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "FRECUENCIA CC1101"
) {
    Text(
        label,
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        color = TextGray,
        modifier = modifier.padding(bottom = 4.dp)
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        RfFrequencyPresets.PRESETS.forEach { mhz ->
            FilterChip(
                selected = selectedMhz == mhz,
                onClick = { onSelected(mhz) },
                label = {
                    Text(
                        "${mhz}M",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = if (selectedMhz == mhz) BlackAMOLED else MatrixGreen
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MatrixGreen,
                    containerColor = DarkSurface
                )
            )
        }
    }
}
