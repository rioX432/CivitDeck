package com.riox432.civitdeck.domain.repository

interface ModelVersionCheckpointRepository {
    suspend fun getCheckpoint(modelId: Long): Long?
    suspend fun getAllCheckpoints(): Map<Long, Long>
    suspend fun saveCheckpoint(modelId: Long, versionId: Long)
    suspend fun saveCheckpoints(checkpoints: Map<Long, Long>)
    suspend fun deleteStaleCheckpoints(activeModelIds: Set<Long>)
}
