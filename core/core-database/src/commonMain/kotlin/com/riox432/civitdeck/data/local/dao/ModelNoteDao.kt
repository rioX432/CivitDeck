package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.ModelNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelNoteDao {
    @Query("SELECT * FROM model_notes WHERE modelId = :modelId LIMIT 1")
    fun observeByModelId(modelId: Long): Flow<ModelNoteEntity?>

    @Query("SELECT * FROM model_notes WHERE modelId = :modelId LIMIT 1")
    suspend fun getByModelId(modelId: Long): ModelNoteEntity?

    @Query("SELECT * FROM model_notes ORDER BY modelId ASC")
    suspend fun getAll(): List<ModelNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ModelNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ModelNoteEntity>)

    @Query("DELETE FROM model_notes")
    suspend fun deleteAll(): Int

    @Query("DELETE FROM model_notes WHERE modelId = :modelId")
    suspend fun deleteByModelId(modelId: Long): Int
}
