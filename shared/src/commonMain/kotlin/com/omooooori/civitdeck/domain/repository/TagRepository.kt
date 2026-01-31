package com.omooooori.civitdeck.domain.repository

import com.omooooori.civitdeck.domain.model.PaginatedResult
import com.omooooori.civitdeck.domain.model.Tag

interface TagRepository {
    suspend fun getTags(
        query: String? = null,
        page: Int? = null,
        limit: Int? = null,
    ): PaginatedResult<Tag>
}
