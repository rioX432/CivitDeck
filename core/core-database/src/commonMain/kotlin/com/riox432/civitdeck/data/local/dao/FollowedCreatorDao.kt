package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.FollowedCreatorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowedCreatorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FollowedCreatorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<FollowedCreatorEntity>)

    @Query("DELETE FROM followed_creators")
    suspend fun deleteAll(): Int

    @Query("DELETE FROM followed_creators WHERE username = :username")
    suspend fun delete(username: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM followed_creators WHERE username = :username)")
    fun isFollowing(username: String): Flow<Boolean>

    @Query("SELECT * FROM followed_creators ORDER BY followedAt DESC")
    fun observeAll(): Flow<List<FollowedCreatorEntity>>

    @Query("SELECT * FROM followed_creators ORDER BY followedAt DESC")
    suspend fun getAll(): List<FollowedCreatorEntity>

    @Query("UPDATE followed_creators SET lastCheckedAt = :timestamp WHERE username = :username")
    suspend fun updateLastCheckedAt(username: String, timestamp: Long): Int
}
