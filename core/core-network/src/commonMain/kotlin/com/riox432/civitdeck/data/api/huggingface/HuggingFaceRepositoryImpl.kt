package com.riox432.civitdeck.data.api.huggingface

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.repository.HuggingFaceRepository

class HuggingFaceRepositoryImpl(
    private val api: HuggingFaceApi,
) : HuggingFaceRepository {

    override suspend fun searchModels(
        query: String?,
        limit: Int,
        offset: Int,
    ): List<Model> {
        return api.searchModels(
            query = query,
            limit = limit,
            offset = offset,
        ).map { it.toDomain() }
    }
}
