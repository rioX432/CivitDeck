package com.omooooori.civitdeck.domain.usecase

import com.omooooori.civitdeck.domain.model.Image
import com.omooooori.civitdeck.domain.model.NsfwLevel
import com.omooooori.civitdeck.domain.model.PaginatedResult
import com.omooooori.civitdeck.domain.model.SortOrder
import com.omooooori.civitdeck.domain.model.TimePeriod
import com.omooooori.civitdeck.domain.repository.ImageRepository

class GetImagesUseCase(private val repository: ImageRepository) {
    suspend operator fun invoke(
        modelId: Long? = null,
        modelVersionId: Long? = null,
        username: String? = null,
        sort: SortOrder? = null,
        period: TimePeriod? = null,
        nsfwLevel: NsfwLevel? = null,
        limit: Int? = null,
        cursor: String? = null,
    ): PaginatedResult<Image> = repository.getImages(
        modelId = modelId,
        modelVersionId = modelVersionId,
        username = username,
        sort = sort,
        period = period,
        nsfwLevel = nsfwLevel,
        limit = limit,
        cursor = cursor,
    )
}
