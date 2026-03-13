package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.InstalledPlugin
import com.riox432.civitdeck.domain.model.InstalledPluginState
import kotlinx.coroutines.flow.Flow

interface PluginRepository {
    fun observeAll(): Flow<List<InstalledPlugin>>
    fun observeById(pluginId: String): Flow<InstalledPlugin?>
    suspend fun getById(pluginId: String): InstalledPlugin?
    suspend fun install(plugin: InstalledPlugin)
    suspend fun uninstall(pluginId: String)
    suspend fun updateState(pluginId: String, state: InstalledPluginState)
    suspend fun getConfig(pluginId: String): String
    suspend fun updateConfig(pluginId: String, configJson: String)
}
