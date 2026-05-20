package com.riox432.civitdeck.data.api.externalserver

import com.riox432.civitdeck.data.api.TimeoutConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createExternalServerHttpClient(
    timeoutConfig: TimeoutConfig = TimeoutConfig.ExternalServer,
): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                },
            )
        }
        install(HttpTimeout) {
            connectTimeoutMillis = timeoutConfig.connectTimeoutMs
            requestTimeoutMillis = timeoutConfig.requestTimeoutMs
            socketTimeoutMillis = timeoutConfig.socketTimeoutMs
        }
        install(Logging) { level = LogLevel.NONE }
        defaultRequest { header(HttpHeaders.Accept, "application/json") }
    }
}
