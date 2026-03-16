package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
@Dao
interface ComfyUIConnectionDao {
    @Query("SELECT * FROM comfyui_connections ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ComfyUIConnectionEntity>>

    @Query("SELECT * FROM comfyui_connections WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<ComfyUIConnectionEntity?>

    @Query("SELECT * FROM comfyui_connections WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): ComfyUIConnectionEntity?

    @Query("SELECT * FROM comfyui_connections WHERE id = :id")
    suspend fun getById(id: Long): ComfyUIConnectionEntity?

    @Query("SELECT * FROM comfyui_connections ORDER BY createdAt DESC")
    suspend fun getAll(): List<ComfyUIConnectionEntity>

    @Insert
    suspend fun insert(entity: ComfyUIConnectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ComfyUIConnectionEntity>)

    @Update
    suspend fun update(entity: ComfyUIConnectionEntity): Int

    @Query("UPDATE comfyui_connections SET isActive = 0")
    suspend fun deactivateAll(): Int

    @Query("UPDATE comfyui_connections SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: Long): Int

    @Query(
        "UPDATE comfyui_connections SET lastTestedAt = :testedAt, lastTestSuccess = :success WHERE id = :id",
    )
    suspend fun updateTestResult(id: Long, testedAt: Long, success: Boolean): Int

    @Query("DELETE FROM comfyui_connections WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM comfyui_connections")
    suspend fun deleteAll(): Int
}
