package com.embedsuite.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.ui.components.NeonButton
import com.embedsuite.app.ui.theme.MatrixGreen
import com.embedsuite.app.ui.theme.NeonOrange
import com.embedsuite.app.ui.theme.TextGray

@Composable
fun TehLinkPairingDialog(
    isMockMode: Boolean,
    onDismiss: () -> Unit,
    onSimulateLongPress: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.teh_link_pairing_title),
                fontFamily = FontFamily.Monospace,
                color = MatrixGreen
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.teh_link_pairing_step1),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.teh_link_pairing_step2),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.teh_link_pairing_step3),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextGray
                )
                if (isMockMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.teh_link_pairing_mock_hint),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NeonOrange
                    )
                }
            }
        },
        confirmButton = {
            NeonButton(
                text = stringResource(R.string.action_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(0.5f)
            )
        },
        dismissButton = if (isMockMode && onSimulateLongPress != null) {
            {
                TextButton(onClick = onSimulateLongPress) {
                    Text(
                        stringResource(R.string.teh_link_pairing_mock_btn),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NeonOrange
                    )
                }
            }
        } else null
    )
}
