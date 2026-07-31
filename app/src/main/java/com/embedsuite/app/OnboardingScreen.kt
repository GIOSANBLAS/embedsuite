package com.embedsuite.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.ui.components.NeonButton
import com.embedsuite.app.ui.theme.*

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }
    var legalAccepted by remember { mutableStateOf(false) }
    val pages = listOf(
        Triple(
            stringResource(R.string.onboard_connect_title),
            stringResource(R.string.onboard_connect_body),
            "①"
        ),
        Triple(
            stringResource(R.string.onboard_capture_title),
            stringResource(R.string.onboard_capture_body),
            "②"
        ),
        Triple(
            stringResource(R.string.onboard_library_title),
            stringResource(R.string.onboard_library_body),
            "③"
        ),
        Triple(
            stringResource(R.string.onboard_legal_title),
            stringResource(R.string.onboard_legal_body),
            "⚠"
        )
    )
    val lastPage = page == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackAMOLED)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(pages[page].third, fontSize = 48.sp, color = MatrixGreen)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            pages[page].first,
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MatrixGreen,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            pages[page].second,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = TextGray,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        if (lastPage) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = legalAccepted,
                    onCheckedChange = { legalAccepted = it },
                    colors = CheckboxDefaults.colors(checkedColor = MatrixGreen)
                )
                Text(
                    stringResource(R.string.onboard_legal_accept),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NeonOrange,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pages.indices.forEach { i ->
                Box(
                    Modifier
                        .size(if (i == page) 10.dp else 6.dp)
                        .background(
                            if (i == page) MatrixGreen else TextMuted,
                            androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        NeonButton(
            text = if (!lastPage) {
                stringResource(R.string.onboard_next)
            } else {
                stringResource(R.string.onboard_start)
            },
            enabled = !lastPage || legalAccepted,
            onClick = {
                if (!lastPage) page++ else if (legalAccepted) onComplete()
            },
            modifier = Modifier.fillMaxWidth(0.7f)
        )
        if (page > 0) {
            TextButton(onClick = { page-- }) {
                Text(
                    stringResource(R.string.action_back),
                    fontFamily = FontFamily.Monospace,
                    color = TextGray
                )
            }
        }
    }
}
