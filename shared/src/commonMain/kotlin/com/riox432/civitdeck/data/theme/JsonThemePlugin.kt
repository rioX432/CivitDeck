package com.riox432.civitdeck.data.theme

import com.riox432.civitdeck.plugin.ThemeColorScheme
import com.riox432.civitdeck.plugin.ThemePlugin
import com.riox432.civitdeck.plugin.capability.ThemeCapability
import com.riox432.civitdeck.plugin.model.PluginManifest
import com.riox432.civitdeck.plugin.model.PluginState
import com.riox432.civitdeck.plugin.model.PluginType

/**
 * A ThemePlugin backed by a parsed [ThemeDefinition] from JSON.
 * Provides light and dark color schemes for custom imported themes.
 */
class JsonThemePlugin(
    private val definition: ThemeDefinition,
) : ThemePlugin {

    override val themeCapabilities: Set<ThemeCapability> = setOf(
        ThemeCapability.COLOR_SCHEME,
    )

    override val manifest = PluginManifest(
        id = "theme.${definition.id}",
        name = definition.name,
        version = definition.version,
        author = definition.author,
        description = "Custom theme: ${definition.name}",
        pluginType = PluginType.THEME,
        capabilities = listOf(ThemeCapability.COLOR_SCHEME.name),
    )

    override var state: PluginState = PluginState.INSTALLED
        private set

    override fun getColorScheme(isDark: Boolean): ThemeColorScheme {
        val colors = if (isDark) definition.dark else definition.light
        return colors.toThemeColorScheme()
    }

    override suspend fun initialize() { /* JSON-based — no-op */ }

    override suspend fun activate() {
        state = PluginState.ACTIVE
    }

    override suspend fun deactivate() {
        state = PluginState.INACTIVE
    }

    override suspend fun destroy() {
        state = PluginState.INSTALLED
    }
}

private fun ThemeColors.toThemeColorScheme() = ThemeColorScheme(
    primary = parseHexColor(primary),
    onPrimary = parseHexColor(onPrimary),
    primaryContainer = parseHexColor(primaryContainer),
    onPrimaryContainer = parseHexColor(onPrimaryContainer),
    secondary = parseHexColor(secondary),
    onSecondary = parseHexColor(onSecondary),
    secondaryContainer = parseHexColor(secondaryContainer),
    onSecondaryContainer = parseHexColor(onSecondaryContainer),
    tertiary = parseHexColor(tertiary),
    onTertiary = parseHexColor(onTertiary),
    tertiaryContainer = parseHexColor(tertiaryContainer),
    onTertiaryContainer = parseHexColor(onTertiaryContainer),
    background = parseHexColor(background),
    onBackground = parseHexColor(onBackground),
    surface = parseHexColor(surface),
    onSurface = parseHexColor(onSurface),
    surfaceVariant = parseHexColor(surfaceVariant),
    onSurfaceVariant = parseHexColor(onSurfaceVariant),
    error = parseHexColor(error),
    onError = parseHexColor(onError),
    errorContainer = parseHexColor(errorContainer),
    onErrorContainer = parseHexColor(onErrorContainer),
    outline = parseHexColor(outline),
    outlineVariant = parseHexColor(outlineVariant),
)
