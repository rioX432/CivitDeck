package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.HiddenModelDao
import com.riox432.civitdeck.data.local.entity.HiddenModelEntity
import com.riox432.civitdeck.domain.repository.HiddenModelRepository

class HiddenModelRepositoryImpl(
    private val dao: HiddenModelDao,
) : HiddenModelRepository {

    override suspend fun getHiddenModelIds(): Set<Long> =
        dao.getAllIds().toSet()

    override suspend fun hideModel(modelId: Long, modelName: String) {
        dao.insert(
            HiddenModelEntity(
                modelId = modelId,
                modelName = modelName,
                hiddenAt = currentTimeMillis(),
            ),
        )
    }

    override suspend fun unhideModel(modelId: Long) {
        dao.delete(modelId)
    }
}
