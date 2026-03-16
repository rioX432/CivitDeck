package com.riox432.civitdeck.feature.externalserver.data.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.ExternalServerConfigDao
import com.riox432.civitdeck.data.local.entity.ExternalServerConfigEntity
import com.riox432.civitdeck.domain.model.ExternalServerConfig
import com.riox432.civitdeck.feature.externalserver.domain.repository.ExternalServerConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExternalServerConfigRepositoryImpl(
    private val dao: ExternalServerConfigDao,
) : ExternalServerConfigRepository {

    override fun observeConfigs(): Flow<List<ExternalServerConfig>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeActiveConfig(): Flow<ExternalServerConfig?> =
        dao.observeActive().map { it?.toDomain() }

    override suspend fun saveConfig(config: ExternalServerConfig): Long {
        val entity = config.toEntity()
        return if (config.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            config.id
        }
    }

    override suspend fun deleteConfig(id: Long) { dao.deleteById(id) }

    override suspend fun activateConfig(id: Long) {
        dao.deactivateAll()
        dao.activate(id)
    }

    override suspend fun updateTestResult(id: Long, success: Boolean) {
        dao.updateTestResult(id, currentTimeMillis(), success)
    }
}

private fun ExternalServerConfigEntity.toDomain() = ExternalServerConfig(
    id = id,
    name = name,
    baseUrl = baseUrl,
    apiKey = apiKey,
    isActive = isActive,
    lastTestedAt = lastTestedAt,
    lastTestSuccess = lastTestSuccess,
    createdAt = createdAt,
)

private fun ExternalServerConfig.toEntity() = ExternalServerConfigEntity(
    id = id,
    name = name,
    baseUrl = baseUrl,
    apiKey = apiKey,
    isActive = isActive,
    lastTestedAt = lastTestedAt,
    lastTestSuccess = lastTestSuccess,
    createdAt = if (createdAt == 0L) currentTimeMillis() else createdAt,
)
