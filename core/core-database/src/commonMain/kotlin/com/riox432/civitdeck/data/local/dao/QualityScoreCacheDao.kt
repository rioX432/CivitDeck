package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.riox432.civitdeck.data.local.entity.QualityScoreCacheEntity

@Dao
interface QualityScoreCacheDao {
    @Query("SELECT * FROM quality_score_cache WHERE modelId = :modelId")
    suspend fun getByModelId(modelId: Long): QualityScoreCacheEntity?

    @Query("SELECT * FROM quality_score_cache WHERE modelId IN (:modelIds)")
    suspend fun getByModelIds(modelIds: List<Long>): List<QualityScoreCacheEntity>

    @Upsert
    suspend fun upsert(entity: QualityScoreCacheEntity)

    @Upsert
    suspend fun upsertAll(entities: List<QualityScoreCacheEntity>)

    @Query("DELETE FROM quality_score_cache WHERE cachedAt < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM quality_score_cache")
    suspend fun deleteAll()
}
