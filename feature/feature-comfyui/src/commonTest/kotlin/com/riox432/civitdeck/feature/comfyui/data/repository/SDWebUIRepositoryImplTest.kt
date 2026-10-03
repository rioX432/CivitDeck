package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.webui.SDWebUIApi
import com.riox432.civitdeck.data.local.entity.SDWebUIConnectionEntity
import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.domain.model.SDWebUIConnection
import com.riox432.civitdeck.domain.model.SDWebUIGenerationParams
import com.riox432.civitdeck.domain.model.SDWebUIGenerationProgress
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Covers [SDWebUIRepositoryImpl]: DAO-backed connection CRUD/mapping, asset fetch
 * mapping (models/samplers/vaes), the test-connection paths, the active-connection
 * guard, the txt2img generation flow happy/error branches, and the checkpoint override
 * sent in the generation request body.
 */
class SDWebUIRepositoryImplTest {

    private fun daoWithActive() = FakeSDWebUIConnectionDao().apply {
        rows.add(SDWebUIConnectionEntity(id = 1, name = "A", hostname = "h", port = 7860, isActive = true, createdAt = 1))
    }

    private fun api(body: String) = SDWebUIApi(mockClient { okJson(body) })

    @Test
    fun saveConnection_inserts_and_activates_first() = runTest {
        val dao = FakeSDWebUIConnectionDao()
        val repo = SDWebUIRepositoryImpl(dao, api("[]"))

        val id = repo.saveConnection(SDWebUIConnection(id = 0, name = "Home", hostname = "1.2.3.4"))

        assertEquals(1L, id)
        assertTrue(dao.rows.first().isActive)
    }

    @Test
    fun observeActiveConnection_maps_to_domain() = runTest {
        val dao = daoWithActive()
        val repo = SDWebUIRepositoryImpl(dao, api("[]"))

        val active = repo.observeActiveConnection().first()

        assertEquals(1L, active?.id)
        assertEquals("http://h:7860", active?.baseUrl)
    }

    @Test
    fun fetchModels_maps_titles() = runTest {
        val body = """[{"title":"model A","model_name":"a"},{"title":"model B","model_name":"b"}]"""
        val repo = SDWebUIRepositoryImpl(daoWithActive(), api(body))

        val models = repo.fetchModels()

        assertEquals(listOf("model A", "model B"), models)
    }

    @Test
    fun fetchSamplers_maps_names() = runTest {
        val repo = SDWebUIRepositoryImpl(daoWithActive(), api("""[{"name":"Euler"},{"name":"DPM++"}]"""))

        assertEquals(listOf("Euler", "DPM++"), repo.fetchSamplers())
    }

    @Test
    fun fetchVaes_maps_model_names() = runTest {
        val repo = SDWebUIRepositoryImpl(daoWithActive(), api("""[{"model_name":"vae-ft-mse"}]"""))

        assertEquals(listOf("vae-ft-mse"), repo.fetchVaes())
    }

    @Test
    fun fetchModels_throws_ConnectionException_without_active_connection() = runTest {
        val repo = SDWebUIRepositoryImpl(FakeSDWebUIConnectionDao(), api("[]"))

        assertFailsWith<DomainException.ConnectionException> { repo.fetchModels() }
    }

    @Test
    fun testConnection_returns_true_when_samplers_succeed() = runTest {
        val repo = SDWebUIRepositoryImpl(FakeSDWebUIConnectionDao(), api("""[{"name":"Euler"}]"""))

        assertTrue(repo.testConnection(SDWebUIConnection(name = "n", hostname = "h")))
    }

    @Test
    fun testConnection_returns_false_on_error() = runTest {
        val errorApi = SDWebUIApi(mockClient { respondError(HttpStatusCode.InternalServerError) })
        val repo = SDWebUIRepositoryImpl(FakeSDWebUIConnectionDao(), errorApi)

        assertFalse(repo.testConnection(SDWebUIConnection(name = "n", hostname = "h")))
    }

    @Test
    fun generateImage_emits_completed_with_images() = runTest {
        val client = mockClient { req ->
            when {
                req.url.encodedPath.endsWith("/txt2img") -> okJson("""{"images":["base64data"]}""")
                else -> okJson("""{"progress":0.0,"state":{"sampling_step":0,"sampling_steps":0}}""")
            }
        }
        val repo = SDWebUIRepositoryImpl(daoWithActive(), SDWebUIApi(client))

        val emissions = repo.generateImage(SDWebUIGenerationParams(prompt = "cat")).toList()

        val completed = emissions.last()
        assertIs<SDWebUIGenerationProgress.Completed>(completed)
        assertEquals(listOf("base64data"), completed.base64Images)
    }

    @Test
    fun generateImage_emits_error_when_generation_fails() = runTest {
        val client = mockClient { req ->
            when {
                req.url.encodedPath.endsWith("/txt2img") -> respondError(HttpStatusCode.InternalServerError)
                else -> okJson("""{"progress":0.0,"state":{"sampling_step":0,"sampling_steps":0}}""")
            }
        }
        val repo = SDWebUIRepositoryImpl(daoWithActive(), SDWebUIApi(client))

        val emissions = repo.generateImage(SDWebUIGenerationParams(prompt = "cat")).toList()

        assertIs<SDWebUIGenerationProgress.Error>(emissions.last())
    }

    @Test
    fun generateImage_sends_selected_checkpoint_as_override_settings() = runTest {
        val params = SDWebUIGenerationParams(prompt = "cat", checkpoint = "model A [abc123]")
        val body = capturedGenerationBody("/txt2img", params)

        val overrides = assertNotNull(body["override_settings"]).jsonObject
        assertEquals("model A [abc123]", overrides["sd_model_checkpoint"]?.jsonPrimitive?.content)
    }

    @Test
    fun generateImage_sends_empty_override_settings_for_blank_checkpoint() = runTest {
        val body = capturedGenerationBody("/txt2img", SDWebUIGenerationParams(prompt = "cat"))

        assertEquals(JsonObject(emptyMap()), body["override_settings"])
    }

    @Test
    fun generateImage_img2img_sends_selected_checkpoint_as_override_settings() = runTest {
        val params = SDWebUIGenerationParams(prompt = "cat", initImageBase64 = "img", checkpoint = "model B")
        val body = capturedGenerationBody("/img2img", params)

        val overrides = assertNotNull(body["override_settings"]).jsonObject
        assertEquals("model B", overrides["sd_model_checkpoint"]?.jsonPrimitive?.content)
    }

    private suspend fun capturedGenerationBody(path: String, params: SDWebUIGenerationParams): JsonObject {
        var body: JsonObject? = null
        // encodeDefaults mirrors createSDWebUIHttpClient, so an empty override map is still sent.
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        val client = HttpClient(
            MockEngine { req ->
                if (req.url.encodedPath.endsWith(path)) {
                    body = json.decodeFromString(req.body.toByteArray().decodeToString())
                    okJson("""{"images":["base64data"]}""")
                } else {
                    okJson("""{"progress":0.0,"state":{"sampling_step":0,"sampling_steps":0}}""")
                }
            },
        ) {
            install(ContentNegotiation) { json(json) }
        }
        val repo = SDWebUIRepositoryImpl(daoWithActive(), SDWebUIApi(client))

        assertIs<SDWebUIGenerationProgress.Completed>(repo.generateImage(params).toList().last())
        return assertNotNull(body)
    }
}
