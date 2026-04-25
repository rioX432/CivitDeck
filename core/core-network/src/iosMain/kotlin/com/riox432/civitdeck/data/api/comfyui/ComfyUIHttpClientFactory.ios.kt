package com.riox432.civitdeck.data.api.comfyui

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val CONNECT_TIMEOUT_MS = 5_000L
private const val REQUEST_TIMEOUT_MS = 120_000L
private const val SOCKET_TIMEOUT_MS = 120_000L

/**
 * Creates a Darwin-backed Ktor client for self-signed certificate scenarios.
 * On iOS, full self-signed TLS bypass requires NSURLSessionDelegate configuration
 * which is complex to implement via K/N cinterop. This returns a standard client
 * for now — users should add the self-signed cert to the iOS trust store via
 * Settings > General > About > Certificate Trust Settings, or use a tunnel (Cloudflare/Tailscale).
 */
actual fun createComfyUIHttpClientWithSelfSignedTls(): HttpClient {
    return HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                }
            )
        }
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
        install(Logging) { level = LogLevel.NONE }
        install(WebSockets)
        defaultRequest { header(HttpHeaders.Accept, "application/json") }
    }
}
