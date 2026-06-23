package com.riox432.civitdeck.feature.creator.data.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers [CreatorRepositoryImpl]'s mapping of the CivitAI /creators REST
 * response (CreatorDto -> domain Creator, PaginationMetadataDto -> PageMetadata)
 * and the forwarding of query/page/limit parameters to [CivitAiApi].
 */
class CreatorRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** Builds a [CivitAiApi] that replies with [body] (JSON) and HTTP 200. */
    private fun apiReturning(body: String): CivitAiApi {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
        return CivitAiApi(client)
    }

    @Test
    fun getCreators_maps_items_and_metadata_to_domain() = runTest {
        val body = """
            {
              "items":[
                {"username":"alice","modelCount":12,"link":"https://civitai.com/u/alice"},
                {"username":"bob","modelCount":3,"link":null}
              ],
              "metadata":{"nextCursor":"abc","nextPage":"https://next"}
            }
        """.trimIndent()
        val repo = CreatorRepositoryImpl(apiReturning(body))

        val result = repo.getCreators(query = null, page = null, limit = null)

        assertEquals(2, result.items.size)
        val alice = result.items.first()
        assertEquals("alice", alice.username)
        assertEquals(12, alice.modelCount)
        assertEquals("https://civitai.com/u/alice", alice.link)
        assertNull(alice.image) // REST creators carry no image
        assertEquals("abc", result.metadata.nextCursor)
        assertEquals("https://next", result.metadata.nextPage)
    }

    @Test
    fun getCreators_applies_dto_defaults_for_absent_fields() = runTest {
        val body = """
            {"items":[{"username":"carol"}],"metadata":{}}
        """.trimIndent()
        val repo = CreatorRepositoryImpl(apiReturning(body))

        val result = repo.getCreators(query = "ca", page = 1, limit = 10)

        val carol = result.items.single()
        assertEquals("carol", carol.username)
        assertEquals(0, carol.modelCount) // DTO default
        assertNull(carol.link)
        assertNull(result.metadata.nextCursor)
        assertNull(result.metadata.nextPage)
    }

    @Test
    fun getCreators_returns_empty_items_when_response_empty() = runTest {
        val body = """{"items":[],"metadata":{}}"""
        val repo = CreatorRepositoryImpl(apiReturning(body))

        val result = repo.getCreators(query = null, page = null, limit = null)

        assertTrue(result.items.isEmpty())
    }

    @Test
    fun getCreators_forwards_query_page_and_limit_as_request_parameters() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel("""{"items":[],"metadata":{}}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = CivitAiApi(HttpClient(engine) { install(ContentNegotiation) { json(json) } })
        val repo = CreatorRepositoryImpl(api)

        repo.getCreators(query = "anime", page = 2, limit = 25)

        assertEquals(1, engine.requestHistory.size)
        val params = engine.requestHistory.first().url.parameters
        assertEquals("anime", params["query"])
        assertEquals("2", params["page"])
        assertEquals("25", params["limit"])
    }
}
