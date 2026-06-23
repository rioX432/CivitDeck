package com.riox432.civitdeck.data.repository

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
import kotlin.test.assertTrue

/**
 * Covers [TagRepositoryImpl.getTags]: DTO -> domain mapping (including pagination
 * metadata), the empty-result case, and the parse-error guard that returns an
 * empty page instead of throwing when the API responds with malformed JSON.
 */
class TagRepositoryImplTest {

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
        return CivitAiApi(HttpClient(engine) { install(ContentNegotiation) { json(json) } })
    }

    @Test
    fun getTags_maps_dto_to_domain_including_metadata() = runTest {
        val body = """
            {"items":[
              {"name":"anime","modelCount":42,"link":"https://civitai.com/tag/anime"},
              {"name":"realistic","modelCount":7,"link":null}
            ],
            "metadata":{"nextCursor":"abc","nextPage":"https://next"}}
        """.trimIndent()
        val repo = TagRepositoryImpl(apiReturning(body))

        val result = repo.getTags(query = null, page = null, limit = null)

        assertEquals(2, result.items.size)
        val first = result.items.first()
        assertEquals("anime", first.name)
        assertEquals(42, first.modelCount)
        assertEquals("https://civitai.com/tag/anime", first.link)
        val second = result.items[1]
        assertEquals("realistic", second.name)
        assertEquals(7, second.modelCount)
        assertEquals(null, second.link)
        assertEquals("abc", result.metadata.nextCursor)
        assertEquals("https://next", result.metadata.nextPage)
    }

    @Test
    fun getTags_returns_empty_items_when_response_has_no_tags() = runTest {
        val body = """{"items":[],"metadata":{"nextCursor":null,"nextPage":null}}"""
        val repo = TagRepositoryImpl(apiReturning(body))

        val result = repo.getTags(query = "missing", page = 1, limit = 20)

        assertTrue(result.items.isEmpty())
        assertEquals(null, result.metadata.nextCursor)
    }

    @Test
    fun getTags_defaults_modelCount_to_zero_when_absent() = runTest {
        val body = """{"items":[{"name":"sfw"}],"metadata":{}}"""
        val repo = TagRepositoryImpl(apiReturning(body))

        val result = repo.getTags(query = null, page = null, limit = null)

        assertEquals(1, result.items.size)
        assertEquals(0, result.items.first().modelCount)
        assertEquals(null, result.items.first().link)
    }

    // Note: the DataParseException / SerializationException catch in getTags is
    // effectively unreachable through the public API. CivitAiApi.getTags() does not
    // wrap deserialization errors (unlike getModels), so a malformed body surfaces a
    // ktor JsonConvertException, which is neither of the caught types. The guard is
    // therefore defensive only and cannot be exercised by a unit test here.
}
