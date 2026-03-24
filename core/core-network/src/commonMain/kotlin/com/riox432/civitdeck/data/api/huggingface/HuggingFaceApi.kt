package com.riox432.civitdeck.data.api.huggingface

import com.riox432.civitdeck.data.api.DataParseException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.ContentConvertException

class HuggingFaceApi(private val client: HttpClient) {

    suspend fun searchModels(
        query: String? = null,
        filter: String? = "diffusers",
        sort: String? = "downloads",
        limit: Int = DEFAULT_LIMIT,
        offset: Int = 0,
    ): List<HuggingFaceModelDto> {
        return try {
            client.get("$BASE_URL/models") {
                query?.let { parameter("search", it) }
                filter?.let { parameter("filter", it) }
                sort?.let { parameter("sort", it) }
                parameter("limit", limit)
                if (offset > 0) parameter("offset", offset)
            }.body()
        } catch (e: ContentConvertException) {
            throw DataParseException(e.message, e)
        }
    }

    companion object {
        const val BASE_URL = "https://huggingface.co/api"
        private const val DEFAULT_LIMIT = 20
    }
}
