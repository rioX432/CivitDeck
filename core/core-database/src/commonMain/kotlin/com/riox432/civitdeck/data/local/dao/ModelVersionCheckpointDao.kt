package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.ModelVersionCheckpointEntity

@Dao
interface ModelVersionCheckpointDao {
    @Query("SELECT * FROM model_version_checkpoints WHERE modelId = :modelId")
    suspend fun getCheckpoint(modelId: Long): ModelVersionCheckpointEntity?

    @Query("SELECT * FROM model_version_checkpoints")
    suspend fun getAllCheckpoints(): List<ModelVersionCheckpointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ModelVersionCheckpointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ModelVersionCheckpointEntity>)

    @Query("DELETE FROM model_version_checkpoints WHERE modelId = :modelId")
    suspend fun delete(modelId: Long): Int

    @Query("DELETE FROM model_version_checkpoints WHERE modelId NOT IN (:activeModelIds)")
    suspend fun deleteStaleCheckpoints(activeModelIds: List<Long>): Int
}
