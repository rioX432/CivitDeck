package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.ExcludedTagEntity

@Dao
interface ExcludedTagDao {
    @Query("SELECT * FROM excluded_tags ORDER BY addedAt DESC")
    suspend fun getAll(): List<ExcludedTagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ExcludedTagEntity)

    @Query("DELETE FROM excluded_tags WHERE tag = :tag")
    suspend fun delete(tag: String)
}
