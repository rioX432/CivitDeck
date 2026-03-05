package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.data.api.DataParseException
import com.riox432.civitdeck.data.api.dto.toDomain
import com.riox432.civitdeck.domain.model.PageMetadata
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.Tag
import com.riox432.civitdeck.domain.repository.TagRepository
import com.riox432.civitdeck.util.Logger
import kotlinx.serialization.SerializationException

class TagRepositoryImpl(
    private val api: CivitAiApi,
) : TagRepository {

    private companion object {
        const val TAG = "TagRepositoryImpl"
    }

    override suspend fun getTags(
        query: String?,
        page: Int?,
        limit: Int?,
    ): PaginatedResult<Tag> {
        return try {
            val response = api.getTags(
                query = query,
                page = page,
                limit = limit,
            )
            PaginatedResult(
                items = response.items.map { it.toDomain() },
                metadata = response.metadata.toDomain(),
            )
        } catch (@Suppress("SwallowedException") e: DataParseException) {
            Logger.w(TAG, "Parse error fetching tags, returning empty: ${e.message}")
            PaginatedResult(items = emptyList(), metadata = PageMetadata(null, null))
        } catch (@Suppress("SwallowedException") e: SerializationException) {
            Logger.w(TAG, "Serialization error fetching tags, returning empty: ${e.message}")
            PaginatedResult(items = emptyList(), metadata = PageMetadata(null, null))
        }
    }
}
