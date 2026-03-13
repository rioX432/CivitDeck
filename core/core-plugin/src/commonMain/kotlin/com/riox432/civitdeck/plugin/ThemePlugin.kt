package com.riox432.civitdeck.plugin

import com.riox432.civitdeck.plugin.capability.ThemeCapability

/**
 * Specialized plugin interface for custom theme integrations.
 * Plugins implement this to provide custom color schemes, typography, and shapes.
 */
interface ThemePlugin : Plugin {
    val themeCapabilities: Set<ThemeCapability>
    fun getColorScheme(isDark: Boolean): ThemeColorScheme
}

/**
 * Platform-agnostic color scheme definition using ARGB Long values.
 * Platform code (Compose / SwiftUI) converts these to native Color types.
 */
data class ThemeColorScheme(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val onSecondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val tertiary: Long,
    val onTertiary: Long,
    val tertiaryContainer: Long,
    val onTertiaryContainer: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val error: Long,
    val onError: Long,
    val errorContainer: Long,
    val onErrorContainer: Long,
    val outline: Long,
    val outlineVariant: Long,
)
