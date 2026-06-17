package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.data.api.dto.ModelListResponse
import com.riox432.civitdeck.data.api.dto.ModelResponse
import com.riox432.civitdeck.data.api.dto.PaginationMetadataDto
import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.data.local.dao.CachedApiResponseDao
import com.riox432.civitdeck.data.local.entity.CachedApiResponseEntity
import com.riox432.civitdeck.domain.model.ModelSearchQuery
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Covers [ModelRepositoryImpl]'s offline cache fallback: when the network call
 * fails, it should serve previously cached data (including TTL-expired data),
 * and throw only when nothing is cached.
 */
class ModelRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** In-memory [CachedApiResponseDao]. */
    private class FakeCacheDao : CachedApiResponseDao {
        val store = mutableMapOf<String, CachedApiResponseEntity>()

        override suspend fun getByKey(key: String): CachedApiResponseEntity? = store[key]
        override suspend fun insert(entity: CachedApiResponseEntity) {
            store[entity.cacheKey] = entity
        }
        override suspend fun deleteByKey(key: String): Int =
            if (store.remove(key) != null) 1 else 0
        override suspend fun deleteExpired(expiryTime: Long): Int = 0
        override suspend fun deleteAll(): Int {
            val n = store.size
            store.clear()
            return n
        }
        override suspend fun setPinned(key: String, pinned: Boolean): Int = 0
        override suspend fun getTotalCacheSizeBytes(): Long? = null
        override suspend fun getEntryCount(): Int = store.size
        override suspend fun deleteOldestUnpinned(count: Int): Int = 0
    }

    /** Builds a [CivitAiApi] whose network calls always throw (simulated offline). */
    private fun offlineApi(): CivitAiApi {
        val engine = MockEngine { throw IOException("offline") }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
        return CivitAiApi(client)
    }

    private fun cachedModelsJson(id: Long, name: String): String {
        val response = ModelListResponse(
            items = listOf(ModelResponse(id = id, name = name, type = "Checkpoint")),
            metadata = PaginationMetadataDto(nextCursor = "next"),
        )
        return json.encodeToString(ModelListResponse.serializer(), response)
    }

    @Test
    fun getModels_falls_back_to_cache_when_network_fails() = runTest {
        val dao = FakeCacheDao()
        // Cache key for an empty query is simply "models".
        dao.store["models"] = CachedApiResponseEntity(
            cacheKey = "models",
            responseJson = cachedModelsJson(id = 42L, name = "Cached Model"),
            cachedAt = 0L, // stale, but offline fallback ignores TTL
            isOfflinePinned = false,
        )
        val repo = ModelRepositoryImpl(offlineApi(), LocalCacheDataSource(dao), json)

        val result = repo.getModels(ModelSearchQuery())

        assertEquals(1, result.items.size)
        assertEquals(42L, result.items[0].id)
        assertEquals("Cached Model", result.items[0].name)
        assertEquals("next", result.metadata.nextCursor)
    }

    @Test
    fun getModels_rethrows_when_no_cache_available() = runTest {
        val repo = ModelRepositoryImpl(offlineApi(), LocalCacheDataSource(FakeCacheDao()), json)
        assertFailsWith<IOException> { repo.getModels(ModelSearchQuery()) }
    }

    @Test
    fun getModel_falls_back_to_cache_when_network_fails() = runTest {
        val dao = FakeCacheDao()
        val modelJson = json.encodeToString(
            ModelResponse.serializer(),
            ModelResponse(id = 7L, name = "Detail Cached", type = "LORA"),
        )
        dao.store["model:7"] = CachedApiResponseEntity(
            cacheKey = "model:7",
            responseJson = modelJson,
            cachedAt = 0L,
            isOfflinePinned = false,
        )
        val repo = ModelRepositoryImpl(offlineApi(), LocalCacheDataSource(dao), json)

        val model = repo.getModel(7L)

        assertEquals(7L, model.id)
        assertEquals("Detail Cached", model.name)
    }
}
