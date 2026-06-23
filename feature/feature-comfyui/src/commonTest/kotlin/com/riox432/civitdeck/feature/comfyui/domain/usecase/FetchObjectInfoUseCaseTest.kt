package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.data.local.dao.CachedApiResponseDao
import com.riox432.civitdeck.data.local.entity.CachedApiResponseEntity
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.repository.ComfyUIGenerationRepository
import com.riox432.civitdeck.domain.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers [FetchObjectInfoUseCase] caching behavior: cache-hit short-circuit within
 * TTL, fetch-and-store on a miss, forceRefresh bypass, and stale-cache fallback when
 * the network call fails.
 */
class FetchObjectInfoUseCaseTest {

    private val cacheKey = "comfyui_object_info"

    @Test
    fun returns_cached_value_without_fetching_when_within_ttl() = runTest {
        // Fresh cache entry (cachedAt = now) -> served directly, repository untouched.
        val dao = FakeCacheDao()
        dao.entity = entity(json = "cached", cachedAt = now())
        val repo = FakeGenerationRepo(fresh = "fresh")
        val useCase = FetchObjectInfoUseCase(repo, LocalCacheDataSource(dao))

        val result = useCase()

        assertEquals("cached", result)
        assertFalse(repo.fetched)
    }

    @Test
    fun fetches_and_caches_when_no_cache_present() = runTest {
        val dao = FakeCacheDao()
        val repo = FakeGenerationRepo(fresh = "fresh")
        val useCase = FetchObjectInfoUseCase(repo, LocalCacheDataSource(dao))

        val result = useCase()

        assertEquals("fresh", result)
        assertTrue(repo.fetched)
        // The fresh response was written to cache.
        assertEquals("fresh", dao.entity?.responseJson)
    }

    @Test
    fun fetches_when_cache_is_stale_beyond_ttl() = runTest {
        // Entry older than the 30-minute TTL -> getCached returns null -> fetch.
        val staleAt = now() - (31L * 60L * 1000L)
        val dao = FakeCacheDao()
        dao.entity = entity(json = "stale", cachedAt = staleAt)
        val repo = FakeGenerationRepo(fresh = "fresh")
        val useCase = FetchObjectInfoUseCase(repo, LocalCacheDataSource(dao))

        val result = useCase()

        assertEquals("fresh", result)
        assertTrue(repo.fetched)
    }

    @Test
    fun forceRefresh_bypasses_fresh_cache() = runTest {
        val dao = FakeCacheDao()
        dao.entity = entity(json = "cached", cachedAt = now())
        val repo = FakeGenerationRepo(fresh = "fresh")
        val useCase = FetchObjectInfoUseCase(repo, LocalCacheDataSource(dao))

        val result = useCase(forceRefresh = true)

        assertEquals("fresh", result)
        assertTrue(repo.fetched)
    }

    @Test
    fun falls_back_to_stale_cache_when_fetch_fails() = runTest {
        // Stale entry + network error -> use case returns the stale cached JSON.
        val staleAt = now() - (31L * 60L * 1000L)
        val dao = FakeCacheDao()
        dao.entity = entity(json = "stale", cachedAt = staleAt)
        val repo = FakeGenerationRepo(fresh = null, failFetch = true)
        val useCase = FetchObjectInfoUseCase(repo, LocalCacheDataSource(dao))

        val result = useCase(forceRefresh = true)

        assertEquals("stale", result)
        assertTrue(repo.fetched)
    }

    @Test
    fun returns_null_when_fetch_fails_and_no_cache_exists() = runTest {
        val dao = FakeCacheDao()
        val repo = FakeGenerationRepo(fresh = null, failFetch = true)
        val useCase = FetchObjectInfoUseCase(repo, LocalCacheDataSource(dao))

        assertNull(useCase())
    }

    // --- helpers & fakes ---

    // Use the same wall clock LocalCacheDataSource reads so freshness math is consistent.
    private fun now(): Long = currentTimeMillis()

    private fun entity(json: String, cachedAt: Long) = CachedApiResponseEntity(
        cacheKey = cacheKey,
        responseJson = json,
        cachedAt = cachedAt,
    )

    /**
     * Single-entry in-memory DAO. Note [LocalCacheDataSource.getCached] uses the real
     * wall clock, so freshness is controlled via the entity's `cachedAt` relative to now().
     */
    private class FakeCacheDao : CachedApiResponseDao {
        var entity: CachedApiResponseEntity? = null
        override suspend fun getByKey(key: String): CachedApiResponseEntity? =
            entity?.takeIf { it.cacheKey == key }
        override suspend fun insert(entity: CachedApiResponseEntity) {
            this.entity = entity
        }
        override suspend fun deleteByKey(key: String): Int = 0
        override suspend fun deleteExpired(expiryTime: Long): Int = 0
        override suspend fun deleteAll(): Int = 0
        override suspend fun setPinned(key: String, pinned: Boolean): Int = 0
        override suspend fun getTotalCacheSizeBytes(): Long? = null
        override suspend fun getEntryCount(): Int = if (entity == null) 0 else 1
        override suspend fun deleteOldestUnpinned(count: Int): Int = 0
    }

    private class FakeGenerationRepo(
        private val fresh: String?,
        private val failFetch: Boolean = false,
    ) : ComfyUIGenerationRepository {
        var fetched = false
        override suspend fun fetchObjectInfo(): String {
            fetched = true
            if (failFetch) error("network down")
            return fresh ?: error("no fresh value configured")
        }
        override suspend fun fetchCheckpoints(): List<String> = emptyList()
        override suspend fun fetchLoras(): List<String> = emptyList()
        override suspend fun fetchControlNets(): List<String> = emptyList()
        override suspend fun submitGeneration(params: ComfyUIGenerationParams): String = ""
        override suspend fun pollGenerationResult(promptId: String): GenerationResult =
            error("not used")
        override fun observeGenerationProgress(
            promptId: String,
            host: String,
            port: Int,
        ): Flow<com.riox432.civitdeck.domain.model.GenerationProgress> = emptyFlow()
        override fun observeGenerationProgress(
            promptId: String,
            baseUrl: String,
            wsScheme: String,
        ): Flow<com.riox432.civitdeck.domain.model.GenerationProgress> = emptyFlow()
        override fun getImageUrl(filename: String, subfolder: String, type: String): String = ""
        override suspend fun interruptGeneration() = Unit
        override suspend fun uploadMaskImage(maskPngBytes: ByteArray): String = ""
    }
}
