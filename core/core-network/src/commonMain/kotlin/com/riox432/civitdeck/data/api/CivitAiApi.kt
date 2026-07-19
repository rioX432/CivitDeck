package com.riox432.civitdeck.data.api

import com.riox432.civitdeck.data.api.dto.CreatorListResponse
import com.riox432.civitdeck.data.api.dto.ImageListResponse
import com.riox432.civitdeck.data.api.dto.ModelListResponse
import com.riox432.civitdeck.data.api.dto.ModelResponse
import com.riox432.civitdeck.data.api.dto.ModelVersionResponse
import com.riox432.civitdeck.data.api.dto.TagListResponse
import com.riox432.civitdeck.data.api.dto.UserMeResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.serialization.ContentConvertException

/**
 * Raw query parameters for the CivitAI /models endpoint.
 * All values are strings matching the API specification.
 */
data class ModelListQuery(
    val query: String? = null,
    val tag: String? = null,
    val type: String? = null,
    val sort: String? = null,
    val period: String? = null,
    val baseModels: List<String>? = null,
    val cursor: String? = null,
    val limit: Int? = null,
    val username: String? = null,
    val nsfw: Boolean? = null,
)

class CivitAiApi(
    private val client: HttpClient,
    private val endpoints: CivitAiEndpoints = CivitAiEndpoints.Production,
) {

    suspend fun getModels(params: ModelListQuery = ModelListQuery()): ModelListResponse {
        return try {
            client.get("${endpoints.apiBaseUrl}/models") {
                params.query?.let { parameter("query", it) }
                params.tag?.let { parameter("tag", it) }
                params.type?.let { parameter("types", it) }
                params.sort?.let { parameter("sort", it) }
                params.period?.let { parameter("period", it) }
                params.baseModels?.forEach { parameter("baseModels", it) }
                params.cursor?.let { parameter("cursor", it) }
                params.limit?.let { parameter("limit", it) }
                params.username?.let { parameter("username", it) }
                params.nsfw?.let { parameter("nsfw", it) }
            }.body()
        } catch (e: ContentConvertException) {
            throw DataParseException(e.message, e)
        }
    }

    suspend fun getModel(id: Long): ModelResponse {
        return try {
            client.get("${endpoints.apiBaseUrl}/models/$id").body()
        } catch (e: ContentConvertException) {
            throw DataParseException(e.message, e)
        }
    }

    suspend fun getModelVersion(id: Long): ModelVersionResponse {
        return try {
            client.get("${endpoints.apiBaseUrl}/model-versions/$id").body()
        } catch (e: ContentConvertException) {
            throw DataParseException(e.message, e)
        }
    }

    suspend fun getModelVersionLicense(versionId: Long): ModelVersionResponse {
        return try {
            client.get("${endpoints.apiBaseUrl}/model-versions/$versionId").body()
        } catch (e: ContentConvertException) {
            throw DataParseException(e.message, e)
        }
    }

    suspend fun getModelVersionByHash(hash: String): ModelVersionResponse {
        return try {
            client.get("${endpoints.apiBaseUrl}/model-versions/by-hash/$hash").body()
        } catch (e: ContentConvertException) {
            throw DataParseException(e.message, e)
        }
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
        return client.get("${endpoints.apiBaseUrl}/images") {
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
        return client.get("${endpoints.apiBaseUrl}/creators") {
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
        return client.get("${endpoints.apiBaseUrl}/tags") {
            query?.let { parameter("query", it) }
            page?.let { parameter("page", it) }
            limit?.let { parameter("limit", it) }
        }.body()
    }

    suspend fun getMe(apiKey: String): UserMeResponse {
        return client.get("${endpoints.apiBaseUrl}/me") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }.body()
    }
}
