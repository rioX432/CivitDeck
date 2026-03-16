package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.ShareHashtagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShareHashtagDao {
    @Query("SELECT * FROM share_hashtags ORDER BY addedAt ASC")
    fun observeAll(): Flow<List<ShareHashtagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ShareHashtagEntity)

    @Query("UPDATE share_hashtags SET isEnabled = :isEnabled WHERE tag = :tag")
    suspend fun setEnabled(tag: String, isEnabled: Boolean)

    @Query("DELETE FROM share_hashtags WHERE tag = :tag AND isCustom = 1")
    suspend fun deleteCustom(tag: String): Int
}
