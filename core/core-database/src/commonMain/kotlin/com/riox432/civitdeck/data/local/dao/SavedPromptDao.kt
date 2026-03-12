package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.riox432.civitdeck.data.local.entity.SavedPromptEntity
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
@Dao
interface SavedPromptDao {
    @Query("SELECT * FROM saved_prompts ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<SavedPromptEntity>>

    @Query("SELECT * FROM saved_prompts WHERE isTemplate = 1 ORDER BY savedAt DESC")
    fun observeTemplates(): Flow<List<SavedPromptEntity>>

    @Query("SELECT * FROM saved_prompts WHERE autoSaved = 1 ORDER BY savedAt DESC")
    fun observeHistory(): Flow<List<SavedPromptEntity>>

    @Query(
        "SELECT * FROM saved_prompts WHERE " +
            "prompt LIKE '%' || :query || '%' OR " +
            "negativePrompt LIKE '%' || :query || '%' OR " +
            "modelName LIKE '%' || :query || '%' OR " +
            "templateName LIKE '%' || :query || '%' " +
            "ORDER BY savedAt DESC",
    )
    fun search(query: String): Flow<List<SavedPromptEntity>>

    @Query(
        "SELECT COUNT(*) FROM saved_prompts WHERE prompt = :prompt " +
            "AND (modelName = :modelName OR (modelName IS NULL AND :modelName IS NULL))",
    )
    suspend fun countByPromptAndModel(prompt: String, modelName: String?): Int

    @Query("UPDATE saved_prompts SET isTemplate = :isTemplate, templateName = :templateName WHERE id = :id")
    suspend fun updateTemplate(id: Long, isTemplate: Boolean, templateName: String?)

    @Query("SELECT * FROM saved_prompts WHERE id > 0 ORDER BY savedAt DESC")
    suspend fun getAllUserCreated(): List<SavedPromptEntity>

    @Insert
    suspend fun insert(entity: SavedPromptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SavedPromptEntity>)

    @Upsert
    suspend fun upsert(entity: SavedPromptEntity)

    @Query("DELETE FROM saved_prompts WHERE id > 0")
    suspend fun deleteAllUserCreated()

    @Query("DELETE FROM saved_prompts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
