package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.SDWebUIConnection
import com.riox432.civitdeck.domain.model.SDWebUIGenerationParams
import com.riox432.civitdeck.domain.model.SDWebUIGenerationProgress
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface SDWebUIRepository {
    fun observeConnections(): Flow<List<SDWebUIConnection>>
    fun observeActiveConnection(): Flow<SDWebUIConnection?>
    suspend fun getActiveConnection(): SDWebUIConnection?
    suspend fun saveConnection(connection: SDWebUIConnection): Long
    suspend fun deleteConnection(id: Long)
    suspend fun activateConnection(id: Long)
    suspend fun testConnection(connection: SDWebUIConnection): Boolean
    suspend fun updateTestResult(id: Long, success: Boolean)
    suspend fun fetchModels(): List<String>
    suspend fun fetchSamplers(): List<String>
    suspend fun fetchVaes(): List<String>
    fun generateImage(params: SDWebUIGenerationParams): Flow<SDWebUIGenerationProgress>
    suspend fun interruptGeneration()
}
