package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.HiddenModel

interface HiddenModelRepository {
    suspend fun getHiddenModelIds(): Set<Long>
    suspend fun getHiddenModels(): List<HiddenModel>
    suspend fun hideModel(modelId: Long, modelName: String)
    suspend fun unhideModel(modelId: Long)
}
