package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.ModelDownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDownloadDao {
    @Insert
    suspend fun insert(entity: ModelDownloadEntity): Long

    @Query("SELECT * FROM model_downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ModelDownloadEntity>>

    @Query("SELECT * FROM model_downloads WHERE modelId = :modelId ORDER BY createdAt DESC")
    fun observeByModelId(modelId: Long): Flow<List<ModelDownloadEntity>>

    @Query("SELECT * FROM model_downloads WHERE id = :id")
    suspend fun getById(id: Long): ModelDownloadEntity?

    @Query("SELECT * FROM model_downloads WHERE fileId = :fileId LIMIT 1")
    suspend fun getByFileId(fileId: Long): ModelDownloadEntity?

    @Query("UPDATE model_downloads SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long): Int

    @Query(
        "UPDATE model_downloads SET downloadedBytes = :bytes, updatedAt = :updatedAt WHERE id = :id",
    )
    suspend fun updateProgress(id: Long, bytes: Long, updatedAt: Long): Int

    @Query(
        "UPDATE model_downloads SET destinationPath = :path, updatedAt = :updatedAt WHERE id = :id",
    )
    suspend fun updateDestinationPath(id: Long, path: String, updatedAt: Long): Int

    @Query("DELETE FROM model_downloads WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query(
        "UPDATE model_downloads SET hashVerified = :verified, updatedAt = :updatedAt WHERE id = :id",
    )
    suspend fun updateHashVerified(id: Long, verified: Int, updatedAt: Long): Int

    @Query("DELETE FROM model_downloads WHERE status = 'Completed'")
    suspend fun deleteCompleted(): Int
}
