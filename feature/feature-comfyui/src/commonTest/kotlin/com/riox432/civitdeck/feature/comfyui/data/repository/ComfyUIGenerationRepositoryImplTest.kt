package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.api.comfyui.ComfyUIWebSocketApi
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.domain.model.GenerationStatus
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Covers [ComfyUIGenerationRepositoryImpl]'s non-WebSocket surface: asset fetch,
 * prompt submission, history-based result polling, mask upload, object-info fetch,
 * image URL building, and the active-connection guard. WebSocket progress is excluded.
 */
class ComfyUIGenerationRepositoryImplTest {

    private fun wsApi() = ComfyUIWebSocketApi(mockClient { okJson("{}") }, testJson)

    private fun daoWithActive() = FakeComfyUIConnectionDao().apply {
        rows.add(ComfyUIConnectionEntity(id = 1, name = "A", hostname = "h", port = 8188, isActive = true, createdAt = 1))
    }

    private fun repo(
        dao: FakeComfyUIConnectionDao = daoWithActive(),
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ): ComfyUIGenerationRepositoryImpl {
        val api = ComfyUIApi(mockClient(handler), testJson)
        return ComfyUIGenerationRepositoryImpl(dao, api, wsApi(), testJson)
    }

    @Test
    fun fetchControlNets_parses_object_info_list() = runTest {
        val body = """{"ControlNetLoader":{"input":{"required":{"control_net_name":[["canny.pth"]]}}}}"""
        val r = repo { okJson(body) }

        assertEquals(listOf("canny.pth"), r.fetchControlNets())
    }

    @Test
    fun submitGeneration_returns_prompt_id() = runTest {
        val r = repo { okJson("""{"prompt_id":"g-1"}""") }

        val id = r.submitGeneration(ComfyUIGenerationParams(checkpoint = "m", prompt = "p"))

        assertEquals("g-1", id)
    }

    @Test
    fun submitGeneration_uses_custom_workflow_json_when_provided() = runTest {
        // Custom JSON bypasses the workflow builder; submission should still succeed.
        val r = repo { okJson("""{"prompt_id":"custom-1"}""") }

        val id = r.submitGeneration(
            ComfyUIGenerationParams(
                checkpoint = "m",
                prompt = "p",
                customWorkflowJson = """{"3":{"class_type":"CheckpointLoaderSimple","inputs":{}}}""",
            ),
        )

        assertEquals("custom-1", id)
    }

    @Test
    fun submitGeneration_throws_without_active_connection() = runTest {
        val r = repo(dao = FakeComfyUIConnectionDao()) { okJson("""{"prompt_id":"x"}""") }

        assertFailsWith<DomainException.ConnectionException> {
            r.submitGeneration(ComfyUIGenerationParams(checkpoint = "m", prompt = "p"))
        }
    }

    @Test
    fun pollGenerationResult_running_when_not_completed() = runTest {
        val body = """{"p1":{"status":{"completed":false},"outputs":{}}}"""
        val r = repo { okJson(body) }

        assertEquals(GenerationStatus.Running, r.pollGenerationResult("p1").status)
    }

    @Test
    fun pollGenerationResult_completed_with_images() = runTest {
        val body = """
            {"p1":{"status":{"status_str":"success"},"outputs":{"9":{"images":[{"filename":"a.png","type":"output"}]}}}}
        """.trimIndent()
        val r = repo { okJson(body) }

        val result = r.pollGenerationResult("p1")

        assertEquals(GenerationStatus.Completed, result.status)
        assertEquals(1, result.imageUrls.size)
    }

    @Test
    fun uploadMaskImage_returns_server_filename() = runTest {
        val r = repo { okJson("""{"name":"mask_uploaded.png","subfolder":"","type":"input"}""") }

        val name = r.uploadMaskImage(byteArrayOf(1, 2, 3))

        assertEquals("mask_uploaded.png", name)
    }

    @Test
    fun fetchObjectInfo_returns_raw_body() = runTest {
        val r = repo { okJson("""{"KSampler":{}}""") }

        assertTrue(r.fetchObjectInfo().contains("KSampler"))
    }

    @Test
    fun getImageUrl_builds_view_url() = runTest {
        val r = repo { okJson("{}") }
        r.fetchObjectInfo() // configures base URL

        val url = r.getImageUrl("o.png", subfolder = "", type = "output")

        assertTrue(url.startsWith("http://h:8188/view"))
        assertTrue(url.contains("filename=o.png"))
    }

    @Test
    fun submitGeneration_propagates_server_error() = runTest {
        // submitPrompt deserializes the response via .body(); an unparseable error body fails.
        val r = repo { respondError(HttpStatusCode.InternalServerError) }

        assertFailsWith<Exception> {
            r.submitGeneration(ComfyUIGenerationParams(checkpoint = "m", prompt = "p"))
        }
    }

    @Test
    fun fetchControlNets_returns_empty_on_unparseable_response() = runTest {
        // getControlNets swallows parse errors and returns an empty list rather than throwing.
        val r = repo { respondError(HttpStatusCode.InternalServerError) }

        assertEquals(emptyList(), r.fetchControlNets())
    }
}
