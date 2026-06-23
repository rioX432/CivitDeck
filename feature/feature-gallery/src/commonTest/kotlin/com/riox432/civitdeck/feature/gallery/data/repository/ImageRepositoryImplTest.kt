package com.riox432.civitdeck.feature.gallery.data.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.data.local.dao.CachedApiResponseDao
import com.riox432.civitdeck.data.local.entity.CachedApiResponseEntity
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
import kotlin.test.assertTrue

/**
 * Covers [ImageRepositoryImpl]'s network-success path (caches the raw JSON, maps to
 * domain) and its offline fallback that decodes the previously cached response when
 * the network call throws, plus the re-throw when no cache exists.
 */
class ImageRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** In-memory [CachedApiResponseDao] honouring the TTL window read by [LocalCacheDataSource]. */
    private class FakeCacheDao : CachedApiResponseDao {
        val store = mutableMapOf<String, CachedApiResponseEntity>()

        override suspend fun getByKey(key: String): CachedApiResponseEntity? = store[key]
        override suspend fun insert(entity: CachedApiResponseEntity) { store[entity.cacheKey] = entity }
        override suspend fun deleteByKey(key: String): Int =
            if (store.remove(key) != null) 1 else 0
        override suspend fun deleteExpired(expiryTime: Long): Int {
            val before = store.size
            store.entries.removeAll { it.value.cachedAt < expiryTime && !it.value.isOfflinePinned }
            return before - store.size
        }
        override suspend fun deleteAll(): Int {
            val n = store.size
            store.clear()
            return n
        }
        override suspend fun setPinned(key: String, pinned: Boolean): Int = 0
        override suspend fun getTotalCacheSizeBytes(): Long? =
            store.values.sumOf { it.responseJson.length.toLong() }
        override suspend fun getEntryCount(): Int = store.size
        override suspend fun deleteOldestUnpinned(count: Int): Int = 0
    }

    private val imagesBody = """
        {"items":[
          {"id":1,"url":"http://img/1.png","width":512,"height":768,"nsfw":false,
           "username":"alice","meta":{"prompt":"1girl","seed":7}}
        ],"metadata":{"nextCursor":"cursor-2","nextPage":null}}
    """.trimIndent()

    /** [CivitAiApi] whose mock engine replies [body] with HTTP 200. */
    private fun apiReturning(body: String): CivitAiApi {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(json) } }
        return CivitAiApi(client)
    }

    /** [CivitAiApi] whose mock engine always throws, simulating an offline failure. */
    private fun apiThrowing(): CivitAiApi {
        val engine = MockEngine { throw IllegalStateException("network down") }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(json) } }
        return CivitAiApi(client)
    }

    @Test
    fun getImages_success_maps_domain_and_caches_response() = runTest {
        val cacheDao = FakeCacheDao()
        val repo = ImageRepositoryImpl(apiReturning(imagesBody), LocalCacheDataSource(cacheDao), json)

        val result = repo.getImages(
            modelId = 5L, modelVersionId = null, username = "alice",
            sort = null, period = null, nsfwLevel = null, limit = 20, cursor = null,
        )

        assertEquals(1, result.items.size)
        assertEquals(1L, result.items[0].id)
        assertEquals("alice", result.items[0].username)
        assertEquals("1girl", result.items[0].meta?.prompt)
        assertEquals("cursor-2", result.metadata.nextCursor)
        // Raw response is persisted under a key derived from the request params.
        assertEquals(1, cacheDao.store.size)
    }

    @Test
    fun getImages_falls_back_to_cache_when_network_fails() = runTest {
        val cacheDao = FakeCacheDao()
        // Prime the cache via a successful call, then fail the network on the identical request.
        val seededRepo =
            ImageRepositoryImpl(apiReturning(imagesBody), LocalCacheDataSource(cacheDao), json)
        seededRepo.getImages(
            modelId = 5L, modelVersionId = null, username = "alice",
            sort = null, period = null, nsfwLevel = null, limit = 20, cursor = null,
        )

        val offlineRepo =
            ImageRepositoryImpl(apiThrowing(), LocalCacheDataSource(cacheDao), json)
        val result = offlineRepo.getImages(
            modelId = 5L, modelVersionId = null, username = "alice",
            sort = null, period = null, nsfwLevel = null, limit = 20, cursor = null,
        )

        assertEquals(1, result.items.size)
        assertEquals(1L, result.items[0].id)
        assertEquals("cursor-2", result.metadata.nextCursor)
    }

    @Test
    fun getImages_rethrows_when_network_fails_and_no_cache() = runTest {
        val repo = ImageRepositoryImpl(apiThrowing(), LocalCacheDataSource(FakeCacheDao()), json)

        assertFailsWith<IllegalStateException> {
            repo.getImages(
                modelId = 1L, modelVersionId = null, username = null,
                sort = null, period = null, nsfwLevel = null, limit = 10, cursor = null,
            )
        }
    }

    @Test
    fun getImages_empty_items_returns_empty_result() = runTest {
        val body = """{"items":[],"metadata":{"nextCursor":null,"nextPage":null}}"""
        val repo = ImageRepositoryImpl(apiReturning(body), LocalCacheDataSource(FakeCacheDao()), json)

        val result = repo.getImages(
            modelId = null, modelVersionId = null, username = null,
            sort = null, period = null, nsfwLevel = null, limit = null, cursor = null,
        )

        assertTrue(result.items.isEmpty())
    }
}
