package com.riox432.civitdeck.data.api.tensorart

import com.riox432.civitdeck.data.api.DataParseException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.ContentConvertException

/**
 * API client for TensorArt model search.
 *
 * Base URL: https://api.tensor.art/v1
 * No authentication required for search.
 */
class TensorArtApi(private val client: HttpClient) {

    /**
     * Search models on TensorArt.
     *
     * @param query search query string
     * @param sort sort order (e.g. "MOST_DOWNLOADED", "NEWEST")
     * @param page page number (1-based)
     * @param pageSize number of results per page
     */
    suspend fun searchModels(
        query: String = "",
        sort: String = "MOST_DOWNLOADED",
        page: Int = 1,
        pageSize: Int = 20,
    ): TensorArtSearchResponse {
        return try {
            client.post("$BASE_URL/models/search") {
                contentType(ContentType.Application.Json)
                setBody(
                    TensorArtSearchRequest(
                        query = query,
                        sort = sort,
                        page = page,
                        pageSize = pageSize,
                    ),
                )
            }.body()
        } catch (e: ContentConvertException) {
            throw DataParseException(e.message, e)
        }
    }

    companion object {
        const val BASE_URL = "https://api.tensor.art/v1"
    }
}
