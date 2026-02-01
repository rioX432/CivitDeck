package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.Image
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod

interface ImageRepository {
    suspend fun getImages(
        modelId: Long? = null,
        modelVersionId: Long? = null,
        username: String? = null,
        sort: SortOrder? = null,
        period: TimePeriod? = null,
        nsfwLevel: NsfwLevel? = null,
        limit: Int? = null,
        cursor: String? = null,
    ): PaginatedResult<Image>
}
