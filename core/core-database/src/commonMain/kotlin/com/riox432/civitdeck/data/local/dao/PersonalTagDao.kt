package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.PersonalTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalTagDao {
    @Query("SELECT * FROM personal_tags WHERE modelId = :modelId ORDER BY addedAt DESC")
    fun observeByModelId(modelId: Long): Flow<List<PersonalTagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PersonalTagEntity)

    @Query("DELETE FROM personal_tags WHERE modelId = :modelId AND tag = :tag")
    suspend fun delete(modelId: Long, tag: String)

    @Query("SELECT DISTINCT tag FROM personal_tags ORDER BY tag ASC")
    suspend fun getAllDistinctTags(): List<String>

    @Query("SELECT DISTINCT modelId FROM personal_tags WHERE tag = :tag")
    suspend fun getModelIdsByTag(tag: String): List<Long>
}
