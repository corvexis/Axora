package frb.axeron.manager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import frb.axeron.manager.R

private val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold),
)

private val CaskaydiaMonoFamily = FontFamily(
    Font(R.font.caskaydia_mono, FontWeight.Normal),
    Font(R.font.caskaydia_mono, FontWeight.Medium),
    Font(R.font.caskaydia_mono, FontWeight.Bold),
)

private val OverpassFamily = FontFamily(
    Font(R.font.overpass, FontWeight.Normal),
    Font(R.font.overpass, FontWeight.Medium),
    Font(R.font.overpass, FontWeight.Bold),
)

private val FiraCodeFamily = FontFamily(
    Font(R.font.firacode, FontWeight.Normal),
    Font(R.font.firacode, FontWeight.Medium),
    Font(R.font.firacode, FontWeight.Bold),
)

private val HurmitFamily = FontFamily(
    Font(R.font.hurmit, FontWeight.Normal),
    Font(R.font.hurmit, FontWeight.Medium),
    Font(R.font.hurmit, FontWeight.Bold),
)

private val HeavyDataFamily = FontFamily(
    Font(R.font.heavydata, FontWeight.Normal),
    Font(R.font.heavydata, FontWeight.Medium),
    Font(R.font.heavydata, FontWeight.Bold),
)

private val DroidSansMonoFamily = FontFamily(
    Font(R.font.droid_sans_mono, FontWeight.Normal),
    Font(R.font.droid_sans_mono, FontWeight.Medium),
    Font(R.font.droid_sans_mono, FontWeight.Bold),
)

private fun createTypography(fontFamily: FontFamily) = Typography(
    displayLarge = TextStyle(fontFamily = fontFamily),
    displayMedium = TextStyle(fontFamily = fontFamily),
    displaySmall = TextStyle(fontFamily = fontFamily),
    headlineLarge = TextStyle(fontFamily = fontFamily),
    headlineMedium = TextStyle(fontFamily = fontFamily),
    headlineSmall = TextStyle(fontFamily = fontFamily),
    titleLarge = TextStyle(fontFamily = fontFamily),
    titleMedium = TextStyle(fontFamily = fontFamily),
    titleSmall = TextStyle(fontFamily = fontFamily),
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(fontFamily = fontFamily),
    bodySmall = TextStyle(fontFamily = fontFamily),
    labelLarge = TextStyle(fontFamily = fontFamily),
    labelMedium = TextStyle(fontFamily = fontFamily),
    labelSmall = TextStyle(fontFamily = fontFamily),
)

val fontTypographyMap: Map<String, Typography> = mapOf(
    "jetbrains_mono" to createTypography(JetBrainsMonoFamily),
    "caskaydia_mono" to createTypography(CaskaydiaMonoFamily),
    "overpass" to createTypography(OverpassFamily),
    "firacode" to createTypography(FiraCodeFamily),
    "hurmit" to createTypography(HurmitFamily),
    "heavydata" to createTypography(HeavyDataFamily),
    "droid_sans_mono" to createTypography(DroidSansMonoFamily),
)

val SystemTypography = Typography()
