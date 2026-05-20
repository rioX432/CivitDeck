package com.riox432.civitdeck.data.api.comfyui

import com.riox432.civitdeck.data.api.TimeoutConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
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
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Creates an OkHttp-backed Ktor client with configurable TLS trust.
 * When [trustSelfSignedCerts] is `true`, bypasses certificate validation for self-signed setups.
 * When `false`, uses the platform default trust manager (standard CA validation).
 */
@Suppress("EmptyFunctionBlock", "TrustAllX509TrustManager", "CustomX509TrustManager")
actual fun createPlatformComfyUIHttpClient(
    trustSelfSignedCerts: Boolean,
    timeoutConfig: TimeoutConfig,
): HttpClient {
    return HttpClient(OkHttp) {
        engine {
            if (trustSelfSignedCerts) {
                val trustAllManager = object : X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?,
                    ) {
                        // Intentionally empty — trust all client certificates
                    }
                    override fun checkServerTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?,
                    ) {
                        // Intentionally empty — trust all server certificates for self-signed setups
                    }
                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                }
                val sslContext = SSLContext.getInstance("TLS").apply {
                    init(null, arrayOf<TrustManager>(trustAllManager), SecureRandom())
                }
                config {
                    sslSocketFactory(sslContext.socketFactory, trustAllManager)
                    hostnameVerifier { _, _ -> true }
                }
            }
        }
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
