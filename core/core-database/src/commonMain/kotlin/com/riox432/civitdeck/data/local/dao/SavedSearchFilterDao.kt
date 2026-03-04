package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.SavedSearchFilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSearchFilterDao {
    @Query("SELECT * FROM saved_search_filters ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<SavedSearchFilterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SavedSearchFilterEntity): Long

    @Query("DELETE FROM saved_search_filters WHERE id = :id")
    suspend fun deleteById(id: Long)
}
