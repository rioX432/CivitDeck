package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.domain.model.DomainException
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Covers [ReviewRepositoryImpl]'s DTO -> domain mapping (including nested
 * user/thread defaults) and the auth guard on [ReviewRepositoryImpl.submitReview].
 */
class ReviewRepositoryImplTest {

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

    private fun provider(key: String?): ApiKeyProvider =
        ApiKeyProvider().apply { apiKey = key }

    @Test
    fun getReviews_maps_dto_fields_to_domain_including_nested_user_and_thread() = runTest {
        val body = """
            {"result":{"data":{"json":{
              "items":[
                {"id":11,"modelId":5,"modelVersionId":7,"rating":4,"recommended":true,
                 "details":"Great","createdAt":"2024-01-01",
                 "user":{"id":1,"username":"alice","image":"img.png"},
                 "thread":{"commentCount":3}}
              ],
              "nextCursor":42
            }}}}
        """.trimIndent()
        val repo = ReviewRepositoryImpl(apiReturning(body), provider(null))

        val page = repo.getReviews(modelId = 5L, modelVersionId = 7L, limit = 20, cursor = null)

        assertEquals(1, page.items.size)
        val review = page.items.first()
        assertEquals(11L, review.id)
        assertEquals(5L, review.modelId)
        assertEquals(7L, review.modelVersionId)
        assertEquals(4, review.rating)
        assertEquals(true, review.recommended)
        assertEquals("Great", review.details)
        assertEquals("alice", review.username)
        assertEquals("img.png", review.userImage)
        assertEquals(3, review.commentCount)
        assertEquals(42, page.nextCursor)
    }

    @Test
    fun getReviews_defaults_comment_count_to_zero_when_user_and_thread_absent() = runTest {
        val body = """
            {"result":{"data":{"json":{
              "items":[{"id":1,"modelId":2,"modelVersionId":3,"rating":5,"recommended":false,"createdAt":"x"}],
              "nextCursor":null
            }}}}
        """.trimIndent()
        val repo = ReviewRepositoryImpl(apiReturning(body), provider(null))

        val page = repo.getReviews(modelId = 2L, modelVersionId = null, limit = 10, cursor = null)

        val review = page.items.first()
        assertNull(review.username)
        assertNull(review.userImage)
        assertEquals(0, review.commentCount)
        assertNull(page.nextCursor)
    }

    @Test
    fun getReviews_returns_empty_page_when_no_items() = runTest {
        val body = """{"result":{"data":{"json":{"items":[],"nextCursor":null}}}}"""
        val repo = ReviewRepositoryImpl(apiReturning(body), provider(null))

        val page = repo.getReviews(modelId = 1L, modelVersionId = null, limit = 20, cursor = null)

        assertEquals(0, page.items.size)
        assertNull(page.nextCursor)
    }

    @Test
    fun getRatingTotals_maps_star_buckets_and_thumbs() = runTest {
        val body = """
            {"result":{"data":{"json":{"1":10,"2":20,"3":30,"4":40,"5":50,"up":100,"down":5}}}}
        """.trimIndent()
        val repo = ReviewRepositoryImpl(apiReturning(body), provider(null))

        val totals = repo.getRatingTotals(modelId = 1L, modelVersionId = null)

        assertEquals(10, totals.star1)
        assertEquals(20, totals.star2)
        assertEquals(30, totals.star3)
        assertEquals(40, totals.star4)
        assertEquals(50, totals.star5)
        assertEquals(100, totals.thumbsUp)
        assertEquals(5, totals.thumbsDown)
    }

    @Test
    fun submitReview_throws_AuthException_when_api_key_missing() = runTest {
        // API should never be hit; a throwing engine confirms the guard fires first.
        val engine = MockEngine { throw IllegalStateException("network must not be called") }
        val api = CivitAiApi(HttpClient(engine) { install(ContentNegotiation) { json(json) } })
        val repo = ReviewRepositoryImpl(api, provider(null))

        assertFailsWith<DomainException.AuthException> {
            repo.submitReview(
                modelId = 1L,
                modelVersionId = 2L,
                rating = 5,
                recommended = true,
                details = null,
            )
        }
    }

    @Test
    fun submitReview_calls_api_when_api_key_present() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel("""{"result":{"data":{"json":{}}}}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = CivitAiApi(HttpClient(engine) { install(ContentNegotiation) { json(json) } })
        val repo = ReviewRepositoryImpl(api, provider("valid-key"))

        // Should not throw.
        repo.submitReview(
            modelId = 1L,
            modelVersionId = 2L,
            rating = 4,
            recommended = false,
            details = "ok",
        )

        assertEquals(1, engine.requestHistory.size)
        val authHeader = engine.requestHistory.first().headers[HttpHeaders.Authorization]
        assertEquals("Bearer valid-key", authHeader)
    }
}
