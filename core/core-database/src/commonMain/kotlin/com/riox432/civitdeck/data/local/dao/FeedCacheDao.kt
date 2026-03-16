package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.FeedCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<FeedCacheEntity>)

    @Query("SELECT * FROM feed_cache ORDER BY publishedAt DESC")
    suspend fun getAll(): List<FeedCacheEntity>

    @Query("DELETE FROM feed_cache WHERE creatorUsername = :username")
    suspend fun deleteByCreator(username: String): Int

    @Query("DELETE FROM feed_cache WHERE cachedAt < :threshold")
    suspend fun deleteExpired(threshold: Long): Int

    @Query("SELECT COUNT(*) FROM feed_cache WHERE cachedAt > :since")
    fun countNewSince(since: Long): Flow<Int>
}
