package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.LocalModelFileEntity
import com.riox432.civitdeck.data.local.entity.ModelDirectoryEntity
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
@Dao
interface LocalModelFileDao {
    // --- Directory CRUD ---

    @Query("SELECT * FROM model_directories ORDER BY id ASC")
    fun observeDirectories(): Flow<List<ModelDirectoryEntity>>

    @Query("SELECT * FROM model_directories WHERE isEnabled = 1")
    suspend fun getEnabledDirectories(): List<ModelDirectoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirectory(directory: ModelDirectoryEntity): Long

    @Query("DELETE FROM model_directories WHERE id = :id")
    suspend fun deleteDirectory(id: Long): Int

    @Query("UPDATE model_directories SET lastScannedAt = :scannedAt WHERE id = :id")
    suspend fun updateLastScannedAt(id: Long, scannedAt: Long): Int

    // --- Local Model File CRUD ---

    @Query("SELECT * FROM local_model_files ORDER BY fileName ASC")
    fun observeAllFiles(): Flow<List<LocalModelFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: LocalModelFileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<LocalModelFileEntity>)

    @Query("DELETE FROM local_model_files WHERE directoryId = :directoryId")
    suspend fun deleteFilesByDirectory(directoryId: Long): Int

    @Query("SELECT sha256Hash FROM local_model_files WHERE matchedModelId IS NOT NULL")
    fun observeOwnedHashes(): Flow<List<String>>

    @Query("SELECT sha256Hash FROM local_model_files WHERE matchedModelId IS NOT NULL")
    suspend fun getOwnedHashes(): List<String>

    @Suppress("LongParameterList")
    @Query(
        "UPDATE local_model_files SET matchedModelId = :modelId, matchedModelName = :modelName, " +
            "matchedVersionId = :versionId, matchedVersionName = :versionName, " +
            "latestVersionId = :latestVersionId, hasUpdate = :hasUpdate WHERE id = :fileId",
    )
    suspend fun updateMatchInfo(
        fileId: Long,
        modelId: Long,
        modelName: String,
        versionId: Long,
        versionName: String,
        latestVersionId: Long?,
        hasUpdate: Boolean,
    ): Int

    @Query("SELECT COUNT(*) FROM local_model_files")
    fun observeFileCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM local_model_files WHERE matchedModelId IS NOT NULL")
    fun observeMatchedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM local_model_files WHERE hasUpdate = 1")
    fun observeUpdatesAvailableCount(): Flow<Int>
}
