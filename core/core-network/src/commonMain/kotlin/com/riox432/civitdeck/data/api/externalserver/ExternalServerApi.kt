package com.riox432.civitdeck.data.api.externalserver

import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.contentType
import kotlin.concurrent.Volatile

/**
 * Holds the server configuration as an immutable snapshot so that
 * [baseUrl] and [apiKey] are always read together consistently.
 */
private data class ServerConfig(val baseUrl: String = "", val apiKey: String = "")

class ExternalServerApi(
    private val client: HttpClient,
) {
    @Volatile
    private var config: ServerConfig = ServerConfig()

    fun configure(baseUrl: String, apiKey: String) {
        config = ServerConfig(baseUrl.trimEnd('/'), apiKey)
    }

    suspend fun getCapabilities(): CapabilitiesResponseDto {
        val cfg = config
        return client.get("${cfg.baseUrl}/capabilities") {
            if (cfg.apiKey.isNotBlank()) header("X-API-Key", cfg.apiKey)
        }.body()
    }

    suspend fun getImages(
        page: Int,
        perPage: Int,
        filters: Map<String, String>,
    ): PaginatedImagesResponseDto {
        val cfg = config
        val url = URLBuilder("${cfg.baseUrl}/images").apply {
            parameters.append("page", page.toString())
            parameters.append("per_page", perPage.toString())
            filters.forEach { (key, value) ->
                if (value.isNotBlank()) parameters.append(key, value)
            }
        }.buildString()
        return client.get(url) {
            if (cfg.apiKey.isNotBlank()) header("X-API-Key", cfg.apiKey)
        }.body()
    }

    suspend fun getGenerationOptions(): GenerationOptionsResponseDto {
        val cfg = config
        return client.get("${cfg.baseUrl}/generation/options") {
            if (cfg.apiKey.isNotBlank()) header("X-API-Key", cfg.apiKey)
        }.body()
    }

    suspend fun getDependentChoices(endpoint: String): List<GenerationChoiceDto> {
        val cfg = config
        // Absolute paths (starting with /) are resolved against the server origin,
        // not baseUrl, because servers may return full paths including their prefix.
        val url = when {
            endpoint.startsWith("http://") || endpoint.startsWith("https://") -> endpoint
            endpoint.startsWith("/") -> "${originOf(cfg.baseUrl)}$endpoint"
            else -> "${cfg.baseUrl}/$endpoint"
        }
        return client.get(url) {
            if (cfg.apiKey.isNotBlank()) header("X-API-Key", cfg.apiKey)
        }.body()
    }

    suspend fun executeGeneration(
        params: Map<String, String>,
    ): GenerationExecuteResponseDto {
        val cfg = config
        return client.post("${cfg.baseUrl}/generation/execute") {
            if (cfg.apiKey.isNotBlank()) header("X-API-Key", cfg.apiKey)
            contentType(ContentType.Application.Json)
            setBody(GenerationExecuteRequestDto(params))
        }.body()
    }

    suspend fun getGenerationStatus(jobId: String): GenerationStatusResponseDto {
        val cfg = config
        return client.get("${cfg.baseUrl}/generation/status") {
            if (cfg.apiKey.isNotBlank()) header("X-API-Key", cfg.apiKey)
            url { parameters.append("job_id", jobId) }
        }.body()
    }

    suspend fun deleteImage(cloudKey: String): DeleteResponseDto {
        val cfg = config
        return client.post("${cfg.baseUrl}/images/delete") {
            if (cfg.apiKey.isNotBlank()) header("X-API-Key", cfg.apiKey)
            contentType(ContentType.Application.Json)
            setBody(DeleteImageRequestDto(cloudKey))
        }.body()
    }

    suspend fun deleteImages(cloudKeys: List<String>): DeleteResponseDto {
        val cfg = config
        return client.post("${cfg.baseUrl}/images/delete-bulk") {
            if (cfg.apiKey.isNotBlank()) header("X-API-Key", cfg.apiKey)
            contentType(ContentType.Application.Json)
            setBody(BulkDeleteRequestDto(cloudKeys))
        }.body()
    }

    /**
     * Health check: tries GET /capabilities and returns true if reachable.
     */
    suspend fun testConnection(): Boolean = try {
        val cfg = config
        client.get("${cfg.baseUrl}/capabilities") {
            if (cfg.apiKey.isNotBlank()) header("X-API-Key", cfg.apiKey)
        }
        true
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Logger.w(TAG, "Connection test failed: ${e.message}")
        false
    }

    private companion object {
        const val TAG = "ExternalServerApi"

        /** Extract scheme://host[:port] from a URL, dropping any path. */
        fun originOf(url: String): String {
            val parsed = Url(url)
            val port = parsed.port
            val defaultPort = parsed.protocol.defaultPort
            return if (port == defaultPort) {
                "${parsed.protocol.name}://${parsed.host}"
            } else {
                "${parsed.protocol.name}://${parsed.host}:$port"
            }
        }
    }
}
