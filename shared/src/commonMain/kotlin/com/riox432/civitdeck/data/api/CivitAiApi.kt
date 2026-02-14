package com.riox432.civitdeck.data.api

import com.riox432.civitdeck.data.api.dto.CreatorListResponse
import com.riox432.civitdeck.data.api.dto.ImageListResponse
import com.riox432.civitdeck.data.api.dto.ModelListResponse
import com.riox432.civitdeck.data.api.dto.ModelResponse
import com.riox432.civitdeck.data.api.dto.ModelVersionResponse
import com.riox432.civitdeck.data.api.dto.TagListResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class CivitAiApi(private val client: HttpClient) {

    companion object {
        const val BASE_URL = "https://civitai.com/api/v1"
    }

    @Suppress("LongParameterList")
    suspend fun getModels(
        query: String? = null,
        tag: String? = null,
        type: String? = null,
        sort: String? = null,
        period: String? = null,
        baseModels: List<String>? = null,
        cursor: String? = null,
        limit: Int? = null,
        username: String? = null,
        nsfw: Boolean? = null,
    ): ModelListResponse {
        return client.get("$BASE_URL/models") {
            query?.let { parameter("query", it) }
            tag?.let { parameter("tag", it) }
            type?.let { parameter("types", it) }
            sort?.let { parameter("sort", it) }
            period?.let { parameter("period", it) }
            baseModels?.forEach { parameter("baseModels", it) }
            cursor?.let { parameter("cursor", it) }
            limit?.let { parameter("limit", it) }
            username?.let { parameter("username", it) }
            nsfw?.let { parameter("nsfw", it) }
        }.body()
    }

    suspend fun getModel(id: Long): ModelResponse {
        return client.get("$BASE_URL/models/$id").body()
    }

    suspend fun getModelVersion(id: Long): ModelVersionResponse {
        return client.get("$BASE_URL/model-versions/$id").body()
    }

    suspend fun getModelVersionByHash(hash: String): ModelVersionResponse {
        return client.get("$BASE_URL/model-versions/by-hash/$hash").body()
    }

    suspend fun getImages(
        modelId: Long? = null,
        modelVersionId: Long? = null,
        username: String? = null,
        sort: String? = null,
        period: String? = null,
        nsfw: String? = null,
        limit: Int? = null,
        cursor: String? = null,
    ): ImageListResponse {
        return client.get("$BASE_URL/images") {
            modelId?.let { parameter("modelId", it) }
            modelVersionId?.let { parameter("modelVersionId", it) }
            username?.let { parameter("username", it) }
            sort?.let { parameter("sort", it) }
            period?.let { parameter("period", it) }
            nsfw?.let { parameter("nsfw", it) }
            limit?.let { parameter("limit", it) }
            cursor?.let { parameter("cursor", it) }
        }.body()
    }

    suspend fun getCreators(
        query: String? = null,
        page: Int? = null,
        limit: Int? = null,
    ): CreatorListResponse {
        return client.get("$BASE_URL/creators") {
            query?.let { parameter("query", it) }
            page?.let { parameter("page", it) }
            limit?.let { parameter("limit", it) }
        }.body()
    }

    suspend fun getTags(
        query: String? = null,
        page: Int? = null,
        limit: Int? = null,
    ): TagListResponse {
        return client.get("$BASE_URL/tags") {
            query?.let { parameter("query", it) }
            page?.let { parameter("page", it) }
            limit?.let { parameter("limit", it) }
        }.body()
    }
}
