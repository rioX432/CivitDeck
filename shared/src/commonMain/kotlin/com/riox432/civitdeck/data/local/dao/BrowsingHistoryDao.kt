package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.BrowsingHistoryEntity

@Dao
interface BrowsingHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BrowsingHistoryEntity)

    @Query("SELECT * FROM browsing_history ORDER BY viewedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<BrowsingHistoryEntity>

    @Query("SELECT DISTINCT modelId FROM browsing_history ORDER BY viewedAt DESC LIMIT :limit")
    suspend fun getRecentModelIds(limit: Int = 50): List<Long>

    @Query("SELECT DISTINCT modelId FROM browsing_history")
    suspend fun getAllModelIds(): List<Long>

    @Query("SELECT COUNT(*) FROM browsing_history")
    suspend fun count(): Int
}
