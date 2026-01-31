package com.omooooori.civitdeck.data.repository

import com.omooooori.civitdeck.data.api.CivitAiApi
import com.omooooori.civitdeck.data.api.dto.toDomain
import com.omooooori.civitdeck.domain.model.Image
import com.omooooori.civitdeck.domain.model.NsfwLevel
import com.omooooori.civitdeck.domain.model.PaginatedResult
import com.omooooori.civitdeck.domain.model.SortOrder
import com.omooooori.civitdeck.domain.model.TimePeriod
import com.omooooori.civitdeck.domain.repository.ImageRepository

class ImageRepositoryImpl(
    private val api: CivitAiApi,
) : ImageRepository {

    override suspend fun getImages(
        modelId: Long?,
        modelVersionId: Long?,
        username: String?,
        sort: SortOrder?,
        period: TimePeriod?,
        nsfwLevel: NsfwLevel?,
        limit: Int?,
        cursor: String?,
    ): PaginatedResult<Image> {
        val response = api.getImages(
            modelId = modelId,
            modelVersionId = modelVersionId,
            username = username,
            sort = sort?.let {
                when (it) {
                    SortOrder.HighestRated -> "Most Reactions"
                    SortOrder.MostDownloaded -> "Most Comments"
                    SortOrder.Newest -> "Newest"
                }
            },
            period = period?.name,
            nsfw = nsfwLevel?.name,
            limit = limit,
            cursor = cursor,
        )
        return PaginatedResult(
            items = response.items.map { it.toDomain() },
            metadata = response.metadata.toDomain(),
        )
    }
}
