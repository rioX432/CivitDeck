package com.riox432.civitdeck.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.riox432.civitdeck.domain.model.AccentColor

@Composable
fun CivitDeckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: AccentColor = AccentColor.Blue,
    amoledDarkMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseScheme = accentColorScheme(accentColor, darkTheme)
    val colorScheme = if (darkTheme && amoledDarkMode) baseScheme.withAmoled() else baseScheme

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
