package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.ModelVersionCheckpointDao
import com.riox432.civitdeck.data.local.entity.ModelVersionCheckpointEntity
import com.riox432.civitdeck.domain.repository.ModelVersionCheckpointRepository

class ModelVersionCheckpointRepositoryImpl(
    private val dao: ModelVersionCheckpointDao,
) : ModelVersionCheckpointRepository {

    override suspend fun getCheckpoint(modelId: Long): Long? =
        dao.getCheckpoint(modelId)?.lastKnownVersionId

    override suspend fun getAllCheckpoints(): Map<Long, Pair<Long, Long>> =
        dao.getAllCheckpoints().associate {
            it.modelId to (it.lastKnownVersionId to it.lastCheckedAt)
        }

    override suspend fun saveCheckpoint(modelId: Long, versionId: Long) {
        dao.upsert(
            ModelVersionCheckpointEntity(
                modelId = modelId,
                lastKnownVersionId = versionId,
                lastCheckedAt = currentTimeMillis(),
            ),
        )
    }

    override suspend fun saveCheckpoints(checkpoints: Map<Long, Long>) {
        val now = currentTimeMillis()
        dao.upsertAll(
            checkpoints.map { (modelId, versionId) ->
                ModelVersionCheckpointEntity(
                    modelId = modelId,
                    lastKnownVersionId = versionId,
                    lastCheckedAt = now,
                )
            },
        )
    }

    override suspend fun deleteStaleCheckpoints(activeModelIds: Set<Long>) {
        // Compute stale IDs in Kotlin to avoid SQLite bind parameter limit (999)
        val allCheckpoints = dao.getAllCheckpoints()
        val staleIds = allCheckpoints.map { it.modelId }.filter { it !in activeModelIds }
        for (id in staleIds) {
            dao.delete(id)
        }
    }
}
