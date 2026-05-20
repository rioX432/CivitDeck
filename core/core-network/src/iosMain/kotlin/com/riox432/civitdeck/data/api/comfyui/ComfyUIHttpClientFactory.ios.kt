package com.riox432.civitdeck.data.api.comfyui

import com.riox432.civitdeck.data.api.TimeoutConfig
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

/**
 * Creates a Darwin-backed Ktor client with configurable TLS trust.
 * On iOS, full self-signed TLS bypass requires NSURLSessionDelegate configuration
 * which is complex to implement via K/N cinterop. This returns a standard Darwin client
 * regardless of [trustSelfSignedCerts] — users should add the self-signed cert to the iOS
 * trust store via Settings > General > About > Certificate Trust Settings, or use a tunnel
 * (Cloudflare/Tailscale).
 */
actual fun createPlatformComfyUIHttpClient(
    trustSelfSignedCerts: Boolean,
    timeoutConfig: TimeoutConfig,
): HttpClient {
    return HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                },
            )
        }
        install(HttpTimeout) {
            connectTimeoutMillis = timeoutConfig.connectTimeoutMs
            requestTimeoutMillis = timeoutConfig.requestTimeoutMs
            socketTimeoutMillis = timeoutConfig.socketTimeoutMs
        }
        install(Logging) { level = LogLevel.NONE }
        install(WebSockets)
        defaultRequest { header(HttpHeaders.Accept, "application/json") }
    }
}
