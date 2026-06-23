package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.api.comfyui.ComfyUIWebSocketApi
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.domain.model.GenerationStatus
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Covers [ComfyUIRepositoryImpl]'s non-WebSocket surface: connection CRUD/mapping,
 * asset fetch (checkpoints/loras), prompt submission, history polling, image URL
 * building, and queue polling (which rethrows on error unlike the queue-only repo).
 * WebSocket progress streaming is not exercised here.
 */
class ComfyUIRepositoryImplTest {

    // WS API is never invoked by the methods under test; wrap an inert mock client.
    private fun wsApi() = ComfyUIWebSocketApi(mockClient { okJson("{}") }, testJson)

    private fun daoWithActive() = FakeComfyUIConnectionDao().apply {
        rows.add(ComfyUIConnectionEntity(id = 1, name = "A", hostname = "h", port = 8188, isActive = true, createdAt = 1))
    }

    private fun repo(
        dao: FakeComfyUIConnectionDao = daoWithActive(),
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ): ComfyUIRepositoryImpl {
        val api = ComfyUIApi(mockClient(handler), testJson)
        return ComfyUIRepositoryImpl(dao, api, wsApi(), testJson)
    }

    @Test
    fun saveConnection_inserts_and_activates_first() = runTest {
        val dao = FakeComfyUIConnectionDao()
        val r = ComfyUIRepositoryImpl(dao, ComfyUIApi(mockClient { okJson("{}") }, testJson), wsApi(), testJson)

        val id = r.saveConnection(ComfyUIConnection(id = 0, name = "Home", hostname = "1.2.3.4"))

        assertEquals(1L, id)
        assertTrue(dao.rows.first().isActive)
    }

    @Test
    fun observeConnections_maps_entities_to_domain() = runTest {
        val r = repo { okJson("{}") }

        val connections = r.observeConnections().first()

        assertEquals(1, connections.size)
        assertEquals("http://h:8188", connections.first().baseUrl)
    }

    @Test
    fun fetchCheckpoints_parses_object_info_list() = runTest {
        val body = """{"CheckpointLoaderSimple":{"input":{"required":{"ckpt_name":[["a.safetensors","b.ckpt"]]}}}}"""
        val r = repo { okJson(body) }

        assertEquals(listOf("a.safetensors", "b.ckpt"), r.fetchCheckpoints())
    }

    @Test
    fun fetchLoras_parses_object_info_list() = runTest {
        val body = """{"LoraLoader":{"input":{"required":{"lora_name":[["lora1.safetensors"]]}}}}"""
        val r = repo { okJson(body) }

        assertEquals(listOf("lora1.safetensors"), r.fetchLoras())
    }

    @Test
    fun submitGeneration_posts_prompt_and_returns_prompt_id() = runTest {
        var promptPath = false
        val r = repo { req ->
            if (req.url.encodedPath == "/prompt") promptPath = true
            okJson("""{"prompt_id":"gen-1"}""")
        }

        val id = r.submitGeneration(ComfyUIGenerationParams(checkpoint = "m.safetensors", prompt = "cat"))

        assertEquals("gen-1", id)
        assertTrue(promptPath)
    }

    @Test
    fun submitGeneration_throws_without_active_connection() = runTest {
        val r = repo(dao = FakeComfyUIConnectionDao()) { okJson("""{"prompt_id":"x"}""") }

        assertFailsWith<DomainException.ConnectionException> {
            r.submitGeneration(ComfyUIGenerationParams(checkpoint = "m", prompt = "p"))
        }
    }

    @Test
    fun pollGenerationResult_completed_with_images() = runTest {
        val body = """
            {"p1":{"status":{"completed":true},"outputs":{"9":{"images":[{"filename":"o.png","type":"output"}]}}}}
        """.trimIndent()
        val r = repo { okJson(body) }

        val result = r.pollGenerationResult("p1")

        assertEquals(GenerationStatus.Completed, result.status)
        assertEquals(1, result.imageUrls.size)
        assertTrue(result.imageUrls.first().contains("filename=o.png"))
    }

    @Test
    fun pollGenerationResult_running_when_history_absent() = runTest {
        val r = repo { okJson("{}") }

        val result = r.pollGenerationResult("missing")

        assertEquals(GenerationStatus.Running, result.status)
    }

    @Test
    fun pollGenerationResult_error_when_completed_without_images() = runTest {
        val body = """{"p1":{"status":{"completed":true},"outputs":{}}}"""
        val r = repo { okJson(body) }

        val result = r.pollGenerationResult("p1")

        assertEquals(GenerationStatus.Error, result.status)
    }

    @Test
    fun getImageUrl_builds_view_url_with_active_base() = runTest {
        // getImageUrl uses the API's current base URL; configure it via a prior call.
        val r = repo { okJson("{}") }
        r.fetchObjectInfo() // sets base URL to http://h:8188

        val url = r.getImageUrl("img.png", subfolder = "sub", type = "output")

        assertTrue(url.startsWith("http://h:8188/view"))
        assertTrue(url.contains("filename=img.png"))
        assertTrue(url.contains("subfolder=sub"))
    }

    @Test
    fun observeQueue_rethrows_on_api_error() = runTest {
        val r = repo { respondError(HttpStatusCode.InternalServerError) }

        assertFailsWith<Exception> { r.observeQueue(intervalMs = 1000).first() }
    }
}
