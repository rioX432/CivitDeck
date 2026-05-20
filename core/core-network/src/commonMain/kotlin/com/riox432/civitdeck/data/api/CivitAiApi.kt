package com.riox432.civitdeck.data.api

import com.riox432.civitdeck.data.api.dto.CreateReviewInput
import com.riox432.civitdeck.data.api.dto.CreatorListResponse
import com.riox432.civitdeck.data.api.dto.ImageListResponse
import com.riox432.civitdeck.data.api.dto.ModelListResponse
import com.riox432.civitdeck.data.api.dto.ModelResponse
import com.riox432.civitdeck.data.api.dto.ModelVersionResponse
import com.riox432.civitdeck.data.api.dto.RatingTotalsInput
import com.riox432.civitdeck.data.api.dto.RatingTotalsResponse
import com.riox432.civitdeck.data.api.dto.ReviewListInput
import com.riox432.civitdeck.data.api.dto.ReviewListResponse
import com.riox432.civitdeck.data.api.dto.TagListResponse
import com.riox432.civitdeck.data.api.dto.TrpcInput
import com.riox432.civitdeck.data.api.dto.TrpcResponse
import com.riox432.civitdeck.data.api.dto.UserMeResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

class CivitAiApi(private val client: HttpClient) {

    suspend fun getModels(params: ModelListQuery = ModelListQuery()): ModelListResponse {
        return try {
            client.get("$BASE_URL/models") {
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
            client.get("$BASE_URL/models/$id").body()
        } catch (e: ContentConvertException) {
            throw DataParseException(e.message, e)
        }
    }

    suspend fun getModelVersion(id: Long): ModelVersionResponse {
        return try {
            client.get("$BASE_URL/model-versions/$id").body()
        } catch (e: ContentConvertException) {
            throw DataParseException(e.message, e)
        }
    }

    suspend fun getModelVersionLicense(versionId: Long): ModelVersionResponse {
        return try {
            client.get("$BASE_URL/model-versions/$versionId").body()
        } catch (e: ContentConvertException) {
            throw DataParseException(e.message, e)
        }
    }

    suspend fun getModelVersionByHash(hash: String): ModelVersionResponse {
        return try {
            client.get("$BASE_URL/model-versions/by-hash/$hash").body()
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

    suspend fun getMe(apiKey: String): UserMeResponse {
        return client.get("$BASE_URL/me") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }.body()
    }

    // --- tRPC endpoints for reviews ---

    suspend fun getReviews(
        modelId: Long,
        modelVersionId: Long? = null,
        limit: Int = 20,
        cursor: Int? = null,
    ): ReviewListResponse {
        val input = TrpcInput(
            ReviewListInput(
                modelId = modelId,
                modelVersionId = modelVersionId,
                limit = limit,
                cursor = cursor,
            ),
        )
        val response: TrpcResponse<ReviewListResponse> =
            client.get("$TRPC_BASE_URL/resourceReview.getInfinite") {
                parameter("input", Json.encodeToString(input))
            }.body()
        return response.result.data.json
    }

    suspend fun getRatingTotals(
        modelId: Long,
        modelVersionId: Long? = null,
    ): RatingTotalsResponse {
        val input = TrpcInput(
            RatingTotalsInput(modelId = modelId, modelVersionId = modelVersionId),
        )
        val response: TrpcResponse<RatingTotalsResponse> =
            client.get("$TRPC_BASE_URL/resourceReview.getRatingTotals") {
                parameter("input", Json.encodeToString(input))
            }.body()
        return response.result.data.json
    }

    suspend fun createReview(
        apiKey: String,
        modelId: Long,
        modelVersionId: Long,
        rating: Int,
        recommended: Boolean,
        details: String? = null,
    ) {
        val input = TrpcInput(
            CreateReviewInput(
                modelId = modelId,
                modelVersionId = modelVersionId,
                rating = rating,
                recommended = recommended,
                details = details,
            ),
        )
        client.post("$TRPC_BASE_URL/resourceReview.create") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(input)
        }
    }

    companion object {
        const val BASE_URL = "https://civitai.com/api/v1"
        private const val TRPC_BASE_URL = "https://civitai.com/api/trpc"
    }
}
