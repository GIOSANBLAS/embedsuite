package com.embedsuite.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.ui.components.GlassCard
import com.embedsuite.app.ui.components.HackerSectionHeader
import com.embedsuite.app.ui.components.NeonOutlinedButton
import com.embedsuite.app.ui.theme.DarkSurface
import com.embedsuite.app.ui.theme.MatrixGreen
import com.embedsuite.app.ui.theme.NeonCyan
import com.embedsuite.app.ui.theme.NeonOrange
import com.embedsuite.app.ui.theme.TextGray
import com.embedsuite.app.ui.theme.TextMuted

private const val HARDWARE_TEST_SUITE_DOC_URL =
    "https://github.com/GIOSANBLAS/te-embed-xibalba/blob/main/docs/HARDWARE_TEST_SUITE.md"
private const val HARDWARE_TEST_PS1_URL =
    "https://github.com/GIOSANBLAS/te-embed-xibalba/blob/main/scripts/hardware_test_suite.ps1"
private const val HARDWARE_CHECKLIST_CSV_URL =
    "https://github.com/GIOSANBLAS/te-embed-xibalba/blob/main/examples/hardware_test_checklist.csv"

@Composable
fun HardwareBringupScreen(onBack: () -> Unit) {
    val context = LocalContext.current

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
                    stringResource(R.string.hardware_bringup_title),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixGreen
                )
                Text(
                    stringResource(R.string.hardware_bringup_subtitle),
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
            GlassCard(accent = NeonOrange, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                HackerSectionHeader(stringResource(R.string.hardware_bringup_flash_header), accent = NeonOrange)
                Text(
                    stringResource(R.string.hardware_bringup_flash_body),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextGray,
                    lineHeight = 14.sp
                )
            }

            GlassCard(accent = NeonCyan, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                HackerSectionHeader(stringResource(R.string.hardware_bringup_pairing_header), accent = NeonCyan)
                Text(
                    stringResource(R.string.hardware_bringup_pairing_body),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextGray,
                    lineHeight = 14.sp
                )
            }

            GlassCard(accent = MatrixGreen, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                HackerSectionHeader(stringResource(R.string.hardware_bringup_checklist_header), accent = MatrixGreen)
                Text(
                    stringResource(R.string.hardware_bringup_checklist_body),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextGray,
                    lineHeight = 14.sp
                )
            }

            NeonOutlinedButton(
                text = stringResource(R.string.hardware_bringup_test_log),
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(HARDWARE_TEST_SUITE_DOC_URL))
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Guía paso a paso del test suite 46 tests hardware (con soporte nativo en Xibalba 0.17+)",
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 6.dp)
            )

            NeonOutlinedButton(
                text = "Descargar test_suite.ps1",
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(HARDWARE_TEST_PS1_URL))
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            NeonOutlinedButton(
                text = "Checklist imprimible CSV",
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(HARDWARE_CHECKLIST_CSV_URL)))
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)
            )
        }
    }
}
