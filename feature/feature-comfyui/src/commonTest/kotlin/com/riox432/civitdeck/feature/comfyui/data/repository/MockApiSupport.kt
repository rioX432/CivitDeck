package com.riox432.civitdeck.feature.comfyui.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.json.Json

/** Lenient JSON used by all ComfyUI repository tests. */
internal val testJson = Json { ignoreUnknownKeys = true }

/** JSON content-type headers for mock responses. */
internal val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

/**
 * Builds an [HttpClient] backed by a [MockEngine] running [handler] with
 * ContentNegotiation installed so `.body()` deserialization works.
 */
internal fun mockClient(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData,
): HttpClient = HttpClient(MockEngine(handler)) {
    install(ContentNegotiation) { json(testJson) }
}

/** Convenience: responds 200 with [body] as JSON for every request. */
internal fun MockRequestHandleScope.okJson(body: String) =
    respond(ByteReadChannel(body), HttpStatusCode.OK, jsonHeaders)
