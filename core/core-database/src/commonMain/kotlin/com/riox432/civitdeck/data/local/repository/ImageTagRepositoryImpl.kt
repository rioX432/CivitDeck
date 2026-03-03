package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.DatasetImageMetaDao
import com.riox432.civitdeck.data.local.entity.ImageTagEntity
import com.riox432.civitdeck.domain.model.ImageTag
import com.riox432.civitdeck.domain.repository.ImageTagRepository

class ImageTagRepositoryImpl(private val metaDao: DatasetImageMetaDao) : ImageTagRepository {

    override suspend fun getTagsForImage(datasetImageId: Long): List<ImageTag> =
        metaDao.getTagsForImage(datasetImageId).map { it.toDomain() }

    override suspend fun addTagsToImages(imageIds: List<Long>, tags: List<String>) {
        for (imageId in imageIds) {
            metaDao.insertTags(tags.map { ImageTagEntity(datasetImageId = imageId, tag = it) })
        }
    }

    override suspend fun removeTagsFromImages(imageIds: List<Long>, tags: List<String>) {
        for (imageId in imageIds) {
            for (tag in tags) {
                metaDao.deleteTagByName(imageId, tag)
            }
        }
    }

    override suspend fun getTagSuggestions(datasetId: Long, prefix: String): List<String> =
        metaDao.getTagSuggestions(datasetId, prefix)

    private fun ImageTagEntity.toDomain() = ImageTag(id = id, datasetImageId = datasetImageId, tag = tag)
}
