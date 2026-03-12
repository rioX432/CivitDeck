package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.riox432.civitdeck.data.local.entity.ExternalServerConfigEntity
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
@Dao
interface ExternalServerConfigDao {
    @Query("SELECT * FROM external_server_configs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ExternalServerConfigEntity>>

    @Query("SELECT * FROM external_server_configs WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<ExternalServerConfigEntity?>

    @Query("SELECT * FROM external_server_configs WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): ExternalServerConfigEntity?

    @Query("SELECT * FROM external_server_configs WHERE id = :id")
    suspend fun getById(id: Long): ExternalServerConfigEntity?

    @Query("SELECT * FROM external_server_configs ORDER BY createdAt DESC")
    suspend fun getAll(): List<ExternalServerConfigEntity>

    @Insert
    suspend fun insert(entity: ExternalServerConfigEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ExternalServerConfigEntity>)

    @Update
    suspend fun update(entity: ExternalServerConfigEntity)

    @Query("UPDATE external_server_configs SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE external_server_configs SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: Long)

    @Query(
        "UPDATE external_server_configs SET lastTestedAt = :testedAt, lastTestSuccess = :success WHERE id = :id",
    )
    suspend fun updateTestResult(id: Long, testedAt: Long, success: Boolean)

    @Query("DELETE FROM external_server_configs WHERE id = :id")
    suspend fun deleteById(id: Long)
}
