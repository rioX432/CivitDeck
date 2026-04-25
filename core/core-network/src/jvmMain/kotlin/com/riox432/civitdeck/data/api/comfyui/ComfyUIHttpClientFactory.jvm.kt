package com.riox432.civitdeck.data.api.comfyui

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
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
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

private const val CONNECT_TIMEOUT_MS = 5_000L
private const val REQUEST_TIMEOUT_MS = 120_000L
private const val SOCKET_TIMEOUT_MS = 120_000L

/**
 * Creates a CIO-backed Ktor client that trusts all certificates.
 * WARNING: Only for self-signed cert scenarios.
 */
@Suppress("EmptyFunctionBlock", "TrustAllX509TrustManager", "CustomX509TrustManager")
actual fun createComfyUIHttpClientWithSelfSignedTls(): HttpClient {
    val trustAllManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            // Intentionally empty — trust all client certificates
        }
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            // Intentionally empty — trust all server certificates for self-signed setups
        }
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
    return HttpClient(CIO) {
        engine {
            https {
                trustManager = trustAllManager
            }
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true })
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
