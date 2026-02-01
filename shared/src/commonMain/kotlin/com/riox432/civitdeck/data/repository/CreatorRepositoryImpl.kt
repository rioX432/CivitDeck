package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.data.api.dto.toDomain
import com.riox432.civitdeck.domain.model.Creator
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.repository.CreatorRepository

class CreatorRepositoryImpl(
    private val api: CivitAiApi,
) : CreatorRepository {

    override suspend fun getCreators(
        query: String?,
        page: Int?,
        limit: Int?,
    ): PaginatedResult<Creator> {
        val response = api.getCreators(
            query = query,
            page = page,
            limit = limit,
        )
        return PaginatedResult(
            items = response.items.map { it.toDomain() },
            metadata = response.metadata.toDomain(),
        )
    }
}
