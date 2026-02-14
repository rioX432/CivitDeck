package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.data.api.dto.ModelListResponse
import com.riox432.civitdeck.data.api.dto.ModelResponse
import com.riox432.civitdeck.data.api.dto.ModelVersionResponse
import com.riox432.civitdeck.data.api.dto.toDomain
import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.ModelRepository
import kotlinx.serialization.json.Json

class ModelRepositoryImpl(
    private val api: CivitAiApi,
    private val localCache: LocalCacheDataSource,
    private val json: Json,
) : ModelRepository {

    override suspend fun getModels(
        query: String?,
        tag: String?,
        type: ModelType?,
        sort: SortOrder?,
        period: TimePeriod?,
        baseModels: List<BaseModel>?,
        cursor: String?,
        limit: Int?,
        username: String?,
        nsfw: Boolean?,
    ): PaginatedResult<Model> {
        val cacheKey = buildCacheKey(
            "models",
            query,
            tag,
            type?.name,
            sort?.toApiParam(),
            period?.toApiParam(),
            baseModels?.joinToString(",") { it.apiValue },
            cursor,
            limit?.toString(),
            username,
            nsfw?.toString(),
        )
        return try {
            val response = api.getModels(
                query = query,
                tag = tag,
                type = type?.name,
                sort = sort?.toApiParam(),
                period = period?.toApiParam(),
                baseModels = baseModels?.map { it.apiValue },
                cursor = cursor,
                limit = limit,
                username = username,
                nsfw = nsfw,
            )
            localCache.putCache(cacheKey, json.encodeToString(ModelListResponse.serializer(), response))
            PaginatedResult(
                items = response.items.map { it.toDomain() },
                metadata = response.metadata.toDomain(),
            )
        } catch (e: Exception) {
            val cached = localCache.getCached(cacheKey)
            if (cached != null) {
                val response = json.decodeFromString(ModelListResponse.serializer(), cached)
                PaginatedResult(
                    items = response.items.map { it.toDomain() },
                    metadata = response.metadata.toDomain(),
                )
            } else {
                throw e
            }
        }
    }

    override suspend fun getModel(id: Long): Model {
        val cacheKey = "model:$id"
        return try {
            val response = api.getModel(id)
            localCache.putCache(cacheKey, json.encodeToString(ModelResponse.serializer(), response))
            response.toDomain()
        } catch (e: Exception) {
            val cached = localCache.getCached(cacheKey)
            if (cached != null) {
                json.decodeFromString(ModelResponse.serializer(), cached).toDomain()
            } else {
                throw e
            }
        }
    }

    override suspend fun getModelVersion(id: Long): ModelVersion {
        val cacheKey = "modelVersion:$id"
        return try {
            val response = api.getModelVersion(id)
            localCache.putCache(
                cacheKey,
                json.encodeToString(ModelVersionResponse.serializer(), response),
            )
            response.toDomain()
        } catch (e: Exception) {
            val cached = localCache.getCached(cacheKey)
            if (cached != null) {
                json.decodeFromString(ModelVersionResponse.serializer(), cached).toDomain()
            } else {
                throw e
            }
        }
    }

    override suspend fun getModelVersionByHash(hash: String): ModelVersion {
        val cacheKey = "modelVersionHash:$hash"
        return try {
            val response = api.getModelVersionByHash(hash)
            localCache.putCache(
                cacheKey,
                json.encodeToString(ModelVersionResponse.serializer(), response),
            )
            response.toDomain()
        } catch (e: Exception) {
            val cached = localCache.getCached(cacheKey)
            if (cached != null) {
                json.decodeFromString(ModelVersionResponse.serializer(), cached).toDomain()
            } else {
                throw e
            }
        }
    }
}

private fun SortOrder.toApiParam(): String = when (this) {
    SortOrder.HighestRated -> "Highest Rated"
    SortOrder.MostDownloaded -> "Most Downloaded"
    SortOrder.Newest -> "Newest"
}

private fun TimePeriod.toApiParam(): String = when (this) {
    TimePeriod.AllTime -> "AllTime"
    TimePeriod.Year -> "Year"
    TimePeriod.Month -> "Month"
    TimePeriod.Week -> "Week"
    TimePeriod.Day -> "Day"
}

private fun buildCacheKey(vararg parts: String?): String =
    parts.filterNotNull().joinToString(":")
