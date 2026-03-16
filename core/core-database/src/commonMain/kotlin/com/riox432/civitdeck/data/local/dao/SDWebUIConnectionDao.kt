package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.riox432.civitdeck.data.local.entity.SDWebUIConnectionEntity
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
@Dao
interface SDWebUIConnectionDao {
    @Query("SELECT * FROM sdwebui_connections ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SDWebUIConnectionEntity>>

    @Query("SELECT * FROM sdwebui_connections WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<SDWebUIConnectionEntity?>

    @Query("SELECT * FROM sdwebui_connections WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): SDWebUIConnectionEntity?

    @Query("SELECT * FROM sdwebui_connections ORDER BY createdAt DESC")
    suspend fun getAll(): List<SDWebUIConnectionEntity>

    @Insert
    suspend fun insert(entity: SDWebUIConnectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SDWebUIConnectionEntity>)

    @Update
    suspend fun update(entity: SDWebUIConnectionEntity): Int

    @Query("UPDATE sdwebui_connections SET isActive = 0")
    suspend fun deactivateAll(): Int

    @Query("UPDATE sdwebui_connections SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: Long): Int

    @Query(
        "UPDATE sdwebui_connections SET lastTestedAt = :testedAt, lastTestSuccess = :success WHERE id = :id",
    )
    suspend fun updateTestResult(id: Long, testedAt: Long, success: Boolean): Int

    @Query("DELETE FROM sdwebui_connections WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM sdwebui_connections")
    suspend fun deleteAll(): Int
}
