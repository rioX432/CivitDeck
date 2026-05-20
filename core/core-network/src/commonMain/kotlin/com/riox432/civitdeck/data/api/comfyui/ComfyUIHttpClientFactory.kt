package com.riox432.civitdeck.data.api.comfyui

import com.riox432.civitdeck.data.api.TimeoutConfig
import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
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

fun createComfyUIHttpClient(
    timeoutConfig: TimeoutConfig = TimeoutConfig.ComfyUI,
): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                    coerceInputValues = true
                },
            )
        }
        install(HttpTimeout) {
            connectTimeoutMillis = timeoutConfig.connectTimeoutMs
            requestTimeoutMillis = timeoutConfig.requestTimeoutMs
            socketTimeoutMillis = timeoutConfig.socketTimeoutMs
        }
        install(Logging) {
            level = LogLevel.NONE
        }
        install(WebSockets)
        defaultRequest {
            header(HttpHeaders.Accept, "application/json")
        }
    }
}

private const val TAG = "ComfyUIHttpClientFactory"

/**
 * Creates an HttpClient with TLS trust configuration for self-signed certificates.
 * Platform-specific engines configure certificate trust via expect/actual.
 *
 * @param trustSelfSignedCerts When `true`, bypasses SSL certificate validation to support
 *   self-signed certificates commonly used by local ComfyUI servers. When `false`, uses
 *   standard SSL validation requiring valid CA-signed certificates. Defaults to `true`
 *   for backward compatibility with local network setups.
 * @param timeoutConfig Timeout configuration. Defaults to [TimeoutConfig.ComfyUI].
 */
fun createComfyUIHttpClientWithSelfSignedTls(
    trustSelfSignedCerts: Boolean = true,
    timeoutConfig: TimeoutConfig = TimeoutConfig.ComfyUI,
): HttpClient {
    if (trustSelfSignedCerts) {
        Logger.w(
            TAG,
            "SSL trust-all mode is active — certificate validation is bypassed. " +
                "This is intended for local ComfyUI servers with self-signed certificates only.",
        )
    }
    return createPlatformComfyUIHttpClient(trustSelfSignedCerts, timeoutConfig)
}

/**
 * Platform-specific HttpClient creation with configurable TLS trust.
 * When [trustSelfSignedCerts] is `true`, the platform engine skips certificate validation.
 * When `false`, standard SSL validation is used.
 */
expect fun createPlatformComfyUIHttpClient(
    trustSelfSignedCerts: Boolean,
    timeoutConfig: TimeoutConfig,
): HttpClient
