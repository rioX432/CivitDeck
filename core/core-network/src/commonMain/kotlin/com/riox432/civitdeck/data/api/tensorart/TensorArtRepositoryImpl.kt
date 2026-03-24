package com.riox432.civitdeck.data.api.tensorart

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.repository.TensorArtRepository

class TensorArtRepositoryImpl(
    private val api: TensorArtApi,
) : TensorArtRepository {

    override suspend fun searchModels(
        query: String,
        page: Int,
        pageSize: Int,
    ): List<Model> {
        return api.searchModels(
            query = query,
            page = page,
            pageSize = pageSize,
        ).data?.models?.map { it.toDomain() } ?: emptyList()
    }
}
