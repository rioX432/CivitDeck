package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ComfyUIConnection
import kotlinx.coroutines.flow.Flow

interface ComfyUIConnectionRepository {
    fun observeConnections(): Flow<List<ComfyUIConnection>>
    fun observeActiveConnection(): Flow<ComfyUIConnection?>
    suspend fun getActiveConnection(): ComfyUIConnection?
    suspend fun saveConnection(connection: ComfyUIConnection): Long
    suspend fun deleteConnection(id: Long)
    suspend fun activateConnection(id: Long)
    suspend fun testConnection(connection: ComfyUIConnection): Boolean
    suspend fun updateTestResult(id: Long, success: Boolean)
}
