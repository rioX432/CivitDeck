package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.data.local.entity.HiddenModelEntity

interface HiddenModelRepository {
    suspend fun getHiddenModelIds(): Set<Long>
    suspend fun getHiddenModels(): List<HiddenModelEntity>
    suspend fun hideModel(modelId: Long, modelName: String)
    suspend fun unhideModel(modelId: Long)
}
