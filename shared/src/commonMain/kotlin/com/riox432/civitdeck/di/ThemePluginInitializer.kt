package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.theme.JsonThemePlugin
import com.riox432.civitdeck.data.theme.ThemeDefinition
import com.riox432.civitdeck.domain.model.InstalledPluginState
import com.riox432.civitdeck.domain.model.InstalledPluginType
import com.riox432.civitdeck.domain.repository.PluginRepository
import com.riox432.civitdeck.plugin.PluginRegistry
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

/**
 * Restores previously imported theme plugins from the database on app startup.
 * Should be called once after Koin initialization.
 */
suspend fun registerThemePlugins() {
    val koin = org.koin.mp.KoinPlatform.getKoin()
    val pluginRepository: PluginRepository = koin.get()
    val registry: PluginRegistry = koin.get()

    val installedPlugins = pluginRepository.observeAll().first()
    val themePlugins = installedPlugins.filter {
        it.pluginType == InstalledPluginType.THEME
    }

    for (installed in themePlugins) {
        val definition = runCatching {
            json.decodeFromString<ThemeDefinition>(installed.configJson)
        }.getOrNull() ?: continue

        val plugin = JsonThemePlugin(definition)
        registry.register(plugin)

        // Restore active state if it was active before
        if (installed.state == InstalledPluginState.ACTIVE) {
            plugin.activate()
        }
    }
}
