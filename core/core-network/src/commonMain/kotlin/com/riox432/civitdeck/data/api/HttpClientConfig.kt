package com.riox432.civitdeck.data.api

/**
 * Configuration for HTTP request timeouts.
 *
 * Each factory defines its own defaults matching the service's expected latency profile:
 * - CivitAI/HuggingFace/TensorArt: 15s connect, 30s request (fast public APIs)
 * - ComfyUI: 5s connect, 120s request (local server, long generation jobs)
 * - SDWebUI: 5s connect, 300s request (local server, very long generation jobs)
 * - ExternalServer: 5s connect, 60s request (user-configured server)
 */
data class TimeoutConfig(
    val connectTimeoutMs: Long = DEFAULT_CONNECT_TIMEOUT_MS,
    val requestTimeoutMs: Long = DEFAULT_REQUEST_TIMEOUT_MS,
    val socketTimeoutMs: Long = DEFAULT_SOCKET_TIMEOUT_MS,
) {
    companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MS = 15_000L
        const val DEFAULT_REQUEST_TIMEOUT_MS = 30_000L
        const val DEFAULT_SOCKET_TIMEOUT_MS = 30_000L

        /** CivitAI, HuggingFace, TensorArt — public APIs with moderate latency. */
        val PublicApi = TimeoutConfig()

        /** ComfyUI — local server with long-running generation jobs. */
        val ComfyUI = TimeoutConfig(
            connectTimeoutMs = 5_000L,
            requestTimeoutMs = 120_000L,
            socketTimeoutMs = 120_000L,
        )

        /** SDWebUI — local server with very long generation jobs. */
        val SDWebUI = TimeoutConfig(
            connectTimeoutMs = 5_000L,
            requestTimeoutMs = 300_000L,
            socketTimeoutMs = 300_000L,
        )

        /** External server — user-configured with moderate timeout. */
        val ExternalServer = TimeoutConfig(
            connectTimeoutMs = 5_000L,
            requestTimeoutMs = 60_000L,
            socketTimeoutMs = 60_000L,
        )
    }
}

/**
 * Configuration for HTTP request retry with exponential backoff.
 *
 * Used by both Ktor's [io.ktor.client.plugins.HttpRequestRetry] plugin
 * and the manual WebSocket reconnection logic in [ComfyUIWebSocketApi].
 */
data class RetryConfig(
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    val baseDelayMs: Long = DEFAULT_BASE_DELAY_MS,
    val maxDelayMs: Long = DEFAULT_MAX_DELAY_MS,
) {
    companion object {
        const val DEFAULT_MAX_RETRIES = 2
        const val DEFAULT_BASE_DELAY_MS = 1_000L
        const val DEFAULT_MAX_DELAY_MS = 16_000L

        /** Standard retry for public API clients (CivitAI, HuggingFace, TensorArt). */
        val Default = RetryConfig()

        /** WebSocket reconnection with more retries and capped backoff. */
        val WebSocket = RetryConfig(
            maxRetries = 5,
            baseDelayMs = 1_000L,
            maxDelayMs = 16_000L,
        )
    }
}
