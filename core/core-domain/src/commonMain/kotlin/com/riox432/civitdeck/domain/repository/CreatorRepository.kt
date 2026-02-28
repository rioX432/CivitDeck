package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.Creator
import com.riox432.civitdeck.domain.model.PaginatedResult

interface CreatorRepository {
    suspend fun getCreators(
        query: String? = null,
        page: Int? = null,
        limit: Int? = null,
    ): PaginatedResult<Creator>
}
