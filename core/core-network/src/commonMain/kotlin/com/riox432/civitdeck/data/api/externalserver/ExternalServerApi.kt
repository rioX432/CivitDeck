package com.riox432.civitdeck.data.api.externalserver

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.contentType

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

    suspend fun getGenerationOptions(): GenerationOptionsResponseDto {
        return client.get("$baseUrl/api/generation/options") {
            if (apiKey.isNotBlank()) header("X-API-Key", apiKey)
        }.body()
    }

    suspend fun getDependentChoices(endpoint: String): List<GenerationChoiceDto> {
        val url = if (endpoint.startsWith("/")) "$baseUrl$endpoint" else "$baseUrl/$endpoint"
        return client.get(url) {
            if (apiKey.isNotBlank()) header("X-API-Key", apiKey)
        }.body()
    }

    suspend fun executeGeneration(
        params: Map<String, String>,
    ): GenerationExecuteResponseDto {
        return client.post("$baseUrl/api/generation/execute") {
            if (apiKey.isNotBlank()) header("X-API-Key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(GenerationExecuteRequestDto(params))
        }.body()
    }

    suspend fun getGenerationStatus(jobId: String): GenerationStatusResponseDto {
        return client.get("$baseUrl/api/generation/status") {
            if (apiKey.isNotBlank()) header("X-API-Key", apiKey)
            url { parameters.append("job_id", jobId) }
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
