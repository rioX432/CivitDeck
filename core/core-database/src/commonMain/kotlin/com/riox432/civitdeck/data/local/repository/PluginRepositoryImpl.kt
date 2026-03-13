package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.PluginDao
import com.riox432.civitdeck.data.local.entity.PluginEntity
import com.riox432.civitdeck.domain.model.InstalledPlugin
import com.riox432.civitdeck.domain.model.InstalledPluginState
import com.riox432.civitdeck.domain.model.InstalledPluginType
import com.riox432.civitdeck.domain.repository.PluginRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PluginRepositoryImpl(
    private val dao: PluginDao,
) : PluginRepository {

    override fun observeAll(): Flow<List<InstalledPlugin>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeById(pluginId: String): Flow<InstalledPlugin?> =
        dao.observeById(pluginId).map { it?.toDomain() }

    override suspend fun getById(pluginId: String): InstalledPlugin? =
        dao.getById(pluginId)?.toDomain()

    override suspend fun install(plugin: InstalledPlugin) {
        val now = currentTimeMillis()
        dao.insert(plugin.toEntity(installedAt = now, updatedAt = now))
    }

    override suspend fun uninstall(pluginId: String) {
        dao.delete(pluginId)
    }

    override suspend fun updateState(pluginId: String, state: InstalledPluginState) {
        dao.updateState(pluginId, state.name, currentTimeMillis())
    }

    override suspend fun getConfig(pluginId: String): String =
        dao.getById(pluginId)?.configJson ?: "{}"

    override suspend fun updateConfig(pluginId: String, configJson: String) {
        dao.updateConfig(pluginId, configJson, currentTimeMillis())
    }

    private fun PluginEntity.toDomain() = InstalledPlugin(
        id = id,
        name = name,
        version = version,
        author = author,
        description = description,
        pluginType = InstalledPluginType.valueOf(pluginType),
        capabilities = if (capabilities.isBlank()) emptyList() else capabilities.split(","),
        minAppVersion = minAppVersion,
        state = InstalledPluginState.valueOf(state),
        configJson = configJson,
        installedAt = installedAt,
        updatedAt = updatedAt,
    )

    private fun InstalledPlugin.toEntity(installedAt: Long, updatedAt: Long) = PluginEntity(
        id = id,
        name = name,
        version = version,
        author = author,
        description = description,
        pluginType = pluginType.name,
        capabilities = capabilities.joinToString(","),
        minAppVersion = minAppVersion,
        state = state.name,
        configJson = configJson,
        installedAt = installedAt,
        updatedAt = updatedAt,
    )
}
