package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.DiffusionLatentNode
import com.riox432.civitdeck.domain.model.DiffusionModelResources
import com.riox432.civitdeck.domain.model.DiffusionModelSelection
import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.model.LoraSelection
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers [ComfyUIGenerationRepositoryImpl]'s non-WebSocket surface: asset and loader-list fetch,
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
    fun fetchLoras_parses_object_info_list() = runTest {
        val body = """{"LoraLoader":{"input":{"required":{"lora_name":[["lora1.safetensors"]]}}}}"""
        val r = repo { okJson(body) }

        assertEquals(listOf("lora1.safetensors"), r.fetchLoras())
    }

    private fun loaderRepo(bodies: Map<String, String>) = repo { request ->
        okJson(bodies[request.url.encodedPath.removePrefix("/object_info/")] ?: "{}")
    }

    private val expectedResources = DiffusionModelResources(
        diffusionModels = listOf("krea2_turbo_fp8_scaled.safetensors"),
        textEncoders = listOf("qwen3vl_4b_fp8_scaled.safetensors"),
        vaes = listOf("qwen_image_vae.safetensors", "pixel_space"),
        clipTypes = listOf("stable_diffusion", "krea2"),
    )

    @Test
    fun fetchDiffusionModelResources_reads_the_v1_loader_combos() = runTest {
        // Shapes of ComfyUI's V1 INPUT_TYPES for UNETLoader, CLIPLoader and VAELoader.
        val r = loaderRepo(
            mapOf(
                "UNETLoader" to """
                    {"UNETLoader":{"input":{"required":{
                        "unet_name":[["krea2_turbo_fp8_scaled.safetensors"]],
                        "weight_dtype":[["default","fp8_e4m3fn"],{"advanced":true}]}}}}
                """.trimIndent(),
                "CLIPLoader" to """
                    {"CLIPLoader":{"input":{"required":{
                        "clip_name":[["qwen3vl_4b_fp8_scaled.safetensors"]],
                        "type":[["stable_diffusion","krea2"]]},
                        "optional":{"device":[["default","cpu"],{"advanced":true}]}}}}
                """.trimIndent(),
                "VAELoader" to """
                    {"VAELoader":{"input":{"required":{
                        "vae_name":[["qwen_image_vae.safetensors","pixel_space"],{"tooltip":"VAE"}]}}}}
                """.trimIndent(),
            ),
        )

        assertEquals(expectedResources, r.fetchDiffusionModelResources())
    }

    @Test
    fun fetchDiffusionModelResources_reads_the_v3_combo_shape() = runTest {
        val r = loaderRepo(
            mapOf(
                "UNETLoader" to """
                    {"UNETLoader":{"input":{"required":{
                        "unet_name":["COMBO",{"options":["krea2_turbo_fp8_scaled.safetensors"]}]}}}}
                """.trimIndent(),
                "CLIPLoader" to """
                    {"CLIPLoader":{"input":{"required":{
                        "clip_name":["COMBO",{"options":["qwen3vl_4b_fp8_scaled.safetensors"]}],
                        "type":["COMBO",{"options":["stable_diffusion","krea2"],"tooltip":"type"}]}}}}
                """.trimIndent(),
                "VAELoader" to """
                    {"VAELoader":{"input":{"required":{
                        "vae_name":["COMBO",{"options":["qwen_image_vae.safetensors","pixel_space"]}]}}}}
                """.trimIndent(),
            ),
        )

        assertEquals(expectedResources, r.fetchDiffusionModelResources())
    }

    @Test
    fun fetchDiffusionModelResources_gives_an_empty_diffusion_list_without_unet_loader() = runTest {
        // ComfyUI answers /object_info/<unknown node> with an empty object.
        val r = loaderRepo(
            mapOf(
                "UNETLoader" to "{}",
                "CLIPLoader" to """{"CLIPLoader":{"input":{"required":{"clip_name":[["te.safetensors"]],"type":[["sd3"]]}}}}""",
                "VAELoader" to """{"VAELoader":{"input":{"required":{"vae_name":[["ae.safetensors"]]}}}}""",
            ),
        )

        val resources = r.fetchDiffusionModelResources()

        assertEquals(emptyList(), resources.diffusionModels)
        assertEquals(listOf("te.safetensors"), resources.textEncoders)
        assertEquals(listOf("sd3"), resources.clipTypes)
        assertEquals(listOf("ae.safetensors"), resources.vaes)
    }

    @Test
    fun fetchDiffusionModelResources_throws_on_server_error() = runTest {
        val r = repo { respondError(HttpStatusCode.InternalServerError) }

        assertFailsWith<ResponseException> { r.fetchDiffusionModelResources() }
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
    fun pollGenerationResult_running_when_history_absent() = runTest {
        val r = repo { okJson("{}") }

        assertEquals(GenerationStatus.Running, r.pollGenerationResult("missing").status)
    }

    @Test
    fun pollGenerationResult_error_when_completed_without_images() = runTest {
        val body = """{"p1":{"status":{"completed":true},"outputs":{}}}"""
        val r = repo { okJson(body) }

        val result = r.pollGenerationResult("p1")

        assertEquals(GenerationStatus.Error, result.status)
        assertEquals("No images generated", result.error)
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
    fun pollGenerationResult_reports_the_exception_message_of_a_failed_job() = runTest {
        // History entry ComfyUI stores for a job whose node raised during execution.
        val body = """
            {"p1":{"status":{"status_str":"error","completed":false,"messages":[["execution_start",{"prompt_id":"p1","timestamp":1}],["execution_error",{"prompt_id":"p1","node_id":"3","node_type":"KSampler","exception_message":"CUDA out of memory","exception_type":"torch.OutOfMemoryError","traceback":[],"current_inputs":{},"current_outputs":[]}]]},"outputs":{}}}
        """.trimIndent()
        val r = repo { okJson(body) }

        val result = r.pollGenerationResult("p1")

        assertEquals(GenerationStatus.Error, result.status)
        assertEquals("CUDA out of memory", result.error)
    }

    @Test
    fun pollGenerationResult_reports_an_interrupted_job_as_error_without_a_message() = runTest {
        val body = """
            {"p1":{"status":{"status_str":"error","completed":false,"messages":[["execution_interrupted",{"prompt_id":"p1","node_id":"3","node_type":"KSampler","executed":[]}]]},"outputs":{}}}
        """.trimIndent()
        val r = repo { okJson(body) }

        val result = r.pollGenerationResult("p1")

        assertEquals(GenerationStatus.Error, result.status)
        assertNull(result.error)
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
    fun getImageUrl_includes_a_non_empty_subfolder() = runTest {
        val r = repo { okJson("{}") }
        r.fetchObjectInfo() // configures base URL

        val url = r.getImageUrl("img.png", subfolder = "sub", type = "output")

        assertTrue(url.startsWith("http://h:8188/view"))
        assertTrue(url.contains("filename=img.png"))
        assertTrue(url.contains("subfolder=sub"))
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

    /** Submits [params] and returns the `prompt` graph sent to `/prompt`. */
    private suspend fun submittedGraph(params: ComfyUIGenerationParams): JsonObject {
        var promptBody: JsonObject? = null
        val r = repo { request ->
            if (request.url.encodedPath == "/prompt") {
                promptBody = testJson.decodeFromString(request.body.toByteArray().decodeToString())
            }
            okJson("""{"prompt_id":"g-1"}""")
        }
        r.submitGeneration(params)
        return requireNotNull(promptBody)["prompt"]!!.jsonObject
    }

    private fun JsonObject.node(id: String) = getValue(id).jsonObject

    private fun JsonObject.input(id: String, name: String) = node(id)["inputs"]!!.jsonObject.getValue(name)

    private fun link(nodeId: String, output: Int) = buildJsonArray {
        add(JsonPrimitive(nodeId))
        add(JsonPrimitive(output))
    }

    private val checkpointParams = ComfyUIGenerationParams(
        checkpoint = "sd15.safetensors",
        prompt = "a cat",
        negativePrompt = "blurry",
        seed = 42,
        loraSelections = listOf(LoraSelection("a.safetensors", 0.5f, 0.25f), LoraSelection("b.safetensors")),
    )

    @Test
    fun submitGeneration_without_diffusion_model_sends_the_checkpoint_graph_unchanged() = runTest {
        val graph = submittedGraph(checkpointParams)

        assertEquals(CHECKPOINT_GRAPH, testJson.encodeToString(JsonObject.serializer(), graph))
    }

    @Test
    fun submitGeneration_without_diffusion_model_sends_the_inpainting_graph_unchanged() = runTest {
        val params = checkpointParams.copy(
            loraSelections = listOf(LoraSelection("a.safetensors", 0.5f, 0.25f)),
            initImageFilename = "init.png",
            maskImageFilename = "mask.png",
            denoiseStrength = 0.5,
        )

        val graph = submittedGraph(params)

        assertEquals(INPAINTING_GRAPH, testJson.encodeToString(JsonObject.serializer(), graph))
    }

    private val krea2 = DiffusionModelSelection(
        unetName = "krea2_turbo_fp8_scaled.safetensors",
        textEncoderName = "qwen3vl_4b_fp8_scaled.safetensors",
        clipType = "krea2",
        vaeName = "qwen_image_vae.safetensors",
    )

    private val diffusionParams = ComfyUIGenerationParams(
        checkpoint = "",
        prompt = "a cat",
        negativePrompt = "blurry",
        seed = 42,
        diffusionModel = krea2,
    )

    @Test
    fun submitGeneration_with_diffusion_model_loads_unet_text_encoder_and_vae_separately() = runTest {
        val graph = submittedGraph(diffusionParams)

        assertEquals(
            testJson.parseToJsonElement(
                """{"class_type":"UNETLoader","inputs":{"unet_name":"krea2_turbo_fp8_scaled.safetensors","weight_dtype":"default"}}""",
            ),
            graph.node("3"),
        )
        assertEquals(
            testJson.parseToJsonElement(
                """{"class_type":"CLIPLoader","inputs":{"clip_name":"qwen3vl_4b_fp8_scaled.safetensors","type":"krea2"}}""",
            ),
            graph.node("40"),
        )
        assertEquals(
            testJson.parseToJsonElement("""{"class_type":"VAELoader","inputs":{"vae_name":"qwen_image_vae.safetensors"}}"""),
            graph.node("41"),
        )
        assertFalse(graph.values.any { it.jsonObject["class_type"]?.jsonPrimitive?.content == "CheckpointLoaderSimple" })
        assertEquals(link("40", 0), graph.input("6", "clip"))
        assertEquals(link("40", 0), graph.input("7", "clip"))
        assertEquals(link("3", 0), graph.input("4", "model"))
        assertEquals(link("41", 0), graph.input("8", "vae"))
        assertEquals("EmptyLatentImage", graph.node("5")["class_type"]?.jsonPrimitive?.content)
        assertFalse(graph.values.any { it.jsonObject["class_type"]?.jsonPrimitive?.content == "ModelSamplingAuraFlow" })
    }

    private val zImageTurboParams = diffusionParams.copy(
        width = 1024,
        height = 768,
        diffusionModel = DiffusionModelSelection(
            unetName = "z_image_turbo_bf16.safetensors",
            textEncoderName = "qwen_3_4b.safetensors",
            clipType = "lumina2",
            vaeName = "ae.safetensors",
            latentNode = DiffusionLatentNode.EMPTY_SD3_LATENT_IMAGE,
            auraFlowShift = 3.0,
        ),
    )

    @Test
    fun submitGeneration_with_aura_flow_shift_samples_through_model_sampling_aura_flow() = runTest {
        val graph = submittedGraph(zImageTurboParams)

        assertEquals(
            testJson.parseToJsonElement("""{"class_type":"ModelSamplingAuraFlow","inputs":{"model":["3",0],"shift":3.0}}"""),
            graph.node("42"),
        )
        assertEquals(link("42", 0), graph.input("4", "model"))
        assertEquals(
            testJson.parseToJsonElement(
                """{"class_type":"EmptySD3LatentImage","inputs":{"width":1024,"height":768,"batch_size":1}}""",
            ),
            graph.node("5"),
        )
        assertEquals(link("5", 0), graph.input("4", "latent_image"))
        assertFalse(graph.values.any { it.jsonObject["class_type"]?.jsonPrimitive?.content == "EmptyLatentImage" })
        assertEquals("lumina2", graph.input("40", "type").jsonPrimitive.content)
    }

    @Test
    fun submitGeneration_with_aura_flow_shift_reads_the_model_from_the_end_of_the_lora_chain() = runTest {
        val params = zImageTurboParams.copy(
            loraSelections = listOf(LoraSelection("a.safetensors"), LoraSelection("b.safetensors")),
        )

        val graph = submittedGraph(params)

        assertEquals(link("3", 0), graph.input("10", "model"))
        assertEquals(link("11", 0), graph.input("42", "model"))
        assertEquals(link("42", 0), graph.input("4", "model"))
    }

    @Test
    fun submitGeneration_with_diffusion_model_chains_loras_from_the_split_loaders() = runTest {
        val params = diffusionParams.copy(
            loraSelections = listOf(LoraSelection("a.safetensors", 0.5f, 0.25f), LoraSelection("b.safetensors")),
        )

        val graph = submittedGraph(params)

        assertEquals("LoraLoader", graph.node("10")["class_type"]?.jsonPrimitive?.content)
        assertEquals(link("3", 0), graph.input("10", "model"))
        assertEquals(link("40", 0), graph.input("10", "clip"))
        assertEquals(link("10", 0), graph.input("11", "model"))
        assertEquals(link("10", 1), graph.input("11", "clip"))
        assertEquals(link("11", 1), graph.input("6", "clip"))
        assertEquals(link("11", 1), graph.input("7", "clip"))
        assertEquals(link("11", 0), graph.input("4", "model"))
        assertEquals(link("41", 0), graph.input("8", "vae"))
    }

    @Test
    fun submitGeneration_rejects_a_diffusion_model_combined_with_controlnet_or_mask() = runTest {
        var prompts = 0
        val r = repo { request ->
            if (request.url.encodedPath == "/prompt") prompts++
            okJson("""{"prompt_id":"g-1"}""")
        }
        val withControlNet = diffusionParams.copy(controlNetEnabled = true, controlNetModel = "canny.pth")
        val withInpainting = diffusionParams.copy(initImageFilename = "init.png", maskImageFilename = "mask.png")
        val withMaskOnly = diffusionParams.copy(maskImageFilename = "mask.png")

        assertEquals(
            "ControlNet cannot be combined with a diffusion model",
            assertFailsWith<IllegalArgumentException> { r.submitGeneration(withControlNet) }.message,
        )
        assertFailsWith<IllegalArgumentException> { r.submitGeneration(withInpainting) }
        assertFailsWith<IllegalArgumentException> { r.submitGeneration(withMaskOnly) }
        assertEquals(0, prompts)
    }

    @Test
    fun submitGeneration_with_diffusion_model_ignores_a_controlnet_toggle_without_a_model() = runTest {
        // The builder adds no ControlNet nodes for a blank model, so nothing would be dropped.
        val graph = submittedGraph(diffusionParams.copy(controlNetEnabled = true, controlNetModel = ""))

        assertEquals(link("6", 0), graph.input("4", "positive"))
    }

    private val checkpointWithControlNet = checkpointParams.copy(
        controlNetEnabled = true,
        controlNetModel = "canny.pth",
        controlNetStrength = 0.75f,
    )

    @Test
    fun submitGeneration_rejects_controlnet_on_a_checkpoint_without_a_control_image() = runTest {
        var prompts = 0
        val r = repo { request ->
            if (request.url.encodedPath == "/prompt") prompts++
            okJson("""{"prompt_id":"g-1"}""")
        }
        val withInpainting = checkpointWithControlNet.copy(initImageFilename = "init.png", maskImageFilename = "mask.png")

        assertFailsWith<IllegalArgumentException> { r.submitGeneration(checkpointWithControlNet) }
        assertFailsWith<IllegalArgumentException> { r.submitGeneration(withInpainting) }
        assertEquals(0, prompts)
    }

    @Test
    fun submitGeneration_never_sends_a_controlnet_apply_without_a_linked_image() = runTest {
        val sentGraphs = mutableListOf<JsonObject>()
        val r = repo { request ->
            if (request.url.encodedPath == "/prompt") {
                val body: JsonObject = testJson.decodeFromString(request.body.toByteArray().decodeToString())
                sentGraphs += body.getValue("prompt").jsonObject
            }
            okJson("""{"prompt_id":"g-1"}""")
        }
        val controlNetRequests = listOf(
            checkpointWithControlNet,
            checkpointWithControlNet.copy(initImageFilename = "init.png", maskImageFilename = "mask.png"),
            checkpointWithControlNet.copy(controlNetModel = ""),
            diffusionParams.copy(controlNetEnabled = true, controlNetModel = "canny.pth"),
            diffusionParams.copy(controlNetEnabled = true, controlNetModel = ""),
        )

        controlNetRequests.forEach { params ->
            runCatching { r.submitGeneration(params) }.onFailure { assertIs<IllegalArgumentException>(it) }
        }

        val controlNetImages = sentGraphs.flatMap { graph ->
            graph.values.map { it.jsonObject }
                .filter { it["class_type"]?.jsonPrimitive?.content == "ControlNetApply" }
                .map { it.getValue("inputs").jsonObject["image"] }
        }
        assertTrue(sentGraphs.isNotEmpty())
        assertTrue(controlNetImages.all { it is JsonArray && it.size == 2 }, "Unlinked ControlNet image: $controlNetImages")
    }

    @Test
    fun fetchControlNets_returns_empty_on_unparseable_response() = runTest {
        // getControlNets swallows parse errors of a 200 body and returns an empty list rather than throwing.
        val r = repo { okJson("not json") }

        assertEquals(emptyList(), r.fetchControlNets())
    }

    @Test
    fun fetchControlNets_throws_on_server_error() = runTest {
        val r = repo { respondError(HttpStatusCode.InternalServerError) }

        assertFailsWith<ResponseException> { r.fetchControlNets() }
    }

    private companion object {
        // Checkpoint graphs the builder sends, one node per line.
        val CHECKPOINT_GRAPH = """
            {"3":{"class_type":"CheckpointLoaderSimple","inputs":{"ckpt_name":"sd15.safetensors"}},
            "10":{"class_type":"LoraLoader","inputs":{"lora_name":"a.safetensors","strength_model":0.5,
            "strength_clip":0.25,"model":["3",0],"clip":["3",1]}},
            "11":{"class_type":"LoraLoader","inputs":{"lora_name":"b.safetensors","strength_model":1.0,
            "strength_clip":1.0,"model":["10",0],"clip":["10",1]}},
            "6":{"class_type":"CLIPTextEncode","inputs":{"text":"a cat","clip":["11",1]}},
            "7":{"class_type":"CLIPTextEncode","inputs":{"text":"blurry","clip":["11",1]}},
            "5":{"class_type":"EmptyLatentImage","inputs":{"width":512,"height":512,"batch_size":1}},
            "4":{"class_type":"KSampler","inputs":{"seed":42,"steps":20,"cfg":7.0,"sampler_name":"euler",
            "scheduler":"normal","denoise":1.0,"model":["11",0],"positive":["6",0],"negative":["7",0],
            "latent_image":["5",0]}},
            "8":{"class_type":"VAEDecode","inputs":{"samples":["4",0],"vae":["3",2]}},
            "9":{"class_type":"SaveImage","inputs":{"filename_prefix":"CivitDeck","images":["8",0]}}}
        """.trimIndent().replace("\n", "")

        val INPAINTING_GRAPH = """
            {"3":{"class_type":"CheckpointLoaderSimple","inputs":{"ckpt_name":"sd15.safetensors"}},
            "10":{"class_type":"LoraLoader","inputs":{"lora_name":"a.safetensors","strength_model":0.5,
            "strength_clip":0.25,"model":["3",0],"clip":["3",1]}},
            "30":{"class_type":"LoadImage","inputs":{"image":"init.png"}},
            "31":{"class_type":"LoadImage","inputs":{"image":"mask.png"}},
            "32":{"class_type":"VAEEncodeForInpaint","inputs":{"pixels":["30",0],"vae":["3",2],"mask":["31",0],
            "grow_mask_by":6}},
            "6":{"class_type":"CLIPTextEncode","inputs":{"text":"a cat","clip":["10",1]}},
            "7":{"class_type":"CLIPTextEncode","inputs":{"text":"blurry","clip":["10",1]}},
            "4":{"class_type":"KSampler","inputs":{"seed":42,"steps":20,"cfg":7.0,"sampler_name":"euler",
            "scheduler":"normal","denoise":0.5,"model":["10",0],"positive":["6",0],"negative":["7",0],
            "latent_image":["32",0]}},
            "8":{"class_type":"VAEDecode","inputs":{"samples":["4",0],"vae":["3",2]}},
            "9":{"class_type":"SaveImage","inputs":{"filename_prefix":"CivitDeck","images":["8",0]}}}
        """.trimIndent().replace("\n", "")
    }
}
