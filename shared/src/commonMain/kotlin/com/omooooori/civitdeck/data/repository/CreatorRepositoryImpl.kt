package com.omooooori.civitdeck.data.repository

import com.omooooori.civitdeck.data.api.CivitAiApi
import com.omooooori.civitdeck.data.api.dto.toDomain
import com.omooooori.civitdeck.domain.model.Creator
import com.omooooori.civitdeck.domain.model.PaginatedResult
import com.omooooori.civitdeck.domain.repository.CreatorRepository

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
