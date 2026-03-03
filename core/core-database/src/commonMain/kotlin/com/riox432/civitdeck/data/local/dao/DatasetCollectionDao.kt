package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.CaptionEntity
import com.riox432.civitdeck.data.local.entity.DatasetCollectionEntity
import com.riox432.civitdeck.data.local.entity.DatasetImageEntity
import com.riox432.civitdeck.data.local.entity.ImageTagEntity
import kotlinx.coroutines.flow.Flow

data class DatasetCollectionWithCount(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long,
    val imageCount: Int,
)

@Dao
interface DatasetCollectionDao {

    // --- Dataset Collection CRUD ---

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

    // --- Dataset Images ---

    @Query("SELECT * FROM dataset_images WHERE datasetId = :datasetId ORDER BY addedAt DESC")
    fun observeImages(datasetId: Long): Flow<List<DatasetImageEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImage(entity: DatasetImageEntity): Long

    @Query("DELETE FROM dataset_images WHERE id = :imageId")
    suspend fun deleteImage(imageId: Long)

    @Query("DELETE FROM dataset_images WHERE id IN (:imageIds)")
    suspend fun deleteImages(imageIds: List<Long>)

    // --- Tags ---

    @Query("SELECT * FROM image_tags WHERE datasetImageId = :datasetImageId")
    suspend fun getTagsForImage(datasetImageId: Long): List<ImageTagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(entity: ImageTagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(entities: List<ImageTagEntity>)

    @Query("DELETE FROM image_tags WHERE datasetImageId = :datasetImageId")
    suspend fun deleteTagsForImage(datasetImageId: Long)

    // --- Captions ---

    @Query("SELECT * FROM captions WHERE datasetImageId = :datasetImageId")
    suspend fun getCaption(datasetImageId: Long): CaptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCaption(entity: CaptionEntity)
}
