package com.omooooori.civitdeck.domain.repository

import com.omooooori.civitdeck.domain.model.Image
import com.omooooori.civitdeck.domain.model.NsfwLevel
import com.omooooori.civitdeck.domain.model.PaginatedResult
import com.omooooori.civitdeck.domain.model.SortOrder
import com.omooooori.civitdeck.domain.model.TimePeriod

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
