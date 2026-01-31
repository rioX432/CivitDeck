package com.omooooori.civitdeck.domain.repository

import com.omooooori.civitdeck.domain.model.Creator
import com.omooooori.civitdeck.domain.model.PaginatedResult

interface CreatorRepository {
    suspend fun getCreators(
        query: String? = null,
        page: Int? = null,
        limit: Int? = null,
    ): PaginatedResult<Creator>
}
