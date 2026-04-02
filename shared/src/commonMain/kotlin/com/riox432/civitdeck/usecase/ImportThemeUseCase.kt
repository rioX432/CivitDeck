package com.riox432.civitdeck.usecase

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.theme.JsonThemePlugin
import com.riox432.civitdeck.data.theme.ThemeDefinition
import com.riox432.civitdeck.domain.model.InstalledPlugin
import com.riox432.civitdeck.domain.model.InstalledPluginState
import com.riox432.civitdeck.domain.model.InstalledPluginType
import com.riox432.civitdeck.domain.repository.PluginRepository
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.plugin.PluginRegistry
import com.riox432.civitdeck.plugin.capability.ThemeCapability
import kotlinx.serialization.json.Json

/**
 * Parses a JSON theme string, validates it, persists it as a plugin, and registers it.
 */
class ImportThemeUseCase(
    private val pluginRepository: PluginRepository,
    private val pluginRegistry: PluginRegistry,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(jsonString: String): Result<String> = suspendRunCatching {
        val definition = json.decodeFromString<ThemeDefinition>(jsonString)
        val plugin = JsonThemePlugin(definition)
        val pluginId = plugin.manifest.id
        val now = currentTimeMillis()

        // Persist to DB
        pluginRepository.install(
            InstalledPlugin(
                id = pluginId,
                name = plugin.manifest.name,
                version = plugin.manifest.version,
                author = plugin.manifest.author,
                description = plugin.manifest.description,
                pluginType = InstalledPluginType.THEME,
                capabilities = listOf(ThemeCapability.COLOR_SCHEME.name),
                minAppVersion = "",
                state = InstalledPluginState.INSTALLED,
                configJson = jsonString,
                installedAt = now,
                updatedAt = now,
            ),
        )

        // Register in memory
        pluginRegistry.register(plugin)

        pluginId
    }
}
