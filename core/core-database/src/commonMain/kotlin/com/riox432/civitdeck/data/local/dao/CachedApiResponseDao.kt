package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.CachedApiResponseEntity

@Dao
interface CachedApiResponseDao {
    @Query("SELECT * FROM cached_api_responses WHERE cacheKey = :key")
    suspend fun getByKey(key: String): CachedApiResponseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CachedApiResponseEntity)

    @Query("DELETE FROM cached_api_responses WHERE cacheKey = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM cached_api_responses WHERE cachedAt < :expiryTime AND isOfflinePinned = 0")
    suspend fun deleteExpired(expiryTime: Long)

    @Query("DELETE FROM cached_api_responses")
    suspend fun deleteAll()

    @Query("UPDATE cached_api_responses SET isOfflinePinned = :pinned WHERE cacheKey = :key")
    suspend fun setPinned(key: String, pinned: Boolean)

    @Query("SELECT SUM(LENGTH(responseJson)) FROM cached_api_responses")
    suspend fun getTotalCacheSizeBytes(): Long?

    @Query("SELECT COUNT(*) FROM cached_api_responses")
    suspend fun getEntryCount(): Int

    @Query(
        "DELETE FROM cached_api_responses WHERE cacheKey IN " +
            "(SELECT cacheKey FROM cached_api_responses WHERE isOfflinePinned = 0 " +
            "ORDER BY cachedAt ASC LIMIT :count)",
    )
    suspend fun deleteOldestUnpinned(count: Int)
}
