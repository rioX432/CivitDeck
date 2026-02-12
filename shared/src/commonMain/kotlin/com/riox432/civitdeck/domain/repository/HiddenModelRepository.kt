package com.riox432.civitdeck.domain.repository

interface HiddenModelRepository {
    suspend fun getHiddenModelIds(): Set<Long>
    suspend fun hideModel(modelId: Long, modelName: String)
    suspend fun unhideModel(modelId: Long)
}
