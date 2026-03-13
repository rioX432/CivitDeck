package com.riox432.civitdeck.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.plugin.ThemeColorScheme

@Composable
fun CivitDeckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: AccentColor = AccentColor.Blue,
    amoledDarkMode: Boolean = false,
    customTheme: ThemeColorScheme? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = resolveColorScheme(darkTheme, accentColor, amoledDarkMode, customTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CivitDeckTypography,
        shapes = CivitDeckShapes,
        content = content,
    )
}

private fun resolveColorScheme(
    darkTheme: Boolean,
    accentColor: AccentColor,
    amoledDarkMode: Boolean,
    customTheme: ThemeColorScheme?,
): ColorScheme {
    if (customTheme != null) {
        return customTheme.toMaterialColorScheme(darkTheme, amoledDarkMode)
    }
    val baseScheme = accentColorScheme(accentColor, darkTheme)
    return if (darkTheme && amoledDarkMode) baseScheme.withAmoled() else baseScheme
}

/**
 * Converts a plugin [ThemeColorScheme] to a Material3 [ColorScheme].
 */
private fun ThemeColorScheme.toMaterialColorScheme(
    isDark: Boolean,
    amoledDarkMode: Boolean,
): ColorScheme {
    val base = if (isDark) CivitDeckDarkColorScheme else CivitDeckLightColorScheme
    val scheme = base.copy(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant),
    )
    return if (isDark && amoledDarkMode) scheme.withAmoled() else scheme
}
