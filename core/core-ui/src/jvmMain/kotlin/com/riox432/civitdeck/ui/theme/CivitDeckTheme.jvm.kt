package com.riox432.civitdeck.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
internal actual fun ApplyPlatformTheming(colorScheme: ColorScheme, darkTheme: Boolean) {
    // No-op on JVM/Desktop — no platform-specific theming
}
