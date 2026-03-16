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

    @Query("SELECT * FROM saved_search_filters ORDER BY savedAt DESC")
    suspend fun getAll(): List<SavedSearchFilterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SavedSearchFilterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SavedSearchFilterEntity>)

    @Query("DELETE FROM saved_search_filters")
    suspend fun deleteAll(): Int

    @Query("DELETE FROM saved_search_filters WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
