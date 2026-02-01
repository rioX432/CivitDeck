package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.Tag

interface TagRepository {
    suspend fun getTags(
        query: String? = null,
        page: Int? = null,
        limit: Int? = null,
    ): PaginatedResult<Tag>
}
