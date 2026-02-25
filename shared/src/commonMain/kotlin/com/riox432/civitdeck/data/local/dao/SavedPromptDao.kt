package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.SavedPromptEntity
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT COUNT(*) FROM saved_prompts WHERE prompt = :prompt AND modelName = :modelName")
    suspend fun countByPromptAndModel(prompt: String, modelName: String?): Int

    @Query("UPDATE saved_prompts SET isTemplate = :isTemplate, templateName = :templateName WHERE id = :id")
    suspend fun updateTemplate(id: Long, isTemplate: Boolean, templateName: String?)

    @Query("UPDATE saved_prompts SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String?)

    @Insert
    suspend fun insert(entity: SavedPromptEntity)

    @Query("DELETE FROM saved_prompts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
