package com.riox432.civitdeck.feature.externalserver.data.repository

import com.riox432.civitdeck.data.api.externalserver.ExternalServerApi
import com.riox432.civitdeck.data.local.dao.ExternalServerConfigDao
import com.riox432.civitdeck.data.local.entity.ExternalServerConfigEntity
import com.riox432.civitdeck.feature.externalserver.domain.model.ExternalServerImageFilters
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationJobStatus
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationOptionType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Covers [ExternalServerImagesRepositoryImpl]'s DTO -> domain mapping over a mocked
 * [ExternalServerApi], the active-config gate in `ensureApiConfigured`, and that the
 * active server's baseUrl/apiKey are applied to outgoing requests.
 */
class ExternalServerImagesRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** Minimal DAO that only needs [getActive] for these tests. */
    private class FakeConfigDao(
        private val active: ExternalServerConfigEntity?,
    ) : ExternalServerConfigDao {
        override fun observeAll(): Flow<List<ExternalServerConfigEntity>> = flowOf(emptyList())
        override fun observeActive(): Flow<ExternalServerConfigEntity?> = flowOf(active)
        override suspend fun getActive(): ExternalServerConfigEntity? = active
        override suspend fun getById(id: Long): ExternalServerConfigEntity? = null
        override suspend fun getAll(): List<ExternalServerConfigEntity> = emptyList()
        override suspend fun insert(entity: ExternalServerConfigEntity): Long = 0L
        override suspend fun insertAll(entities: List<ExternalServerConfigEntity>) {}
        override suspend fun update(entity: ExternalServerConfigEntity): Int = 0
        override suspend fun deactivateAll(): Int = 0
        override suspend fun activate(id: Long): Int = 0
        override suspend fun updateTestResult(id: Long, testedAt: Long, success: Boolean): Int = 0
        override suspend fun deleteById(id: Long): Int = 0
        override suspend fun deleteAll(): Int = 0
    }

    private fun activeConfig(
        baseUrl: String = "http://server.local:9000/",
        apiKey: String = "test-key",
    ) = ExternalServerConfigEntity(
        id = 1L,
        name = "Active",
        baseUrl = baseUrl,
        apiKey = apiKey,
        isActive = true,
        createdAt = 1L,
    )

    /** Builds an [ExternalServerApi] backed by a [MockEngine] that always replies [body]. */
    private fun apiReturning(body: String): Pair<ExternalServerApi, MockEngine> {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(json) } }
        return ExternalServerApi(client) to engine
    }

    @Test
    fun getCapabilities_maps_dto_to_domain() = runTest {
        val (api, _) = apiReturning(
            """{"endpoints":["images","generation"],"version":"1.2","name":"MyServer"}""",
        )
        val repo = ExternalServerImagesRepositoryImpl(api, FakeConfigDao(activeConfig()))

        val caps = repo.getCapabilities()

        assertEquals(listOf("images", "generation"), caps.endpoints)
        assertEquals("MyServer", caps.name)
        assertEquals("1.2", caps.version)
        assertTrue(caps.supports("images"))
    }

    @Test
    fun ensureApiConfigured_throws_when_no_active_config() = runTest {
        val (api, _) = apiReturning("{}")
        val repo = ExternalServerImagesRepositoryImpl(api, FakeConfigDao(active = null))

        val error = assertFailsWith<IllegalStateException> { repo.getCapabilities() }
        assertEquals("No active external server config", error.message)
    }

    @Test
    fun getCapabilities_applies_active_baseUrl_and_apiKey_to_request() = runTest {
        val (api, engine) = apiReturning("""{"endpoints":[],"version":"","name":""}""")
        val repo = ExternalServerImagesRepositoryImpl(
            api,
            FakeConfigDao(activeConfig(baseUrl = "http://host:7000/", apiKey = "abc123")),
        )

        repo.getCapabilities()

        val request = engine.requestHistory.single()
        // Trailing slash on baseUrl is trimmed before building the request URL.
        assertEquals("http://host:7000/capabilities", request.url.toString())
        assertEquals("abc123", request.headers["X-API-Key"])
    }

    @Test
    fun getImages_maps_paginated_dto_to_domain() = runTest {
        val body = """
            {"images":[
              {"id":1,"cloud_key":"k1","file":"a.png","nsfw":true,"seed":42,"prompt":"cat"}
            ],"total":1,"page":2,"per_page":10,"total_pages":1}
        """.trimIndent()
        val (api, _) = apiReturning(body)
        val repo = ExternalServerImagesRepositoryImpl(api, FakeConfigDao(activeConfig()))

        val result = repo.getImages(page = 2, perPage = 10, filters = ExternalServerImageFilters())

        assertEquals(1, result.images.size)
        assertEquals(1, result.images[0].id)
        assertEquals("k1", result.images[0].cloudKey)
        assertEquals(true, result.images[0].nsfw)
        assertEquals(42L, result.images[0].seed)
        assertEquals(2, result.page)
        assertEquals(10, result.perPage)
    }

    @Test
    fun getImages_returns_empty_list_when_no_images() = runTest {
        val (api, _) = apiReturning("""{"images":[],"total":0,"page":1,"per_page":96,"total_pages":0}""")
        val repo = ExternalServerImagesRepositoryImpl(api, FakeConfigDao(activeConfig()))

        val result = repo.getImages(page = 1, perPage = 96, filters = ExternalServerImageFilters())

        assertTrue(result.images.isEmpty())
        assertEquals(0, result.total)
    }

    @Test
    fun getGenerationOptions_maps_type_and_default_value() = runTest {
        val body = """
            {"options":[
              {"key":"model","label":"Model","type":"select","default":"sdxl",
               "choices":[{"value":"sdxl","label":"SDXL"}]}
            ]}
        """.trimIndent()
        val (api, _) = apiReturning(body)
        val repo = ExternalServerImagesRepositoryImpl(api, FakeConfigDao(activeConfig()))

        val options = repo.getGenerationOptions()

        assertEquals(1, options.size)
        assertEquals("model", options[0].key)
        assertEquals(GenerationOptionType.SELECT, options[0].type)
        assertEquals("sdxl", options[0].defaultValue)
        assertEquals(1, options[0].choices.size)
        assertEquals("SDXL", options[0].choices[0].label)
    }

    @Test
    fun getDependentChoices_maps_list_to_domain() = runTest {
        val (api, _) = apiReturning("""[{"value":"v1","label":"L1","description":"d1"}]""")
        val repo = ExternalServerImagesRepositoryImpl(api, FakeConfigDao(activeConfig()))

        val choices = repo.getDependentChoices("/options/costume")

        assertEquals(1, choices.size)
        assertEquals("v1", choices[0].value)
        assertEquals("d1", choices[0].description)
    }

    @Test
    fun executeGeneration_maps_status_string_to_enum() = runTest {
        val (api, _) = apiReturning("""{"job_id":"job-1","status":"queued","message":"ok"}""")
        val repo = ExternalServerImagesRepositoryImpl(api, FakeConfigDao(activeConfig()))

        val job = repo.executeGeneration(mapOf("model" to "sdxl"))

        assertEquals("job-1", job.jobId)
        assertEquals(GenerationJobStatus.QUEUED, job.status)
        assertEquals("ok", job.message)
    }

    @Test
    fun getGenerationStatus_maps_progress_fields() = runTest {
        val body = """{"job_id":"job-2","status":"running","progress":0.5,"completed":2,"total":4}"""
        val (api, _) = apiReturning(body)
        val repo = ExternalServerImagesRepositoryImpl(api, FakeConfigDao(activeConfig()))

        val job = repo.getGenerationStatus("job-2")

        assertEquals(GenerationJobStatus.RUNNING, job.status)
        assertEquals(0.5f, job.progress)
        assertEquals(2, job.completed)
        assertEquals(4, job.total)
    }

    @Test
    fun testConnection_returns_true_on_reachable_server() = runTest {
        val (api, _) = apiReturning("""{"endpoints":[],"version":"","name":""}""")
        val repo = ExternalServerImagesRepositoryImpl(api, FakeConfigDao(activeConfig()))

        assertTrue(repo.testConnection())
    }
}
