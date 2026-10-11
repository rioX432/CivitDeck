package com.riox432.civitdeck.data.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createHttpClient(
    apiKeyProvider: ApiKeyProvider,
    timeoutConfig: TimeoutConfig = TimeoutConfig.PublicApi,
    retryConfig: RetryConfig = RetryConfig.Default,
): HttpClient {
    return HttpClient { configureProductionClient(apiKeyProvider, timeoutConfig, retryConfig) }
}

/** Engine injection only; shares the unchanged production configuration. */
fun createHttpClient(
    engine: HttpClientEngine,
    apiKeyProvider: ApiKeyProvider,
    timeoutConfig: TimeoutConfig = TimeoutConfig.PublicApi,
    retryConfig: RetryConfig = RetryConfig.Default,
): HttpClient = HttpClient(engine) { configureProductionClient(apiKeyProvider, timeoutConfig, retryConfig) }

private fun HttpClientConfig<*>.configureProductionClient(
    apiKeyProvider: ApiKeyProvider,
    timeoutConfig: TimeoutConfig,
    retryConfig: RetryConfig,
) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
                coerceInputValues = true
            }
        )
    }
    install(HttpTimeout) {
        connectTimeoutMillis = timeoutConfig.connectTimeoutMs
        requestTimeoutMillis = timeoutConfig.requestTimeoutMs
        socketTimeoutMillis = timeoutConfig.socketTimeoutMs
    }
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = retryConfig.maxRetries)
        retryOnException(maxRetries = retryConfig.maxRetries, retryOnTimeout = true)
        exponentialDelay(baseDelayMs = retryConfig.baseDelayMs)
    }
    install(Logging) {
        level = LogLevel.NONE
    }
    defaultRequest {
        header(HttpHeaders.Accept, "application/json")
    }
    install(
        createClientPlugin("CivitAiOriginAuth") {
            onRequest { request, _ ->
                // Url resolves the effective default port; URLBuilder may still hold 0.
                val origin = request.url.build()
                if (
                    origin.protocol == URLProtocol.HTTPS &&
                    origin.host.equals("civitai.com", ignoreCase = true) &&
                    origin.port == 443
                ) {
                    apiKeyProvider.apiKey?.let {
                        request.header(HttpHeaders.Authorization, "Bearer $it")
                    }
                }
            }
        }
    )
}
