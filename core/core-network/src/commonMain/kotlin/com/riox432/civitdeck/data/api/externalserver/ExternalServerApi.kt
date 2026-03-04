package com.riox432.civitdeck.data.api.externalserver

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.URLBuilder

class ExternalServerApi(
    private val client: HttpClient,
) {
    private var baseUrl: String = ""
    private var apiKey: String = ""

    fun configure(baseUrl: String, apiKey: String) {
        this.baseUrl = baseUrl.trimEnd('/')
        this.apiKey = apiKey
    }

    suspend fun getCapabilities(): CapabilitiesResponseDto {
        return client.get("$baseUrl/api/capabilities") {
            if (apiKey.isNotBlank()) header("X-API-Key", apiKey)
        }.body()
    }

    suspend fun getImages(
        page: Int,
        perPage: Int,
        filters: Map<String, String>,
    ): PaginatedImagesResponseDto {
        val url = URLBuilder("$baseUrl/api/images").apply {
            parameters.append("page", page.toString())
            parameters.append("per_page", perPage.toString())
            filters.forEach { (key, value) ->
                if (value.isNotBlank()) parameters.append(key, value)
            }
        }.buildString()
        return client.get(url) {
            if (apiKey.isNotBlank()) header("X-API-Key", apiKey)
        }.body()
    }

    /**
     * Health check: tries GET /api/capabilities and returns true if reachable.
     */
    suspend fun testConnection(): Boolean = try {
        client.get("$baseUrl/api/capabilities") {
            if (apiKey.isNotBlank()) header("X-API-Key", apiKey)
        }
        true
    } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
        false
    }
}
