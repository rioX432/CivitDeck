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
        var alreadyPresent = false
        plugins.update { current ->
            if (current.containsKey(id)) {
                alreadyPresent = true
                current
            } else {
                alreadyPresent = false
                current + (id to plugin)
            }
        }
        return if (alreadyPresent) {
            Result.failure(PluginError.AlreadyRegistered(id))
        } else {
            Result.success(Unit)
        }
    }

    override fun unregister(pluginId: String): Result<Unit> {
        var wasPresent = false
        plugins.update { current ->
            if (current.containsKey(pluginId)) {
                wasPresent = true
                current - pluginId
            } else {
                wasPresent = false
                current
            }
        }
        return if (wasPresent) {
            Result.success(Unit)
        } else {
            Result.failure(PluginError.NotFound(pluginId))
        }
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
