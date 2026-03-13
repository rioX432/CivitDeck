package com.riox432.civitdeck.usecase

import com.riox432.civitdeck.plugin.PluginRegistry
import com.riox432.civitdeck.plugin.ThemePlugin
import com.riox432.civitdeck.plugin.model.PluginType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Observes all registered ThemePlugins (both built-in and imported).
 */
class ObserveThemePluginsUseCase(
    private val pluginRegistry: PluginRegistry,
) {
    operator fun invoke(): Flow<List<ThemePlugin>> =
        pluginRegistry.observePluginsByType(PluginType.THEME)
            .map { plugins -> plugins.filterIsInstance<ThemePlugin>() }
}
