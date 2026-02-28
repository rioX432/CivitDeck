package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationProgress
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.model.QueueJob
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface ComfyUIRepository {
    // Connection management
    fun observeConnections(): Flow<List<ComfyUIConnection>>
    fun observeActiveConnection(): Flow<ComfyUIConnection?>
    suspend fun getActiveConnection(): ComfyUIConnection?
    suspend fun saveConnection(connection: ComfyUIConnection): Long
    suspend fun deleteConnection(id: Long)
    suspend fun activateConnection(id: Long)
    suspend fun testConnection(connection: ComfyUIConnection): Boolean
    suspend fun updateTestResult(id: Long, success: Boolean)

    // Generation
    suspend fun fetchCheckpoints(): List<String>
    suspend fun fetchLoras(): List<String>
    suspend fun fetchControlNets(): List<String>
    suspend fun submitGeneration(params: ComfyUIGenerationParams): String
    suspend fun pollGenerationResult(promptId: String): GenerationResult
    fun observeGenerationProgress(promptId: String, host: String, port: Int): Flow<GenerationProgress>
    fun getImageUrl(filename: String, subfolder: String = "", type: String = "output"): String

    // Queue management
    fun observeQueue(intervalMs: Long = QUEUE_POLL_INTERVAL_MS): Flow<List<QueueJob>>
    suspend fun cancelJob(promptId: String)

    companion object {
        const val QUEUE_POLL_INTERVAL_MS = 3000L
    }
}
