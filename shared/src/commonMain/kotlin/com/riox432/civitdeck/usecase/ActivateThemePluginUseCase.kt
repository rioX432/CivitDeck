package com.riox432.civitdeck.usecase

import com.riox432.civitdeck.domain.model.InstalledPluginState
import com.riox432.civitdeck.domain.repository.PluginRepository
import com.riox432.civitdeck.plugin.PluginRegistry
import com.riox432.civitdeck.plugin.ThemePlugin
import com.riox432.civitdeck.plugin.model.PluginState
import com.riox432.civitdeck.plugin.model.PluginType

/**
 * Activates a theme plugin, ensuring only one is active at a time.
 * Pass null pluginId to deactivate all custom themes (revert to built-in).
 */
class ActivateThemePluginUseCase(
    private val pluginRegistry: PluginRegistry,
    private val pluginRepository: PluginRepository,
) {
    suspend operator fun invoke(pluginId: String?) {
        // Deactivate all currently active theme plugins
        val themePlugins = pluginRegistry.getPluginsByType(PluginType.THEME)
        for (plugin in themePlugins) {
            if (plugin.state == PluginState.ACTIVE) {
                plugin.deactivate()
                pluginRepository.updateState(
                    plugin.manifest.id,
                    InstalledPluginState.INACTIVE,
                )
            }
        }

        // Activate the requested one
        if (pluginId != null) {
            val target = pluginRegistry.getPlugin(pluginId)
            if (target is ThemePlugin) {
                target.activate()
                pluginRepository.updateState(pluginId, InstalledPluginState.ACTIVE)
            }
        }
    }
}
