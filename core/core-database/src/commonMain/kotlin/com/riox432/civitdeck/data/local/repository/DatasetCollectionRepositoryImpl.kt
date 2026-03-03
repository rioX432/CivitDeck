package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.DatasetCollectionDao
import com.riox432.civitdeck.data.local.entity.DatasetCollectionEntity
import com.riox432.civitdeck.data.local.entity.DatasetImageEntity
import com.riox432.civitdeck.data.local.entity.ImageTagEntity
import com.riox432.civitdeck.domain.model.Caption
import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.ImageSource
import com.riox432.civitdeck.domain.model.ImageTag
import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DatasetCollectionRepositoryImpl(
    private val dao: DatasetCollectionDao,
) : DatasetCollectionRepository {

    override fun observeCollections(): Flow<List<DatasetCollection>> =
        dao.observeAllWithCount().map { list ->
            list.map { row ->
                DatasetCollection(
                    id = row.id,
                    name = row.name,
                    description = row.description,
                    imageCount = row.imageCount,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt,
                )
            }
        }

    override suspend fun createCollection(name: String, description: String): Long {
        val now = currentTimeMillis()
        return dao.insertCollection(
            DatasetCollectionEntity(name = name, description = description, createdAt = now, updatedAt = now),
        )
    }

    override suspend fun renameCollection(id: Long, name: String) {
        dao.renameCollection(id, name, currentTimeMillis())
    }

    override suspend fun deleteCollection(id: Long) {
        dao.deleteCollection(id)
    }

    override fun observeImages(datasetId: Long): Flow<List<DatasetImage>> =
        dao.observeImages(datasetId).map { entities ->
            entities.map { entity ->
                val tags = dao.getTagsForImage(entity.id).map { ImageTag(it.id, it.datasetImageId, it.tag) }
                val caption = dao.getCaption(entity.id)?.let { Caption(it.datasetImageId, it.text) }
                DatasetImage(
                    id = entity.id,
                    datasetId = entity.datasetId,
                    imageUrl = entity.imageUrl,
                    sourceType = ImageSource.valueOf(entity.sourceType),
                    trainable = entity.trainable,
                    addedAt = entity.addedAt,
                    tags = tags,
                    caption = caption,
                )
            }
        }

    override suspend fun addImage(
        datasetId: Long,
        imageUrl: String,
        sourceType: ImageSource,
        trainable: Boolean,
        tags: List<String>,
    ): Long {
        val imageId = dao.insertImage(
            DatasetImageEntity(
                datasetId = datasetId,
                imageUrl = imageUrl,
                sourceType = sourceType.name,
                trainable = trainable,
                addedAt = currentTimeMillis(),
            ),
        )
        if (imageId != -1L && tags.isNotEmpty()) {
            dao.insertTags(tags.map { ImageTagEntity(datasetImageId = imageId, tag = it) })
        }
        return imageId
    }

    override suspend fun removeImage(imageId: Long) {
        dao.deleteImage(imageId)
    }

    override suspend fun removeImages(imageIds: List<Long>) {
        dao.deleteImages(imageIds)
    }
}
