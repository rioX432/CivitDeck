package com.riox432.civitdeck.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class CivitAiImagesApiTest {

    private val endpoints = CivitAiEndpoints(
        apiBaseUrl = "http://127.0.0.1:8080/api/v1",
        trpcBaseUrl = "http://127.0.0.1:8080/api/trpc",
    )

    private fun imagesClient(body: String, requests: MutableList<Url> = mutableListOf()): HttpClient {
        val engine = MockEngine { request ->
            requests += request.url
            respond(
                content = body,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true })
            }
        }
    }

    private fun imagesPage(meta: String): String = """
        {
          "items": [{ "id": 1, "url": "https://image.civitai.com/1.jpeg", "meta": $meta }],
          "metadata": { "nextCursor": null }
        }
    """.trimIndent()

    @Test
    fun getImagesAlwaysRequestsGenerationMeta() = runTest {
        val requests = mutableListOf<Url>()
        val api = CivitAiApi(imagesClient(imagesPage("null"), requests), endpoints)

        api.getImages(modelId = 42L, limit = 20)

        assertEquals(1, requests.size)
        assertEquals("/api/v1/images", requests.single().encodedPath)
        assertEquals("true", requests.single().parameters["withMeta"])
    }

    @Test
    fun metaWithComfyGraphKeepsPromptAndDropsComfyFromAdditionalParams() = runTest {
        val comfyGraph = "{\\\"prompt\\\":{\\\"3\\\":{\\\"class_type\\\":\\\"KSampler\\\"}}}" +
            "x".repeat(COMFY_PADDING)
        val meta = """
            {
              "prompt": "a lighthouse at dusk",
              "seed": 1234,
              "Clip skip": "2",
              "comfy": "$comfyGraph",
              "resources": [{ "name": "base", "type": "model" }]
            }
        """.trimIndent()
        val api = CivitAiApi(imagesClient(imagesPage(meta)), endpoints)

        val decoded = api.getImages().items.single().meta

        assertEquals("a lighthouse at dusk", decoded?.prompt)
        assertEquals(1234L, decoded?.seed)
        assertEquals("2", decoded?.additionalParams?.get("Clip skip"))
        assertFalse(decoded?.additionalParams.orEmpty().containsKey("comfy"))
    }

    @Test
    fun nonPrimitivePromptDecodesAsNullInsteadOfThrowing() = runTest {
        val meta = """{ "prompt": { "text": "nested" }, "steps": [30], "sampler": "Euler a" }"""
        val api = CivitAiApi(imagesClient(imagesPage(meta)), endpoints)

        val decoded = api.getImages().items.single().meta

        assertNull(decoded?.prompt)
        assertNull(decoded?.steps)
        assertEquals("Euler a", decoded?.sampler)
    }

    private companion object {
        // Live /images responses carry ComfyUI graphs of ~25 KB in `meta.comfy`.
        const val COMFY_PADDING = 25_000
    }
}
