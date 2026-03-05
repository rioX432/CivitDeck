package com.riox432.civitdeck.feature.externalserver.domain.repository

import com.riox432.civitdeck.domain.model.ExternalServerConfig
import kotlinx.coroutines.flow.Flow

interface ExternalServerConfigRepository {
    fun observeConfigs(): Flow<List<ExternalServerConfig>>
    fun observeActiveConfig(): Flow<ExternalServerConfig?>
    suspend fun saveConfig(config: ExternalServerConfig): Long
    suspend fun deleteConfig(id: Long)
    suspend fun activateConfig(id: Long)
    suspend fun updateTestResult(id: Long, success: Boolean)
}
