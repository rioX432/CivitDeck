package com.riox432.civitdeck.data.api.huggingface

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
 * Covers [HuggingFaceRepositoryImpl.searchModels]: DTO -> domain mapping (name,
 * creator, stats, source), type inference from pipeline_tag / tags, and the
 * empty-result case.
 */
class HuggingFaceRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** Builds a [HuggingFaceApi] that replies with [body] (a JSON array) and HTTP 200. */
    private fun apiReturning(body: String): HuggingFaceApi {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return HuggingFaceApi(HttpClient(engine) { install(ContentNegotiation) { json(json) } })
    }

    @Test
    fun searchModels_maps_dto_fields_to_domain() = runTest {
        val body = """
            [
              {"modelId":"stabilityai/sdxl","author":"stabilityai","downloads":120,"likes":30,
               "tags":["diffusers"],"pipeline_tag":"text-to-image"}
            ]
        """.trimIndent()
        val repo = HuggingFaceRepositoryImpl(apiReturning(body))

        val models = repo.searchModels(query = "sdxl", limit = 20, offset = 0)

        assertEquals(1, models.size)
        val model = models.first()
        // name is the segment after the slash.
        assertEquals("sdxl", model.name)
        assertEquals(ModelType.Checkpoint, model.type) // from pipeline_tag text-to-image
        assertEquals(120, model.stats.downloadCount)
        assertEquals(30, model.stats.favoriteCount)
        assertEquals("stabilityai", model.creator?.username)
        assertEquals("https://huggingface.co/stabilityai", model.creator?.link)
        assertEquals(ModelSource.HUGGING_FACE, model.source)
    }

    @Test
    fun searchModels_infers_lora_type_from_tags() = runTest {
        val body = """
            [{"modelId":"user/some-lora","tags":["LoRA"],"downloads":1,"likes":0}]
        """.trimIndent()
        val repo = HuggingFaceRepositoryImpl(apiReturning(body))

        val models = repo.searchModels(query = null, limit = 20, offset = 0)

        assertEquals(ModelType.LORA, models.first().type)
    }

    @Test
    fun searchModels_returns_empty_list_for_empty_response() = runTest {
        val repo = HuggingFaceRepositoryImpl(apiReturning("[]"))

        val models = repo.searchModels(query = "none", limit = 20, offset = 0)

        assertTrue(models.isEmpty())
    }

    @Test
    fun searchModels_handles_missing_author_as_null_creator() = runTest {
        val body = """[{"modelId":"orphan-model","downloads":5,"likes":2}]"""
        val repo = HuggingFaceRepositoryImpl(apiReturning(body))

        val model = repo.searchModels(query = null, limit = 20, offset = 0).first()

        assertEquals("orphan-model", model.name)
        assertEquals(null, model.creator)
        // Falls back to the Checkpoint default when no pipeline tag / type tags match.
        assertEquals(ModelType.Checkpoint, model.type)
    }
}
