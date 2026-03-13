package com.riox432.civitdeck.usecase

import com.riox432.civitdeck.plugin.PluginRegistry
import com.riox432.civitdeck.plugin.ThemeColorScheme
import com.riox432.civitdeck.plugin.ThemePlugin
import com.riox432.civitdeck.plugin.model.PluginState
import com.riox432.civitdeck.plugin.model.PluginType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Observes the currently active ThemePlugin and provides its color scheme.
 * Returns null if no custom theme is active (fallback to built-in).
 */
class GetActiveThemeUseCase(
    private val pluginRegistry: PluginRegistry,
) {
    operator fun invoke(): Flow<ThemePlugin?> =
        pluginRegistry.observePluginsByType(PluginType.THEME)
            .map { plugins ->
                plugins.filterIsInstance<ThemePlugin>()
                    .firstOrNull { it.state == PluginState.ACTIVE }
            }

    /**
     * Returns a flow of ThemeColorScheme for the active theme,
     * or null if no custom theme is active.
     */
    fun observeColorScheme(isDark: Boolean): Flow<ThemeColorScheme?> =
        invoke().map { it?.getColorScheme(isDark) }
}
