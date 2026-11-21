package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.riox432.civitdeck.data.local.entity.SavedPromptEntity
import kotlinx.coroutines.flow.Flow

/**
 * Reference-specific access to `saved_prompts`, kept apart from [SavedPromptDao] so its
 * existing signatures and fakes stay unchanged. A saved row's source snapshot is captured once
 * and never replaced; deleting a row still goes through [SavedPromptDao.deleteById].
 */
@Dao
interface SavedReferenceDao {
    @Insert
    suspend fun insert(entity: SavedPromptEntity): Long

    @Query("SELECT * FROM saved_prompts WHERE id = :id")
    suspend fun getById(id: Long): SavedPromptEntity?

    @Query("SELECT * FROM saved_prompts WHERE id = :id")
    fun observeById(id: Long): Flow<SavedPromptEntity?>

    /** Returns 1 when the snapshot was stored, 0 when the row is missing or already has one. */
    @Query(
        "UPDATE saved_prompts SET sourceReferenceJson = :json " +
            "WHERE id = :id AND sourceReferenceJson IS NULL",
    )
    suspend fun captureSourceIfAbsent(id: Long, json: String): Int

    /**
     * Saves the editable fields of [entity]. An existing non-null snapshot is kept verbatim,
     * whatever [entity] carries, so a caller that never read the snapshot cannot clear it.
     */
    @Transaction
    suspend fun upsertPreservingSource(entity: SavedPromptEntity) {
        val storedSource = if (entity.id == 0L) null else getById(entity.id)?.sourceReferenceJson
        upsertReplacingSource(entity.copy(sourceReferenceJson = storedSource ?: entity.sourceReferenceJson))
    }

    /** Overwrites the snapshot too; only [upsertPreservingSource] should call it. */
    @Upsert
    suspend fun upsertReplacingSource(entity: SavedPromptEntity)
}
