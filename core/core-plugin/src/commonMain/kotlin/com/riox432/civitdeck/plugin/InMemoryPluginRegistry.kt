package com.riox432.civitdeck.plugin

import com.riox432.civitdeck.plugin.model.PluginError
import com.riox432.civitdeck.plugin.model.PluginType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryPluginRegistry : PluginRegistry {

    private val plugins = MutableStateFlow<Map<String, Plugin>>(emptyMap())

    override fun register(plugin: Plugin): Result<Unit> {
        val id = plugin.manifest.id
        if (plugins.value.containsKey(id)) {
            return Result.failure(PluginError.AlreadyRegistered(id))
        }
        plugins.update { it + (id to plugin) }
        return Result.success(Unit)
    }

    override fun unregister(pluginId: String): Result<Unit> {
        if (!plugins.value.containsKey(pluginId)) {
            return Result.failure(PluginError.NotFound(pluginId))
        }
        plugins.update { it - pluginId }
        return Result.success(Unit)
    }

    override fun getPlugin(pluginId: String): Plugin? =
        plugins.value[pluginId]

    override fun getPluginsByType(type: PluginType): List<Plugin> =
        plugins.value.values.filter { it.manifest.pluginType == type }

    override fun observePlugins(): Flow<List<Plugin>> =
        plugins.map { it.values.toList() }

    override fun observePluginsByType(type: PluginType): Flow<List<Plugin>> =
        plugins.map { map -> map.values.filter { it.manifest.pluginType == type } }
}
