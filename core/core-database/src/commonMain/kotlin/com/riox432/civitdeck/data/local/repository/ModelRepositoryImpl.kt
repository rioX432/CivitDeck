package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.data.api.DataParseException
import com.riox432.civitdeck.data.api.ModelListQuery
import com.riox432.civitdeck.data.api.dto.ModelListResponse
import com.riox432.civitdeck.data.api.dto.ModelResponse
import com.riox432.civitdeck.data.api.dto.ModelVersionResponse
import com.riox432.civitdeck.data.api.dto.toDomain
import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelLicenseInfo
import com.riox432.civitdeck.domain.model.ModelSearchQuery
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.PageMetadata
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.util.Logger
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ModelRepositoryImpl(
    private val api: CivitAiApi,
    private val localCache: LocalCacheDataSource,
    private val json: Json,
) : ModelRepository {

    private companion object {
        const val TAG = "ModelRepositoryImpl"
    }

    /**
     * Try fresh cache first, then fall back to stale (TTL-expired) cache for offline mode.
     */
    private suspend fun getCachedWithFallback(key: String): String? =
        localCache.getCached(key) ?: localCache.getCachedIgnoringTtl(key)

    override suspend fun getModels(query: ModelSearchQuery): PaginatedResult<Model> {
        val cacheKey = buildCacheKey(
            "models",
            query.query,
            query.tag,
            query.type?.name,
            query.sort?.toApiParam(),
            query.period?.toApiParam(),
            query.baseModels?.joinToString(",") { it.apiValue },
            query.cursor,
            query.limit?.toString(),
            query.username,
            query.nsfw?.toString(),
        )
        return try {
            val response = api.getModels(
                ModelListQuery(
                    query = query.query,
                    tag = query.tag,
                    type = query.type?.name,
                    sort = query.sort?.toApiParam(),
                    period = query.period?.toApiParam(),
                    baseModels = query.baseModels?.map { it.apiValue },
                    cursor = query.cursor,
                    limit = query.limit,
                    username = query.username,
                    nsfw = query.nsfw,
                ),
            )
            localCache.putCache(cacheKey, json.encodeToString(ModelListResponse.serializer(), response))
            PaginatedResult(
                items = response.items.map { it.toDomain() },
                metadata = response.metadata.toDomain(),
            )
        } catch (e: DataParseException) {
            Logger.w(TAG, "Parse error fetching models, falling back to cache: ${e.message}")
            getModelsFromCacheOrEmpty(cacheKey)
        } catch (e: SerializationException) {
            Logger.w(TAG, "Serialization error fetching models, falling back to cache: ${e.message}")
            getModelsFromCacheOrEmpty(cacheKey)
        } catch (e: Exception) {
            val cached = getCachedWithFallback(cacheKey)
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
            val cached = getCachedWithFallback(cacheKey)
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
            val cached = getCachedWithFallback(cacheKey)
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
            val cached = getCachedWithFallback(cacheKey)
            if (cached != null) {
                json.decodeFromString(ModelVersionResponse.serializer(), cached).toDomain()
            } else {
                throw e
            }
        }
    }

    override suspend fun getModelLicense(versionId: Long): ModelLicenseInfo? {
        return try {
            val response = api.getModelVersionLicense(versionId)
            response.model?.let {
                ModelLicenseInfo(
                    allowNoCredit = it.allowNoCredit,
                    allowCommercialUse = it.allowCommercialUse,
                    allowDerivatives = it.allowDerivatives,
                )
            }
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to fetch license for version $versionId: ${e.message}")
            null
        }
    }

    private suspend fun getModelsFromCacheOrEmpty(cacheKey: String): PaginatedResult<Model> {
        val cached = localCache.getCached(cacheKey)
        return if (cached != null) {
            val response = json.decodeFromString(ModelListResponse.serializer(), cached)
            PaginatedResult(
                items = response.items.map { it.toDomain() },
                metadata = response.metadata.toDomain(),
            )
        } else {
            PaginatedResult(items = emptyList(), metadata = PageMetadata(null, null))
        }
    }
}

private fun SortOrder.toApiParam(): String = when (this) {
    SortOrder.HighestRated -> "Highest Rated"
    SortOrder.MostDownloaded -> "Most Downloaded"
    SortOrder.Newest -> "Newest"
    // Quality sort is client-side only; fetch by Highest Rated as closest proxy
    SortOrder.Quality -> "Highest Rated"
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
