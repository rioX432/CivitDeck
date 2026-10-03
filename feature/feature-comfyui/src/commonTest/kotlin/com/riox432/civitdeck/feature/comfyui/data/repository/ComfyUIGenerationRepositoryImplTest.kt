package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.feature.comfyui.data.ComfyUIApiProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers [ComfyUIGenerationRepositoryImpl]'s non-WebSocket surface: asset fetch,
 * prompt submission, history-based result polling, mask upload, object-info fetch,
 * image URL building, and the active-connection guard, plus the client id shared by
 * prompt submission and the WebSocket handshake. WebSocket message handling is excluded.
 */
class ComfyUIGenerationRepositoryImplTest {

    private fun daoWithActive() = FakeComfyUIConnectionDao().apply {
        rows.add(ComfyUIConnectionEntity(id = 1, name = "A", hostname = "h", port = 8188, isActive = true, createdAt = 1))
    }

    private fun repo(
        dao: FakeComfyUIConnectionDao = daoWithActive(),
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ): ComfyUIGenerationRepositoryImpl {
        val provider = ComfyUIApiProvider(dao, mockClient(handler), testJson)
        return ComfyUIGenerationRepositoryImpl(provider, testJson)
    }

    @Test
    fun fetchCheckpoints_reads_names_from_first_element_of_ckpt_name() = runTest {
        val withTooltip = """
            {"CheckpointLoaderSimple":{"input":{"required":{"ckpt_name":[
                ["a.safetensors","b.ckpt"],
                {"tooltip":"The name of the checkpoint (model) to load."}
            ]}}}}
        """.trimIndent()
        val legacy = """{"CheckpointLoaderSimple":{"input":{"required":{"ckpt_name":[["a.safetensors","b.ckpt"]]}}}}"""
        val expected = listOf("a.safetensors", "b.ckpt")

        assertEquals(expected, repo { okJson(withTooltip) }.fetchCheckpoints())
        assertEquals(expected, repo { okJson(legacy) }.fetchCheckpoints())
        assertEquals(emptyList(), repo { okJson("{}") }.fetchCheckpoints())
    }

    @Test
    fun fetchCheckpoints_throws_on_invalid_json() = runTest {
        val r = repo { okJson("not json") }

        assertFailsWith<SerializationException> { r.fetchCheckpoints() }
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
    fun submitGeneration_sends_the_client_id_the_progress_socket_connects_with() = runTest {
        var promptBody: JsonObject? = null
        var wsUrl: Url? = null
        val client = HttpClient(
            MockEngine { request ->
                when {
                    request.url.encodedPath.startsWith("/ws") -> wsUrl = request.url
                    request.url.encodedPath == "/prompt" ->
                        promptBody = testJson.decodeFromString(request.body.toByteArray().decodeToString())
                }
                okJson("""{"prompt_id":"g-1"}""")
            },
        ) {
            install(ContentNegotiation) { json(testJson) }
            install(WebSockets)
        }
        val r = ComfyUIGenerationRepositoryImpl(ComfyUIApiProvider(daoWithActive(), client, testJson), testJson)

        r.submitGeneration(ComfyUIGenerationParams(checkpoint = "m", prompt = "p"))
        // The mock cannot complete a WebSocket handshake; only the handshake URL matters here.
        runCatching { r.observeGenerationProgress("g-1", "http://h:8188", "ws").toList() }

        val submittedId = promptBody?.get("client_id")?.jsonPrimitive?.content
        // The socket path embeds the query, so re-parse the full URL to read it.
        val socketId = wsUrl?.let { Url(it.toString()).parameters["clientId"] }
        assertFalse(submittedId.isNullOrBlank())
        assertEquals(submittedId, socketId)
    }

    @Test
    fun submitGeneration_resolves_negative_seed_to_a_random_nonnegative_value() = runTest {
        suspend fun ksamplerSeed(seed: Long): Long {
            var promptBody: JsonObject? = null
            val r = repo { request ->
                if (request.url.encodedPath == "/prompt") {
                    promptBody = testJson.decodeFromString(request.body.toByteArray().decodeToString())
                }
                okJson("""{"prompt_id":"g-1"}""")
            }

            r.submitGeneration(ComfyUIGenerationParams(checkpoint = "m", prompt = "p", seed = seed))

            val nodes = promptBody!!["prompt"]!!.jsonObject
            val ksampler = nodes.values.first {
                it.jsonObject["class_type"]?.jsonPrimitive?.content == "KSampler"
            }
            return ksampler.jsonObject["inputs"]!!.jsonObject["seed"]!!.jsonPrimitive.long
        }

        assertTrue(ksamplerSeed(-1) >= 0)
        assertEquals(42L, ksamplerSeed(42))
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
    fun submitGeneration_reports_the_validation_reason_from_a_rejected_prompt() = runTest {
        // Body shape of ComfyUI's /prompt 400 for a workflow that fails validation.
        val body = """
            {"error":{"type":"prompt_outputs_failed_validation","message":"Prompt outputs failed validation",
            "details":"Value -1 smaller than min of 0: seed","extra_info":{}},
            "node_errors":{"3":{"errors":[{"type":"value_smaller_than_min","message":"Value -1 smaller than min of 0",
            "details":"seed","extra_info":{"input_name":"seed"}}],"dependent_outputs":["9"],"class_type":"KSampler"}}}
        """.trimIndent()
        val r = repo { respond(ByteReadChannel(body), HttpStatusCode.BadRequest, jsonHeaders) }

        val e = assertFailsWith<ResponseException> {
            r.submitGeneration(ComfyUIGenerationParams(checkpoint = "m", prompt = "p"))
        }

        val message = e.message.orEmpty()
        assertTrue(message.contains("Value -1 smaller than min of 0"), message)
        assertFalse(message.contains("prompt_id"), message)
    }

    @Test
    fun submitGeneration_reports_a_string_error_or_the_status_for_other_rejections() = runTest {
        suspend fun rejectionMessage(status: HttpStatusCode, body: String): String? {
            val r = repo { respond(ByteReadChannel(body), status, jsonHeaders) }
            return assertFailsWith<ResponseException> {
                r.submitGeneration(ComfyUIGenerationParams(checkpoint = "m", prompt = "p"))
            }.message
        }

        assertEquals(
            "No prompt provided",
            rejectionMessage(HttpStatusCode.BadRequest, """{"error":"No prompt provided"}"""),
        )
        assertEquals("HTTP 502", rejectionMessage(HttpStatusCode.BadGateway, "<html>Bad Gateway</html>"))
    }

    @Test
    fun submitGeneration_propagates_server_error() = runTest {
        // A non-2xx response fails before its body is decoded into PromptResponse.
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
