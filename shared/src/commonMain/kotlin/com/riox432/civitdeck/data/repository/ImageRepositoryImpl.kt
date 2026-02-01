package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.data.api.dto.ImageListResponse
import com.riox432.civitdeck.data.api.dto.toDomain
import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.domain.model.Image
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.ImageRepository
import kotlinx.serialization.json.Json

class ImageRepositoryImpl(
    private val api: CivitAiApi,
    private val localCache: LocalCacheDataSource,
    private val json: Json,
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
        val cacheKey = buildCacheKey(
            "images",
            modelId?.toString(),
            modelVersionId?.toString(),
            username,
            sort?.let {
                when (it) {
                    SortOrder.HighestRated -> "Most Reactions"
                    SortOrder.MostDownloaded -> "Most Comments"
                    SortOrder.Newest -> "Newest"
                }
            },
            period?.name,
            nsfwLevel?.name,
            limit?.toString(),
            cursor,
        )
        return try {
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
            localCache.putCache(
                cacheKey,
                json.encodeToString(ImageListResponse.serializer(), response),
            )
            PaginatedResult(
                items = response.items.map { it.toDomain() },
                metadata = response.metadata.toDomain(),
            )
        } catch (e: Exception) {
            val cached = localCache.getCached(cacheKey)
            if (cached != null) {
                val response = json.decodeFromString(ImageListResponse.serializer(), cached)
                PaginatedResult(
                    items = response.items.map { it.toDomain() },
                    metadata = response.metadata.toDomain(),
                )
            } else {
                throw e
            }
        }
    }
}

private fun buildCacheKey(vararg parts: String?): String =
    parts.filterNotNull().joinToString(":")
