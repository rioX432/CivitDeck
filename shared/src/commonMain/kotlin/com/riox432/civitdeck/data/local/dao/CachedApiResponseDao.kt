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

    @Query("DELETE FROM cached_api_responses WHERE cachedAt < :expiryTime")
    suspend fun deleteExpired(expiryTime: Long)

    @Query("DELETE FROM cached_api_responses")
    suspend fun deleteAll()
}
