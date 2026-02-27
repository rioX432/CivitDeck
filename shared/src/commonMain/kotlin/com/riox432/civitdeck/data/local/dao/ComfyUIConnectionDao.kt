package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import kotlinx.coroutines.flow.Flow

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

    @Insert
    suspend fun insert(entity: ComfyUIConnectionEntity): Long

    @Update
    suspend fun update(entity: ComfyUIConnectionEntity)

    @Query("UPDATE comfyui_connections SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE comfyui_connections SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: Long)

    @Query(
        "UPDATE comfyui_connections SET lastTestedAt = :testedAt, lastTestSuccess = :success WHERE id = :id",
    )
    suspend fun updateTestResult(id: Long, testedAt: Long, success: Boolean)

    @Query("DELETE FROM comfyui_connections WHERE id = :id")
    suspend fun deleteById(id: Long)
}
