package com.jackson4rocks.evexia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.jackson4rocks.evexia.R
import com.jackson4rocks.evexia.data.AppPreferences

@Immutable
data class AccentChoice(
    val id: String,
    val label: String,
    val fill: Color,
    val deep: Color,
)

val AccentChoices = listOf(
    AccentChoice("LIME", "Citron", Color(0xFFD8FF63), Color(0xFF5D7200)),
    AccentChoice("SKY", "Sky", Color(0xFFA9D9FF), Color(0xFF1E5B84)),
    AccentChoice("PEACH", "Peach", Color(0xFFFFBE9E), Color(0xFF823D1E)),
    AccentChoice("LILAC", "Lilac", Color(0xFFCFC2FF), Color(0xFF513993)),
    AccentChoice("MINT", "Mint", Color(0xFFA6E8CC), Color(0xFF1B6348)),
    AccentChoice("MONOCHROME", "Monochrome", Color(0xFF9A9A9A), Color(0xFF626262)),
)

val SubjectPalette = listOf(
    Color(0xFFD8FF63),
    Color(0xFFFFB990),
    Color(0xFFC9C1FF),
    Color(0xFFA8E8D1),
    Color(0xFFFFD174),
    Color(0xFFFFB5C6),
    Color(0xFFB8DBFF),
    Color(0xFFD5DAC5),
)

@Immutable
data class AppColors(
    val dark: Boolean,
    val background: Color,
    val backgroundAlt: Color,
    val glass: Color,
    val glassStrong: Color,
    val glassQuiet: Color,
    val border: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val danger: Color,
    val dangerSoft: Color,
    val success: Color,
    val shadow: Color,
    val scrim: Color,
    val auraA: Color,
    val auraB: Color,
    val auraC: Color,
    val silhouette: Color,
)

@Immutable
data class AppTypography(
    val hero: TextStyle,
    val display: TextStyle,
    val serif: TextStyle,
    val serifItalic: TextStyle,
    val pageTitle: TextStyle,
    val sectionTitle: TextStyle,
    val cardTitle: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val caption: TextStyle,
    val label: TextStyle,
    val button: TextStyle,
    val numeric: TextStyle,
)

@Immutable
data class AppShapes(
    val hero: androidx.compose.ui.graphics.Shape,
    val card: androidx.compose.ui.graphics.Shape,
    val small: androidx.compose.ui.graphics.Shape,
    val pill: androidx.compose.ui.graphics.Shape,
)

val LocalColors = staticCompositionLocalOf<AppColors> {
    error("Evexia theme is missing")
}
val LocalType = staticCompositionLocalOf<AppTypography> {
    error("Evexia typography is missing")
}
val LocalGlassOpacity = staticCompositionLocalOf { 1f }
val LocalReduceMotion = staticCompositionLocalOf { false }

private val Sans = FontFamily(
    Font(R.font.instrument_sans, FontWeight.Normal),
    Font(R.font.instrument_sans, FontWeight.Medium),
    Font(R.font.instrument_sans, FontWeight.SemiBold),
    Font(R.font.instrument_sans, FontWeight.Bold),
)

private val Serif = FontFamily(
    Font(R.font.instrument_serif, FontWeight.Normal),
)

private val SerifItalicFamily = FontFamily(
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

private val Type = AppTypography(
    hero = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.2).sp,
    ),
    display = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp,
    ),
    serif = TextStyle(
        fontFamily = Serif,
        fontSize = 25.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.3).sp,
    ),
    serifItalic = TextStyle(
        fontFamily = SerifItalicFamily,
        fontStyle = FontStyle.Italic,
        fontSize = 25.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.2).sp,
    ),
    pageTitle = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.8).sp,
    ),
    sectionTitle = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.3).sp,
    ),
    cardTitle = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        letterSpacing = (-0.15).sp,
    ),
    body = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.05).sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    caption = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
    ),
    label = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.13.em,
    ),
    button = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp,
    ),
    numeric = TextStyle(
        fontFamily = Serif,
        fontSize = 54.sp,
        lineHeight = 54.sp,
        letterSpacing = (-1.4).sp,
    ),
)

private val Shapes = AppShapes(
    hero = androidx.compose.foundation.shape.RoundedCornerShape(34.dp, 34.dp, 34.dp, 12.dp),
    card = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    pill = androidx.compose.foundation.shape.CircleShape,
)

fun accentFor(id: String): AccentChoice = AccentChoices.firstOrNull { it.id == id } ?: AccentChoices.first()

@Composable
fun EvexiaTheme(
    preferences: AppPreferences,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val amoled = preferences.accent == "MONOCHROME" || preferences.themeMode == "AMOLED"
    val dark = when (preferences.themeMode) {
        "LIGHT" -> amoled
        "DARK", "AMOLED" -> true
        else -> amoled || systemDark
    }
    val accent = accentFor(preferences.accent)
    // Fixed medium translucency; the old intensity control was removed.
    val glass = 0.88f
    val colors = if (dark) {
        AppColors(
            dark = true,
            background = if (amoled) Color.Black else Color(0xFF0C0D0B),
            backgroundAlt = if (amoled) Color(0xFF050505) else Color(0xFF121310),
            glass = if (amoled) Color(0xB30A0A0A) else Color(0xB31B1D18),
            glassStrong = if (amoled) Color(0xE60C0C0C) else Color(0xE622241F),
            glassQuiet = if (amoled) Color(0x80070707) else Color(0x80141612),
            border = Color(0x24FFFFFF),
            borderStrong = Color(0x3DFFFFFF),
            textPrimary = Color(0xFFF4F1E8),
            textSecondary = Color(0xFFA6A79F),
            textTertiary = Color(0xFF777A71),
            accent = accent.fill,
            accentInk = accent.fill,
            accentSoft = accent.fill.copy(alpha = 0.16f),
            onAccent = Color(0xFF171916),
            danger = Color(0xFFFF9A86),
            dangerSoft = Color(0x26FF9A86),
            success = Color(0xFFB6E8B1),
            shadow = Color(0x99000000),
            scrim = Color(0x99000000),
            auraA = accent.fill.copy(alpha = 0.18f),
            auraB = Color(0xFF8F7DFF).copy(alpha = 0.13f),
            auraC = Color(0xFFFFB088).copy(alpha = 0.08f),
            silhouette = if (amoled) Color(0xFF101010) else Color(0xFF1C1E19),
        )
    } else {
        AppColors(
            dark = false,
            background = Color(0xFFF1EFE8),
            backgroundAlt = Color(0xFFE9E6DC),
            glass = Color(0xB8FFFFFF),
            glassStrong = Color(0xEDFFFFFF),
            glassQuiet = Color(0x75FFFFFF),
            border = Color(0x2B171916),
            borderStrong = Color(0x44171916),
            textPrimary = Color(0xFF171916),
            textSecondary = Color(0xFF63665D),
            textTertiary = Color(0xFF8C8F85),
            accent = accent.fill,
            accentInk = accent.deep,
            accentSoft = accent.fill.copy(alpha = 0.32f),
            onAccent = Color(0xFF171916),
            danger = Color(0xFFA63E2D),
            dangerSoft = Color(0x1CA63E2D),
            success = Color(0xFF3A6D45),
            shadow = Color(0x22171916),
            scrim = Color(0x660F100D),
            auraA = accent.fill.copy(alpha = 0.42f),
            auraB = Color(0xFFCFC2FF).copy(alpha = 0.32f),
            auraC = Color(0xFFFFB08D).copy(alpha = 0.22f),
            silhouette = Color(0xFFE4E0D5),
        )
    }

    CompositionLocalProvider(
        LocalColors provides colors,
        LocalType provides Type,
        LocalGlassOpacity provides glass,
        LocalReduceMotion provides preferences.reduceMotion,
    ) {
        content()
    }
}

/** Short accessors keep screen code readability high. */
object DF {
    val colors: AppColors
        @Composable get() = LocalColors.current
    val type: AppTypography
        @Composable get() = LocalType.current
}
