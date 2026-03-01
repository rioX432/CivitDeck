package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.SDWebUIConnection
import kotlinx.coroutines.flow.Flow

interface SDWebUIConnectionRepository {
    fun observeConnections(): Flow<List<SDWebUIConnection>>
    fun observeActiveConnection(): Flow<SDWebUIConnection?>
    suspend fun getActiveConnection(): SDWebUIConnection?
    suspend fun saveConnection(connection: SDWebUIConnection): Long
    suspend fun deleteConnection(id: Long)
    suspend fun activateConnection(id: Long)
    suspend fun testConnection(connection: SDWebUIConnection): Boolean
    suspend fun updateTestResult(id: Long, success: Boolean)
}
