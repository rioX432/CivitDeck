package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.ComfyUIConnectionDao
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "ComfyUIConnectionRepo"

class ComfyUIConnectionRepositoryImpl(
    private val dao: ComfyUIConnectionDao,
    private val api: ComfyUIApi,
) : ComfyUIConnectionRepository {

    override fun observeConnections(): Flow<List<ComfyUIConnection>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeActiveConnection(): Flow<ComfyUIConnection?> =
        dao.observeActive().map { it?.toDomain() }

    override suspend fun getActiveConnection(): ComfyUIConnection? =
        dao.getActive()?.toDomain()

    override suspend fun saveConnection(connection: ComfyUIConnection): Long {
        val entity = connection.toEntity()
        return if (connection.id == 0L) {
            val id = dao.insert(entity)
            // If this is the first connection, activate it
            if (dao.getActive() == null) {
                dao.activate(id)
            }
            id
        } else {
            val stored = dao.getById(connection.id)
            dao.update(entity.copy(tlsCertSha256 = pinForUpdate(stored, connection)))
            connection.id
        }
    }

    /**
     * A pin identifies one endpoint: editing hostname, port or scheme drops it, even when the
     * caller copied the old pin along. Callers that do not know the pin (settings edits pass
     * null) keep the stored one; a new pin for the same endpoint replaces it.
     */
    private fun pinForUpdate(stored: ComfyUIConnectionEntity?, incoming: ComfyUIConnection): String? {
        if (stored == null) return incoming.tlsCertSha256
        val sameEndpoint = stored.hostname == incoming.hostname &&
            stored.port == incoming.port &&
            stored.useHttps == incoming.useHttps
        return if (sameEndpoint) incoming.tlsCertSha256 ?: stored.tlsCertSha256 else null
    }

    override suspend fun deleteConnection(id: Long) { dao.deleteById(id) }

    override suspend fun activateConnection(id: Long) {
        dao.deactivateAll()
        dao.activate(id)
    }

    override suspend fun testConnection(connection: ComfyUIConnection): Boolean {
        api.setBaseUrl(connection.baseUrl)
        return try {
            val probe = api.probeQueue()
            if (!probe.isComfyUIQueue) {
                Logger.w(TAG, "Connection test failed: HTTP ${probe.status}, queue_running=${probe.hasQueueRunning}")
            }
            probe.isComfyUIQueue
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(TAG, "Connection test failed: ${e.message}")
            false
        }
    }

    override suspend fun updateTestResult(id: Long, success: Boolean) {
        dao.updateTestResult(id, currentTimeMillis(), success)
    }

    private fun ComfyUIConnectionEntity.toDomain() = ComfyUIConnection(
        id = id,
        name = name,
        hostname = hostname,
        port = port,
        isActive = isActive,
        lastTestedAt = lastTestedAt,
        lastTestSuccess = lastTestSuccess,
        useHttps = useHttps,
        acceptSelfSigned = acceptSelfSigned,
        ntfyServerUrl = ntfyServerUrl,
        ntfyTopic = ntfyTopic,
        tlsCertSha256 = tlsCertSha256,
    )

    private fun ComfyUIConnection.toEntity() = ComfyUIConnectionEntity(
        id = id,
        name = name,
        hostname = hostname,
        port = port,
        isActive = isActive,
        lastTestedAt = lastTestedAt,
        lastTestSuccess = lastTestSuccess,
        createdAt = currentTimeMillis(),
        useHttps = useHttps,
        acceptSelfSigned = acceptSelfSigned,
        ntfyServerUrl = ntfyServerUrl,
        ntfyTopic = ntfyTopic,
        tlsCertSha256 = tlsCertSha256,
    )
}
