package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.webui.SDWebUIApi
import com.riox432.civitdeck.data.local.entity.SDWebUIConnectionEntity
import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.domain.model.SDWebUIConnection
import com.riox432.civitdeck.domain.model.SDWebUIGenerationParams
import com.riox432.civitdeck.domain.model.SDWebUIGenerationProgress
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Covers [SDWebUIRepositoryImpl]: DAO-backed connection CRUD/mapping, asset fetch
 * mapping (models/samplers/vaes), the test-connection paths, the active-connection
 * guard, and the txt2img generation flow happy/error branches.
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
}
