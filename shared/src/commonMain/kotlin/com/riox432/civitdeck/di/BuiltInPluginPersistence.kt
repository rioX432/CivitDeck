package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.domain.model.InstalledPlugin
import com.riox432.civitdeck.domain.model.InstalledPluginState
import com.riox432.civitdeck.domain.model.InstalledPluginType
import com.riox432.civitdeck.domain.repository.PluginRepository
import com.riox432.civitdeck.plugin.Plugin
import com.riox432.civitdeck.plugin.model.PluginType

/**
 * Persists a built-in plugin to the database if not already installed.
 * This ensures built-in plugins appear in the Plugin Management screen.
 */
internal suspend fun persistBuiltInPlugin(repository: PluginRepository, plugin: Plugin) {
    val existing = repository.getById(plugin.manifest.id)
    if (existing != null) return

    val installed = InstalledPlugin(
        id = plugin.manifest.id,
        name = plugin.manifest.name,
        version = plugin.manifest.version,
        author = plugin.manifest.author,
        description = plugin.manifest.description,
        pluginType = plugin.manifest.pluginType.toDomain(),
        capabilities = plugin.manifest.capabilities,
        minAppVersion = plugin.manifest.minAppVersion,
        state = InstalledPluginState.ACTIVE,
        configJson = "{}",
        installedAt = currentTimeMillis(),
        updatedAt = currentTimeMillis(),
    )
    repository.install(installed)
}

private fun PluginType.toDomain(): InstalledPluginType = when (this) {
    PluginType.WORKFLOW_ENGINE -> InstalledPluginType.WORKFLOW_ENGINE
    PluginType.EXPORT_FORMAT -> InstalledPluginType.EXPORT_FORMAT
    PluginType.THEME -> InstalledPluginType.THEME
}
