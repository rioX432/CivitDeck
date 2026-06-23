package com.riox432.civitdeck.data.api.tensorart

import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.ModelType
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
 * Covers [TensorArtRepositoryImpl.searchModels]: DTO -> domain mapping (numeric id
 * parsing, type mapping, stats, source), the synthetic cover-image version, and the
 * null/empty data fallbacks.
 */
class TensorArtRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** Builds a [TensorArtApi] that replies with [body] (JSON) and HTTP 200. */
    private fun apiReturning(body: String): TensorArtApi {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return TensorArtApi(HttpClient(engine) { install(ContentNegotiation) { json(json) } })
    }

    @Test
    fun searchModels_maps_dto_fields_to_domain() = runTest {
        val body = """
            {"data":{"models":[
              {"id":"42","name":"My LoRA","type":"LORA",
               "author":{"name":"creator1","avatar":"a.png"},
               "stats":{"downloadCount":10,"likeCount":5,"runCount":3},
               "coverImage":"https://img/cover.png","tags":["anime"],"baseModel":"SDXL"}
            ]},"total":1}
        """.trimIndent()
        val repo = TensorArtRepositoryImpl(apiReturning(body))

        val models = repo.searchModels(query = "lora", page = 1, pageSize = 20)

        assertEquals(1, models.size)
        val model = models.first()
        assertEquals(42L, model.id) // numeric string parsed directly
        assertEquals("My LoRA", model.name)
        assertEquals(ModelType.LORA, model.type)
        assertEquals(listOf("anime"), model.tags)
        assertEquals("creator1", model.creator?.username)
        assertEquals(10, model.stats.downloadCount)
        assertEquals(5, model.stats.favoriteCount) // likeCount -> favoriteCount
        assertEquals(ModelSource.TENSOR_ART, model.source)
        // Cover image becomes a single synthetic version.
        assertEquals(1, model.modelVersions.size)
        assertEquals("https://img/cover.png", model.modelVersions.first().images.first().url)
        assertEquals("SDXL", model.modelVersions.first().baseModel)
    }

    @Test
    fun searchModels_returns_empty_when_data_is_null() = runTest {
        val repo = TensorArtRepositoryImpl(apiReturning("""{"total":0}"""))

        val models = repo.searchModels(query = "none", page = 1, pageSize = 20)

        assertTrue(models.isEmpty())
    }

    @Test
    fun searchModels_returns_empty_when_models_list_empty() = runTest {
        val repo = TensorArtRepositoryImpl(apiReturning("""{"data":{"models":[]},"total":0}"""))

        val models = repo.searchModels(query = "none", page = 1, pageSize = 20)

        assertTrue(models.isEmpty())
    }

    @Test
    fun searchModels_maps_unknown_type_to_other_and_no_cover_to_empty_versions() = runTest {
        val body = """
            {"data":{"models":[
              {"id":"7","name":"Mystery","type":"SOMETHING_ODD"}
            ]},"total":1}
        """.trimIndent()
        val repo = TensorArtRepositoryImpl(apiReturning(body))

        val model = repo.searchModels(query = "", page = 1, pageSize = 20).first()

        assertEquals(ModelType.Other, model.type)
        assertTrue(model.modelVersions.isEmpty()) // no coverImage -> no synthetic version
        assertEquals(0, model.stats.downloadCount) // null stats -> default
    }

    @Test
    fun searchModels_generates_stable_id_for_non_numeric_id() = runTest {
        val body = """{"data":{"models":[{"id":"abc-uuid","name":"X","type":"LORA"}]},"total":1}"""
        val repo = TensorArtRepositoryImpl(apiReturning(body))

        val model = repo.searchModels(query = "", page = 1, pageSize = 20).first()

        // Non-numeric ids are prefixed with 3_000_000_000 to avoid CivitAI collisions.
        assertTrue(model.id >= 3_000_000_000L)
    }
}
