package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.DatasetImageMetaDao
import com.riox432.civitdeck.data.local.entity.CaptionEntity
import com.riox432.civitdeck.domain.model.Caption
import com.riox432.civitdeck.domain.repository.CaptionRepository

class CaptionRepositoryImpl(private val metaDao: DatasetImageMetaDao) : CaptionRepository {

    override suspend fun getCaption(datasetImageId: Long): Caption? =
        metaDao.getCaption(datasetImageId)?.toDomain()

    override suspend fun setCaption(datasetImageId: Long, text: String) {
        metaDao.upsertCaption(CaptionEntity(datasetImageId = datasetImageId, text = text))
    }

    private fun CaptionEntity.toDomain() = Caption(datasetImageId = datasetImageId, text = text)
}
