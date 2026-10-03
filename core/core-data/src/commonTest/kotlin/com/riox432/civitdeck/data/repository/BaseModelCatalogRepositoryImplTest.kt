package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.CachedApiResponseDao
import com.riox432.civitdeck.data.local.entity.CachedApiResponseEntity
import com.riox432.civitdeck.di.coreDataModule
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.BaseModelCatalog
import com.riox432.civitdeck.domain.model.BaseModelStatus
import com.riox432.civitdeck.domain.repository.BaseModelCatalogRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BaseModelCatalogRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** In-memory [CachedApiResponseDao] that records pinning. */
    private class FakeCacheDao : CachedApiResponseDao {
        val store = mutableMapOf<String, CachedApiResponseEntity>()

        override suspend fun getByKey(key: String): CachedApiResponseEntity? = store[key]
        override suspend fun insert(entity: CachedApiResponseEntity) {
            store[entity.cacheKey] = entity
        }
        override suspend fun deleteByKey(key: String): Int = if (store.remove(key) != null) 1 else 0
        override suspend fun deleteExpired(expiryTime: Long): Int = 0
        override suspend fun deleteAll(): Int = store.size.also { store.clear() }
        override suspend fun setPinned(key: String, pinned: Boolean): Int {
            val entity = store[key] ?: return 0
            store[key] = entity.copy(isOfflinePinned = pinned)
            return 1
        }
        override suspend fun getTotalCacheSizeBytes(): Long? = null
        override suspend fun getEntryCount(): Int = store.size
        override suspend fun deleteOldestUnpinned(count: Int): Int = 0
    }

    private class Fixture(val repository: BaseModelCatalogRepositoryImpl, val requestedPaths: List<String>)

    private fun fixture(dao: CachedApiResponseDao = FakeCacheDao(), handler: MockRequestHandler): Fixture {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            paths += request.url.encodedPath
            handler(this, request)
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(json) } }
        return Fixture(BaseModelCatalogRepositoryImpl(CivitAiApi(client), LocalCacheDataSource(dao), json), paths)
    }

    private fun offlineFixture(dao: CachedApiResponseDao = FakeCacheDao()) =
        fixture(dao) { throw IOException("offline") }

    private fun enumsFixture(dao: CachedApiResponseDao = FakeCacheDao(), body: String = LIVE_SHAPED_ENUMS) =
        fixture(dao) { respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json")) }

    private fun cachedEnums(cachedAt: Long, active: List<String>, all: List<String>) = CachedApiResponseEntity(
        cacheKey = CACHE_KEY,
        responseJson = """{"ActiveBaseModel":${active.toJsonArray()},"BaseModel":${all.toJsonArray()}}""",
        cachedAt = cachedAt,
        isOfflinePinned = true,
    )

    private fun List<String>.toJsonArray() = joinToString(",", "[", "]") { "\"$it\"" }

    private fun BaseModelCatalog.values() = active.map { it.apiValue } to retired.map { it.apiValue }

    private suspend fun bundledCatalog() = offlineFixture().repository.observeCatalog().first()

    @Test
    fun fresh_fetch_replaces_the_bundled_list_and_is_cached_pinned() = runTest {
        val dao = FakeCacheDao()
        val fixture = enumsFixture(dao)

        val emissions = fixture.repository.observeCatalog().toList()

        assertEquals(listOf("/api/v1/enums"), fixture.requestedPaths)
        assertEquals(listOf(bundledCatalog(), emissions.last()), emissions)
        assertEquals(listOf("Anima", "Krea 2", "SDXL 1.0") to listOf("SVD", "Wan Video"), emissions.last().values())
        val cached = dao.store.getValue(CACHE_KEY)
        assertTrue(cached.isOfflinePinned)
        assertEquals(emissions.last(), fixture(dao) { error("no request expected") }.repository.observeCatalog().first())
    }

    @Test
    fun fresh_cache_is_served_without_a_request() = runTest {
        val dao = FakeCacheDao()
        dao.store[CACHE_KEY] = cachedEnums(currentTimeMillis(), listOf("Krea 2"), listOf("Krea 2", "SVD"))
        val fixture = fixture(dao) { error("no request expected") }

        val emissions = fixture.repository.observeCatalog().toList()

        assertEquals(emptyList(), fixture.requestedPaths)
        assertEquals(listOf(listOf("Krea 2") to listOf("SVD")), emissions.map { it.values() })
    }

    @Test
    fun cache_older_than_24h_is_refetched() = runTest {
        val dao = FakeCacheDao()
        val olderThanTtl = currentTimeMillis() - 25L * 60L * 60L * 1000L
        dao.store[CACHE_KEY] = cachedEnums(olderThanTtl, listOf("Krea 2"), listOf("Krea 2"))
        val fixture = enumsFixture(dao)

        val emissions = fixture.repository.observeCatalog().toList()

        assertEquals(listOf("/api/v1/enums"), fixture.requestedPaths)
        assertEquals(listOf(listOf("Krea 2") to emptyList()), emissions.take(1).map { it.values() })
        assertEquals(listOf("Anima", "Krea 2", "SDXL 1.0"), emissions.last().values().first)
        assertTrue(dao.store.getValue(CACHE_KEY).cachedAt > olderThanTtl)
    }

    @Test
    fun stale_cache_is_kept_when_the_fetch_fails() = runTest {
        val dao = FakeCacheDao()
        dao.store[CACHE_KEY] = cachedEnums(0L, listOf("Krea 2"), listOf("Krea 2", "SVD"))
        val fixture = offlineFixture(dao)

        val emissions = fixture.repository.observeCatalog().toList()

        assertEquals(listOf("/api/v1/enums"), fixture.requestedPaths)
        assertEquals(listOf(listOf("Krea 2") to listOf("SVD")), emissions.map { it.values() })
        assertEquals(0L, dao.store.getValue(CACHE_KEY).cachedAt)
    }

    @Test
    fun bundled_snapshot_is_used_when_nothing_is_cached_and_the_fetch_fails() = runTest {
        val dao = FakeCacheDao()
        val fixture = offlineFixture(dao)

        val emissions = fixture.repository.observeCatalog().toList()

        assertEquals(listOf("/api/v1/enums"), fixture.requestedPaths)
        assertEquals(1, emissions.size)
        assertEquals(78, emissions.single().active.size)
        assertTrue(dao.store.isEmpty())
    }

    @Test
    fun a_response_without_active_values_is_not_cached() = runTest {
        val dao = FakeCacheDao()
        val fixture = enumsFixture(dao, body = """{"ActiveBaseModel":["Foo, Bar"],"BaseModel":["SVD"]}""")

        val emissions = fixture.repository.observeCatalog().toList()

        assertEquals(listOf(bundledCatalog()), emissions)
        assertTrue(dao.store.isEmpty())
    }

    @Test
    fun live_values_containing_a_comma_are_dropped() = runTest {
        val body = """{"ActiveBaseModel":["Krea 2","Foo, Bar"],"BaseModel":["Krea 2","Foo, Bar","Old,Model","SVD"]}"""
        val fixture = enumsFixture(body = body)

        val catalog = fixture.repository.observeCatalog().toList().last()

        assertEquals(listOf("Krea 2") to listOf("SVD"), catalog.values())
    }

    @Test
    fun concurrent_observers_share_one_request() = runTest {
        val requestStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val fixture = fixture { _ ->
            requestStarted.complete(Unit)
            release.await()
            respond(LIVE_SHAPED_ENUMS, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }

        val first = async { fixture.repository.observeCatalog().toList() }
        val second = async { fixture.repository.observeCatalog().toList() }
        requestStarted.await()
        runCurrent()
        release.complete(Unit)

        assertEquals(first.await().last(), second.await().last())
        assertEquals(listOf("/api/v1/enums"), fixture.requestedPaths)
    }

    @Test
    fun bundled_catalog_matches_the_2026_10_03_enums_response() = runTest {
        val catalog = bundledCatalog()

        assertEquals(78, catalog.active.size)
        assertEquals(34, catalog.retired.size)
        assertEquals(BaseModel("Anima"), catalog.active.first())
        assertEquals(BaseModel("Sonilo"), catalog.active.last())
        assertEquals(BaseModel("Ideogram 4.5"), catalog.retired.first())
        assertEquals(BaseModel("Trellis.2"), catalog.retired.last())
        assertTrue(catalog.active.intersect(catalog.retired.toSet()).isEmpty())
    }

    @Test
    fun statusOf_classifies_active_retired_and_unknown_values() = runTest {
        val catalog = bundledCatalog()

        assertEquals(BaseModelStatus.Active, catalog.statusOf(BaseModel("Krea 2")))
        assertEquals(BaseModelStatus.Active, catalog.statusOf(BaseModel("SDXL 1.0")))
        assertEquals(BaseModelStatus.Retired, catalog.statusOf(BaseModel("SVD")))
        assertEquals(BaseModelStatus.Retired, catalog.statusOf(BaseModel("Wan Video")))
        assertEquals(BaseModelStatus.Unknown, catalog.statusOf(BaseModel("NotAModel")))
        // CivitAI matches base models case-sensitively, so a differently cased value is not in the catalog.
        assertEquals(BaseModelStatus.Unknown, catalog.statusOf(BaseModel("sdxl 1.0")))
    }

    @Test
    fun buildBaseModelCatalog_drops_values_containing_a_comma() {
        val catalog = buildBaseModelCatalog(
            activeValues = listOf("Krea 2", "Foo, Bar", "Anima"),
            allValues = listOf("Krea 2", "Foo, Bar", "Anima", "SVD", "Old,Model", "Wan Video"),
        )

        assertEquals(listOf("Krea 2", "Anima"), catalog.active.map { it.apiValue })
        assertEquals(listOf("SVD", "Wan Video"), catalog.retired.map { it.apiValue })
        assertEquals(BaseModelStatus.Unknown, catalog.statusOf(BaseModel("Foo, Bar")))
        assertEquals(BaseModelStatus.Unknown, catalog.statusOf(BaseModel("Old,Model")))
    }

    @Test
    fun buildBaseModelCatalog_keeps_an_active_value_missing_from_the_full_list_as_active() {
        val catalog = buildBaseModelCatalog(
            activeValues = listOf("New Model", "Krea 2"),
            allValues = listOf("Krea 2", "SVD"),
        )

        assertEquals(listOf("New Model", "Krea 2"), catalog.active.map { it.apiValue })
        assertEquals(listOf("SVD"), catalog.retired.map { it.apiValue })
    }

    @Test
    fun coreDataModule_provides_the_repository() {
        val dependencies = module {
            single { CivitAiApi(HttpClient(MockEngine { error("no request expected") })) }
            single { LocalCacheDataSource(FakeCacheDao()) }
            single { json }
        }
        val koin = koinApplication { modules(dependencies, coreDataModule) }.koin

        assertIs<BaseModelCatalogRepositoryImpl>(koin.get<BaseModelCatalogRepository>())
    }

    private companion object {
        const val CACHE_KEY = "civitai:enums"

        // Same shape as the live GET https://civitai.com/api/v1/enums response checked on
        // 2026-10-03: five string arrays, of which only the two base model arrays are read.
        val LIVE_SHAPED_ENUMS = """
            {
              "ModelType": ["Checkpoint", "TextualInversion", "LORA"],
              "ModelFileType": ["Model", "Text Encoder", "Vision Encoder"],
              "ActiveBaseModel": ["Anima", "Krea 2", "SDXL 1.0"],
              "BaseModel": ["Anima", "Krea 2", "SDXL 1.0", "SVD", "Wan Video"],
              "BaseModelType": ["Standard", "Inpainting", "Refiner", "Pix2Pix"]
            }
        """.trimIndent()
    }
}
