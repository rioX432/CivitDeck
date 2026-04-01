package com.riox432.civitdeck.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.riox432.civitdeck.domain.model.AccentColor

// Generated from seed #1e40af using Material Color Utilities (TonalSpot)
val CivitDeckLightColorScheme = lightColorScheme(
    primary = Color(0xFF3755C3),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF001453),
    secondary = Color(0xFF5A5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDEE1F9),
    onSecondaryContainer = Color(0xFF171B2C),
    tertiary = Color(0xFF75546F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD7F4),
    onTertiaryContainer = Color(0xFF2C1229),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBF8FD),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFBF8FD),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE2E1EC),
    onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF767680),
    outlineVariant = Color(0xFFC6C5D0),
    inverseSurface = Color(0xFF303034),
    inverseOnSurface = Color(0xFFF2F0F4),
    inversePrimary = Color(0xFFB8C4FF),
    surfaceDim = Color(0xFFDBD9DE),
    surfaceBright = Color(0xFFFBF8FD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F3F7),
    surfaceContainer = Color(0xFFF0EDF1),
    surfaceContainerHigh = Color(0xFFEAE7EC),
    surfaceContainerHighest = Color(0xFFE4E1E6),
)

val CivitDeckDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB8C4FF),
    onPrimary = Color(0xFF002584),
    primaryContainer = Color(0xFF173BAB),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFC2C5DD),
    onSecondary = Color(0xFF2C2F42),
    secondaryContainer = Color(0xFF424659),
    onSecondaryContainer = Color(0xFFDEE1F9),
    tertiary = Color(0xFFE4BAD9),
    onTertiary = Color(0xFF43273F),
    tertiaryContainer = Color(0xFF5C3D56),
    onTertiaryContainer = Color(0xFFFFD7F4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF131316),
    onBackground = Color(0xFFE4E1E6),
    surface = Color(0xFF131316),
    onSurface = Color(0xFFE4E1E6),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC6C5D0),
    outline = Color(0xFF90909A),
    outlineVariant = Color(0xFF45464F),
    inverseSurface = Color(0xFFE4E1E6),
    inverseOnSurface = Color(0xFF303034),
    inversePrimary = Color(0xFF3755C3),
    surfaceDim = Color(0xFF131316),
    surfaceBright = Color(0xFF39393C),
    surfaceContainerLowest = Color(0xFF0E0E11),
    surfaceContainerLow = Color(0xFF1B1B1F),
    surfaceContainer = Color(0xFF1F1F23),
    surfaceContainerHigh = Color(0xFF2A2A2D),
    surfaceContainerHighest = Color(0xFF343438),
)

// Accent color primary overrides for light/dark themes
private data class AccentPrimaries(
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
    val lightInversePrimary: Color,
    val darkInversePrimary: Color,
)

private val accentPrimariesMap = mapOf(
    AccentColor.Blue to AccentPrimaries(
        lightPrimary = Color(0xFF3755C3), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFDDE1FF), lightOnPrimaryContainer = Color(0xFF001453),
        darkPrimary = Color(0xFFB8C4FF), darkOnPrimary = Color(0xFF002584),
        darkPrimaryContainer = Color(0xFF173BAB), darkOnPrimaryContainer = Color(0xFFDDE1FF),
        lightInversePrimary = Color(0xFFB8C4FF), darkInversePrimary = Color(0xFF3755C3),
    ),
    AccentColor.Indigo to AccentPrimaries(
        lightPrimary = Color(0xFF5856D6), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFE3DFFF), lightOnPrimaryContainer = Color(0xFF17005A),
        darkPrimary = Color(0xFFC5C0FF), darkOnPrimary = Color(0xFF2C0090),
        darkPrimaryContainer = Color(0xFF4336BE), darkOnPrimaryContainer = Color(0xFFE3DFFF),
        lightInversePrimary = Color(0xFFC5C0FF), darkInversePrimary = Color(0xFF5856D6),
    ),
    AccentColor.Purple to AccentPrimaries(
        lightPrimary = Color(0xFF7C3AED), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFECDDFF), lightOnPrimaryContainer = Color(0xFF250060),
        darkPrimary = Color(0xFFD4BBFF), darkOnPrimary = Color(0xFF3E0099),
        darkPrimaryContainer = Color(0xFF5F13D5), darkOnPrimaryContainer = Color(0xFFECDDFF),
        lightInversePrimary = Color(0xFFD4BBFF), darkInversePrimary = Color(0xFF7C3AED),
    ),
    AccentColor.Pink to AccentPrimaries(
        lightPrimary = Color(0xFFDB2777), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFFFD9E2), lightOnPrimaryContainer = Color(0xFF3F0021),
        darkPrimary = Color(0xFFFFB1C8), darkOnPrimary = Color(0xFF630038),
        darkPrimaryContainer = Color(0xFFA50054), darkOnPrimaryContainer = Color(0xFFFFD9E2),
        lightInversePrimary = Color(0xFFFFB1C8), darkInversePrimary = Color(0xFFDB2777),
    ),
    AccentColor.Red to AccentPrimaries(
        lightPrimary = Color(0xFFDC2626), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFFFDAD6), lightOnPrimaryContainer = Color(0xFF410002),
        darkPrimary = Color(0xFFFFB4AB), darkOnPrimary = Color(0xFF690005),
        darkPrimaryContainer = Color(0xFF930010), darkOnPrimaryContainer = Color(0xFFFFDAD6),
        lightInversePrimary = Color(0xFFFFB4AB), darkInversePrimary = Color(0xFFDC2626),
    ),
    AccentColor.Orange to AccentPrimaries(
        lightPrimary = Color(0xFFEA580C), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFFFDBC9), lightOnPrimaryContainer = Color(0xFF341000),
        darkPrimary = Color(0xFFFFB68E), darkOnPrimary = Color(0xFF552100),
        darkPrimaryContainer = Color(0xFFBB4200), darkOnPrimaryContainer = Color(0xFFFFDBC9),
        lightInversePrimary = Color(0xFFFFB68E), darkInversePrimary = Color(0xFFEA580C),
    ),
    AccentColor.Amber to AccentPrimaries(
        lightPrimary = Color(0xFFD97706), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFFFDDB3), lightOnPrimaryContainer = Color(0xFF2A1700),
        darkPrimary = Color(0xFFFFBB3D), darkOnPrimary = Color(0xFF462B00),
        darkPrimaryContainer = Color(0xFFA95D00), darkOnPrimaryContainer = Color(0xFFFFDDB3),
        lightInversePrimary = Color(0xFFFFBB3D), darkInversePrimary = Color(0xFFD97706),
    ),
    AccentColor.Green to AccentPrimaries(
        lightPrimary = Color(0xFF16A34A), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFA6F5C0), lightOnPrimaryContainer = Color(0xFF002110),
        darkPrimary = Color(0xFF6CE892), darkOnPrimary = Color(0xFF00391C),
        darkPrimaryContainer = Color(0xFF005230), darkOnPrimaryContainer = Color(0xFFA6F5C0),
        lightInversePrimary = Color(0xFF6CE892), darkInversePrimary = Color(0xFF16A34A),
    ),
    AccentColor.Teal to AccentPrimaries(
        lightPrimary = Color(0xFF0D9488), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFA0F0E8), lightOnPrimaryContainer = Color(0xFF002019),
        darkPrimary = Color(0xFF4EDBC8), darkOnPrimary = Color(0xFF00382E),
        darkPrimaryContainer = Color(0xFF005144), darkOnPrimaryContainer = Color(0xFFA0F0E8),
        lightInversePrimary = Color(0xFF4EDBC8), darkInversePrimary = Color(0xFF0D9488),
    ),
    AccentColor.Cyan to AccentPrimaries(
        lightPrimary = Color(0xFF0891B2), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFB8EAFF), lightOnPrimaryContainer = Color(0xFF001F29),
        darkPrimary = Color(0xFF5DD5FC), darkOnPrimary = Color(0xFF003544),
        darkPrimaryContainer = Color(0xFF004D63), darkOnPrimaryContainer = Color(0xFFB8EAFF),
        lightInversePrimary = Color(0xFF5DD5FC), darkInversePrimary = Color(0xFF0891B2),
    ),
)

/** Apply accent color overrides to the base light/dark schemes. */
fun accentColorScheme(
    accent: AccentColor,
    isDark: Boolean,
): ColorScheme {
    val base = if (isDark) CivitDeckDarkColorScheme else CivitDeckLightColorScheme
    if (accent == AccentColor.Blue) return base
    val p = accentPrimariesMap[accent] ?: return base
    return if (isDark) {
        base.copy(
            primary = p.darkPrimary,
            onPrimary = p.darkOnPrimary,
            primaryContainer = p.darkPrimaryContainer,
            onPrimaryContainer = p.darkOnPrimaryContainer,
            inversePrimary = p.darkInversePrimary,
        )
    } else {
        base.copy(
            primary = p.lightPrimary,
            onPrimary = p.lightOnPrimary,
            primaryContainer = p.lightPrimaryContainer,
            onPrimaryContainer = p.lightOnPrimaryContainer,
            inversePrimary = p.lightInversePrimary,
        )
    }
}

/** Apply AMOLED pure black overrides to a dark color scheme. */
fun ColorScheme.withAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF1A1A1A),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF0F0F0F),
    surfaceContainerHigh = Color(0xFF161616),
    surfaceContainerHighest = Color(0xFF1E1E1E),
)

/** Fixed semantic color tokens that do not vary with the Material3 dynamic color pipeline. */
object CivitDeckColors {
    // Status indicator colors (connection/download state dots)
    val statusSuccess = Color(0xFF4CAF50)
    val statusWarning = Color(0xFFFFC107)
    val statusError = Color(0xFFF44336)
    val statusNeutral = Color(0xFF9E9E9E)

    // Tutorial accent colors (fixed per step, not tied to user accent selection)
    val tutorialAccentBlue = Color(0xFF3755C3)
    val tutorialAccentPink = Color(0xFF75546F)
    val tutorialAccentGray = Color(0xFF5A5D72)

    // Model source badge colors (fixed brand colors)
    val huggingFaceBadge = Color(0xFFFF9D00)
    val tensorArtBadge = Color(0xFF9C27B0)

    // Full-screen overlay / cinema-mode colors
    // These are intentionally fixed (not theme-adaptive) for maximum contrast in dark contexts.
    val scrim = Color.Black
    val onScrim = Color.White
}
