package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.ModelEmbeddingEntity

@Dao
interface ModelEmbeddingDao {

    @Query("SELECT * FROM model_embeddings WHERE modelId = :modelId")
    suspend fun get(modelId: Long): ModelEmbeddingEntity?

    @Query("SELECT * FROM model_embeddings WHERE embeddingModel = :embeddingModel")
    suspend fun getAllForModel(embeddingModel: String): List<ModelEmbeddingEntity>

    @Query("SELECT COUNT(*) FROM model_embeddings WHERE embeddingModel = :embeddingModel")
    suspend fun countFor(embeddingModel: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ModelEmbeddingEntity)

    @Query("DELETE FROM model_embeddings WHERE modelId = :modelId")
    suspend fun delete(modelId: Long): Int

    @Query("DELETE FROM model_embeddings WHERE embeddingModel != :keepModel")
    suspend fun deleteStale(keepModel: String): Int

    @Query("DELETE FROM model_embeddings")
    suspend fun deleteAll(): Int
}
