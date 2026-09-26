package com.riox432.civitdeck.data.api.download

import com.riox432.civitdeck.data.api.ApiKeyProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

private const val CONNECT_TIMEOUT_MS = 15_000L

/**
 * HTTP client for streaming model file downloads.
 *
 * Model files can be many gigabytes, so unlike the other API clients this one disables the
 * request/socket timeout (a fixed timeout would abort any download slower than that window)
 * and skips [io.ktor.client.plugins.HttpRequestRetry] — a retry would restart the whole
 * transfer, and download queue retry is already a manual, user-triggered action.
 */
fun createModelDownloadHttpClient(apiKeyProvider: ApiKeyProvider): HttpClient {
    return HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
        }
        install(Logging) { level = LogLevel.NONE }
        defaultRequest {
            apiKeyProvider.apiKey?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }
}
