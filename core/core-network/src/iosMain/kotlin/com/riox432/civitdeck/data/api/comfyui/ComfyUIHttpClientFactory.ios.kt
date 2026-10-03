package com.riox432.civitdeck.data.api.comfyui

import com.riox432.civitdeck.data.api.TimeoutConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
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
 * Creates a Darwin-backed Ktor client that always uses system trust and ignores
 * [trustSelfSignedCerts]. A self-signed server is reached through [createComfyUIHttpClient] with
 * [ComfyUIServerTrust.PinnedLeaf] instead.
 */
actual fun createPlatformComfyUIHttpClient(
    trustSelfSignedCerts: Boolean,
    timeoutConfig: TimeoutConfig,
): HttpClient {
    return HttpClient(Darwin) { installComfyUIPlugins(timeoutConfig) }
}

/**
 * Ktor's Darwin delegate passes every task's challenges, WebSocket tasks included, to
 * `handleChallenge`, so the pin covers both HTTP and `wss://` traffic.
 */
actual fun createComfyUIHttpClient(
    trust: ComfyUIServerTrust,
    timeoutConfig: TimeoutConfig,
): HttpClient {
    return HttpClient(Darwin) {
        if (trust is ComfyUIServerTrust.PinnedLeaf) {
            val evaluator = ComfyUIServerTrustEvaluator(trust)
            engine {
                handleChallenge { _, _, challenge, completionHandler ->
                    val decision = evaluator.evaluate(challenge)
                    completionHandler(decision.disposition, decision.credential)
                }
            }
        }
        installComfyUIPlugins(timeoutConfig)
    }
}

private fun HttpClientConfig<*>.installComfyUIPlugins(timeoutConfig: TimeoutConfig) {
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
