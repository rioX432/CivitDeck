package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.CaptionEntity
import com.riox432.civitdeck.data.local.entity.ImageTagEntity

@Dao
interface DatasetImageMetaDao {

    @Query("SELECT * FROM image_tags WHERE datasetImageId = :datasetImageId")
    suspend fun getTagsForImage(datasetImageId: Long): List<ImageTagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(entities: List<ImageTagEntity>)

    @Query("DELETE FROM image_tags WHERE datasetImageId = :datasetImageId")
    suspend fun deleteTagsForImage(datasetImageId: Long)

    @Query("SELECT * FROM captions WHERE datasetImageId = :datasetImageId")
    suspend fun getCaption(datasetImageId: Long): CaptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCaption(entity: CaptionEntity)
}
