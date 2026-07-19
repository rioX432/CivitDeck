package com.riox432.civitdeck.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

/**
 * Proves the base-URL-injectable seam + recorded fixtures give a deterministic discovery
 * path (issue #990). A [MockEngine] serves [DiscoveryFixtures] for the injected E2E host,
 * so [CivitAiApi] never touches civitai.com and every run parses identically. This is the
 * unit-level backstop for the Maestro E2E flow, runnable in CI-equivalent test tasks.
 */
class CivitAiApiFixtureTest {

    private val e2eEndpoints = CivitAiEndpoints(
        apiBaseUrl = "http://127.0.0.1:8080/api/v1",
        trpcBaseUrl = "http://127.0.0.1:8080/api/trpc",
    )

    private fun fixtureClient(record: MutableList<String>): HttpClient {
        val engine = MockEngine { request ->
            record += request.url.encodedPath
            when (request.url.encodedPath) {
                "/api/v1/models" -> respond(
                    content = DiscoveryFixtures.modelsPage,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
                else -> error("Unexpected request to ${request.url}")
            }
        }
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true })
            }
        }
    }

    @Test
    fun getModelsParsesFixtureDeterministically() = runTest {
        val paths = mutableListOf<String>()
        val api = CivitAiApi(fixtureClient(paths), e2eEndpoints)

        val response = api.getModels(ModelListQuery(query = "anime", limit = 20))

        // The request hit the injected E2E host/path, not civitai.com.
        assertEquals(listOf("/api/v1/models"), paths)
        // Fixture parse is stable: fixed count, order, and identifiers.
        assertEquals(2, response.items.size)
        assertEquals(101L, response.items[0].id)
        assertEquals("Anime Diffusion XL", response.items[0].name)
        assertEquals(102L, response.items[1].id)
        assertEquals(2, response.metadata.totalItems)
    }

    @Test
    fun productionEndpointsRemainCivitAi() {
        // Guard the release path: the default must never drift from civitai.com.
        assertTrue(CivitAiEndpoints.Production.apiBaseUrl == "https://civitai.com/api/v1")
        assertTrue(CivitAiEndpoints.Production.trpcBaseUrl == "https://civitai.com/api/trpc")
    }
}
