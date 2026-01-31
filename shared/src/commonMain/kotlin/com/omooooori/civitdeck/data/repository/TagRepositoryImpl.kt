package com.omooooori.civitdeck.data.repository

import com.omooooori.civitdeck.data.api.CivitAiApi
import com.omooooori.civitdeck.data.api.dto.toDomain
import com.omooooori.civitdeck.domain.model.PaginatedResult
import com.omooooori.civitdeck.domain.model.Tag
import com.omooooori.civitdeck.domain.repository.TagRepository

class TagRepositoryImpl(
    private val api: CivitAiApi,
) : TagRepository {

    override suspend fun getTags(
        query: String?,
        page: Int?,
        limit: Int?,
    ): PaginatedResult<Tag> {
        val response = api.getTags(
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
