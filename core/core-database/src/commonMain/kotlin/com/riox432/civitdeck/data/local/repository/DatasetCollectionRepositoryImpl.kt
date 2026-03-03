package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.DatasetCollectionDao
import com.riox432.civitdeck.data.local.dao.DatasetImageMetaDao
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
    private val metaDao: DatasetImageMetaDao,
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
                val tags = metaDao.getTagsForImage(entity.id).map { ImageTag(it.id, it.datasetImageId, it.tag) }
                val caption = metaDao.getCaption(entity.id)?.let { Caption(it.datasetImageId, it.text) }
                DatasetImage(
                    id = entity.id,
                    datasetId = entity.datasetId,
                    imageUrl = entity.imageUrl,
                    sourceType = ImageSource.valueOf(entity.sourceType),
                    trainable = entity.trainable,
                    addedAt = entity.addedAt,
                    tags = tags,
                    caption = caption,
                    licenseNote = entity.licenseNote,
                    pHash = entity.pHash,
                    excluded = entity.excluded,
                    width = entity.width,
                    height = entity.height,
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
            metaDao.insertTags(tags.map { ImageTagEntity(datasetImageId = imageId, tag = it) })
        }
        return imageId
    }

    override suspend fun removeImage(imageId: Long) {
        dao.deleteImage(imageId)
    }

    override suspend fun removeImages(imageIds: List<Long>) {
        dao.deleteImages(imageIds)
    }

    override suspend fun updateTrainable(imageId: Long, trainable: Boolean) {
        dao.updateTrainable(imageId, trainable)
    }

    override suspend fun updateLicenseNote(imageId: Long, licenseNote: String?) {
        dao.updateLicenseNote(imageId, licenseNote)
    }

    override suspend fun getNonTrainableImages(datasetId: Long): List<DatasetImage> {
        val entities = dao.getNonTrainableImages(datasetId)
        return entities.map { entity ->
            val tags = metaDao.getTagsForImage(entity.id).map { ImageTag(it.id, it.datasetImageId, it.tag) }
            val caption = metaDao.getCaption(entity.id)?.let { Caption(it.datasetImageId, it.text) }
            DatasetImage(
                id = entity.id,
                datasetId = entity.datasetId,
                imageUrl = entity.imageUrl,
                sourceType = ImageSource.valueOf(entity.sourceType),
                trainable = entity.trainable,
                addedAt = entity.addedAt,
                tags = tags,
                caption = caption,
                licenseNote = entity.licenseNote,
                pHash = entity.pHash,
                excluded = entity.excluded,
                width = entity.width,
                height = entity.height,
            )
        }
    }

    override suspend fun updatePHash(imageId: Long, pHash: String?) {
        dao.updatePHash(imageId, pHash)
    }

    override suspend fun markExcluded(imageId: Long, excluded: Boolean) {
        dao.updateExcluded(imageId, excluded)
    }

    override suspend fun updateDimensions(imageId: Long, width: Int, height: Int) {
        dao.updateDimensions(imageId, width, height)
    }
}
