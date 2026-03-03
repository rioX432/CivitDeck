package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.DatasetCollectionEntity
import com.riox432.civitdeck.data.local.entity.DatasetImageEntity
import kotlinx.coroutines.flow.Flow

data class DatasetCollectionWithCount(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long,
    val imageCount: Int,
)

@Suppress("TooManyFunctions")
@Dao
interface DatasetCollectionDao {

    @Query(
        """
        SELECT d.id, d.name, d.description, d.createdAt, d.updatedAt,
               COUNT(i.id) AS imageCount
        FROM dataset_collections d
        LEFT JOIN dataset_images i ON i.datasetId = d.id
        GROUP BY d.id
        ORDER BY d.updatedAt DESC
        """,
    )
    fun observeAllWithCount(): Flow<List<DatasetCollectionWithCount>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCollection(entity: DatasetCollectionEntity): Long

    @Query("UPDATE dataset_collections SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameCollection(id: Long, name: String, updatedAt: Long)

    @Query("DELETE FROM dataset_collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Query("SELECT * FROM dataset_images WHERE datasetId = :datasetId ORDER BY addedAt DESC")
    fun observeImages(datasetId: Long): Flow<List<DatasetImageEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImage(entity: DatasetImageEntity): Long

    @Query("DELETE FROM dataset_images WHERE id = :imageId")
    suspend fun deleteImage(imageId: Long)

    @Query("DELETE FROM dataset_images WHERE id IN (:imageIds)")
    suspend fun deleteImages(imageIds: List<Long>)

    @Query("UPDATE dataset_images SET trainable = :trainable WHERE id = :imageId")
    suspend fun updateTrainable(imageId: Long, trainable: Boolean)

    @Query("UPDATE dataset_images SET licenseNote = :licenseNote WHERE id = :imageId")
    suspend fun updateLicenseNote(imageId: Long, licenseNote: String?)

    @Query("SELECT * FROM dataset_images WHERE datasetId = :datasetId AND trainable = 0 ORDER BY addedAt DESC")
    suspend fun getNonTrainableImages(datasetId: Long): List<DatasetImageEntity>

    @Query("UPDATE dataset_images SET pHash = :pHash WHERE id = :imageId")
    suspend fun updatePHash(imageId: Long, pHash: String?)

    @Query("UPDATE dataset_images SET excluded = :excluded WHERE id = :imageId")
    suspend fun updateExcluded(imageId: Long, excluded: Boolean)

    @Query("UPDATE dataset_images SET width = :width, height = :height WHERE id = :imageId")
    suspend fun updateDimensions(imageId: Long, width: Int, height: Int)
}
