package com.riox432.civitdeck.data.api.comfyui

import com.riox432.civitdeck.data.api.TimeoutConfig
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
import kotlin.concurrent.Volatile

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

/** How a ComfyUI HTTP client decides whether to trust the server's TLS certificate. */
sealed interface ComfyUIServerTrust {
    /** The platform's default certificate validation. */
    data object System : ComfyUIServerTrust

    /**
     * Trust only a server whose leaf certificate has the SHA-256 fingerprint [expectedSha256]
     * (64 lowercase hex characters over the DER bytes). A null pin rejects every server, which is
     * how the fingerprint of a not-yet-confirmed certificate is captured. Hostname and validity
     * dates are not checked on Android or iOS because the pin is the server's identity.
     *
     * Not a data class: [presentedSha256] is per-client state, so two instances with the same pin
     * must not be treated as interchangeable.
     */
    class PinnedLeaf(val expectedSha256: String?) : ComfyUIServerTrust {
        /**
         * Fingerprint of the leaf certificate the server presented in the latest handshake of a
         * client built with this trust, whether it was accepted or rejected. Read it after a
         * failed request to report which certificate the server sent; the engine's exception
         * does not carry it in a portable way.
         */
        @Volatile
        var presentedSha256: String? = null
            internal set
    }
}

/** Creates a ComfyUI HttpClient whose TLS validation follows [trust]. */
expect fun createComfyUIHttpClient(
    trust: ComfyUIServerTrust,
    timeoutConfig: TimeoutConfig = TimeoutConfig.ComfyUI,
): HttpClient
