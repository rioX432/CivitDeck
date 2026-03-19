package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.ModelUpdateNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelUpdateNotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ModelUpdateNotificationEntity>)

    @Query("SELECT * FROM model_update_notifications ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ModelUpdateNotificationEntity>>

    @Query("SELECT COUNT(*) FROM model_update_notifications WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("UPDATE model_update_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long): Int

    @Query("UPDATE model_update_notifications SET isRead = 1")
    suspend fun markAllRead(): Int

    @Query("DELETE FROM model_update_notifications WHERE createdAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long): Int
}
