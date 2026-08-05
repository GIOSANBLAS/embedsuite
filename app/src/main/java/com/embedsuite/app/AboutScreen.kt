package com.embedsuite.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.embedsuite.app.core.AppInfo
import com.embedsuite.app.core.AppVersion
import com.embedsuite.app.ui.components.GlassCard
import com.embedsuite.app.ui.components.HackerSectionHeader
import com.embedsuite.app.ui.components.LilyGoLogo
import com.embedsuite.app.ui.theme.*

private enum class AboutSubPage { MAIN, PRIVACY, LICENSES }

@Composable
fun AboutScreen(onBack: () -> Unit, onOpenManual: () -> Unit = {}) {
    var subPage by remember { mutableStateOf(AboutSubPage.MAIN) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface)
    ) {
        AboutTopBar(
            title = when (subPage) {
                AboutSubPage.MAIN -> stringResource(R.string.about_title)
                AboutSubPage.PRIVACY -> stringResource(R.string.about_privacy_title)
                AboutSubPage.LICENSES -> stringResource(R.string.about_licenses_title)
            },
            subtitle = when (subPage) {
                AboutSubPage.MAIN -> stringResource(R.string.about_subtitle)
                AboutSubPage.PRIVACY -> stringResource(R.string.about_privacy_sub)
                AboutSubPage.LICENSES -> stringResource(R.string.about_licenses_sub)
            },
            onBack = {
                if (subPage == AboutSubPage.MAIN) onBack()
                else subPage = AboutSubPage.MAIN
            }
        )

        when (subPage) {
            AboutSubPage.MAIN -> AboutMainContent(
                onOpenPrivacy = { subPage = AboutSubPage.PRIVACY },
                onOpenLicenses = { subPage = AboutSubPage.LICENSES },
                onOpenManual = onOpenManual
            )
            AboutSubPage.PRIVACY -> LegalTextContent(AppInfo.privacyPolicyText)
            AboutSubPage.LICENSES -> LicensesContent()
        }
    }
}

@Composable
private fun AboutTopBar(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MatrixGreen)
        }
        Column {
            Text(title, fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MatrixGreen)
            Text(subtitle, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray)
        }
    }
    HorizontalDivider(color = MatrixGreen.copy(alpha = 0.35f))
}

@Composable
private fun AboutMainContent(
    onOpenPrivacy: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenManual: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        GlassCard(accent = MatrixGreen, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                LilyGoLogo(logoSize = 124.dp, showSubtitle = false, brutal = true)
                Spacer(Modifier.height(8.dp))
                Text("EMBED SUITE", fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MatrixGreen)
                Text("v${AppVersion.NAME} (build ${AppVersion.CODE})", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NeonCyan)
                Text(AppInfo.tagline, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray, modifier = Modifier.padding(top = 6.dp))
            }
        }

        HackerSectionHeader(stringResource(R.string.about_manual_section), accent = MatrixGreen)
        GlassCard(accent = MatrixGreen, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Text(
                stringResource(R.string.about_manual_desc),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = TextGray
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenManual, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.about_manual_btn), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
            }
        }

        HackerSectionHeader("EQUIPO", accent = NeonCyan)
        Text(
            "Proyecto desarrollado en conjunto por GIOSÁNBLAS y Cursor.",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = TextGray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        AppInfo.team.forEach { member ->
            GlassCard(accent = KaliBlue, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(member.name, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MatrixGreen)
                Text(member.role, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NeonCyan)
                Text(member.detail, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray, modifier = Modifier.padding(top = 4.dp))
            }
        }

        HackerSectionHeader("REGISTRO DE CAMBIOS", accent = NeonOrange)
        AppInfo.changelog.forEach { entry ->
            GlassCard(accent = NeonOrange, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("v${entry.version}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                    Text(entry.date, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted)
                }
                entry.highlights.forEach { line ->
                    Text("▸ $line", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }

        HackerSectionHeader("LEGAL", accent = NeonCyan)
        GlassCard(accent = NeonCyan, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text(
                "EMBED SUITE es software companion de uso personal. No está afiliada a LilyGO ni Flipper.",
                fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextGray
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth()) {
                Text("Política de privacidad", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = onOpenLicenses, modifier = Modifier.fillMaxWidth()) {
                Text("Licencias open source", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixGreen)
            }
        }

        Text(
            "© 2026 GIOSÁNBLAS & Cursor · EMBED SUITE",
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = TextMuted,
            modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun LegalTextContent(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        GlassCard(accent = NeonCyan, modifier = Modifier.fillMaxWidth()) {
            text.lines().forEach { line ->
                if (line.isBlank()) Spacer(Modifier.height(6.dp))
                else Text(
                    line,
                    fontFamily = FontFamily.Monospace,
                    fontSize = if (line == line.uppercase() && line.length < 60) 11.sp else 9.sp,
                    fontWeight = if (line.matches(Regex("""^\d+\."""))) FontWeight.Bold else FontWeight.Normal,
                    color = if (line.startsWith("POLÍTICA") || line.startsWith("LICENCIAS")) MatrixGreen else TextGray,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun LicensesContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Text(
            "EMBED SUITE usa librerías open source. Agradecemos a sus autores.",
            fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextGray,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        AppInfo.openSourceLicenses.forEach { lib ->
            GlassCard(accent = KaliBlue, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(lib.name, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MatrixGreen)
                Text("Licencia: ${lib.license}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NeonCyan)
                Text(lib.url, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Text(
            "El código fuente de EMBED SUITE es propiedad del autor salvo componentes listados arriba.",
            fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
