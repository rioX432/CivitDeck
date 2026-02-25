package com.riox432.civitdeck.domain.export

import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains
import kotlin.test.assertTrue

class WorkflowExportServiceTest {

    private val fullMeta = ImageGenerationMeta(
        prompt = "a beautiful landscape",
        negativePrompt = "ugly, blurry",
        sampler = "DPM++ 2M Karras",
        cfgScale = 7.0,
        steps = 20,
        seed = 12345L,
        model = "sd_xl_base_1.0",
        size = "1024x1024",
        additionalParams = mapOf("Model hash" to "e6bb9ea85b"),
    )

    private val minimalMeta = ImageGenerationMeta(
        prompt = null,
        negativePrompt = null,
        sampler = null,
        cfgScale = null,
        steps = null,
        seed = null,
        model = null,
        size = null,
    )

    // -- ComfyUI Workflow --

    @Test
    fun comfyUI_generates_valid_json() {
        val result = WorkflowExportService.generateComfyUIWorkflow(fullMeta)
        val json = Json.parseToJsonElement(result).jsonObject
        assertTrue(json.containsKey("3")) // CheckpointLoaderSimple
        assertTrue(json.containsKey("4")) // KSampler
        assertTrue(json.containsKey("5")) // EmptyLatentImage
        assertTrue(json.containsKey("6")) // CLIPTextEncode positive
        assertTrue(json.containsKey("7")) // CLIPTextEncode negative
        assertTrue(json.containsKey("8")) // VAEDecode
        assertTrue(json.containsKey("9")) // SaveImage
    }

    @Test
    fun comfyUI_includes_model_name() {
        val result = WorkflowExportService.generateComfyUIWorkflow(fullMeta)
        assertContains(result, "sd_xl_base_1.0")
    }

    @Test
    fun comfyUI_includes_prompt_and_negative() {
        val result = WorkflowExportService.generateComfyUIWorkflow(fullMeta)
        assertContains(result, "a beautiful landscape")
        assertContains(result, "ugly, blurry")
    }

    @Test
    fun comfyUI_parses_karras_scheduler() {
        val result = WorkflowExportService.generateComfyUIWorkflow(fullMeta)
        val json = Json.parseToJsonElement(result).jsonObject
        val kSamplerInputs = json["4"]!!.jsonObject["inputs"]!!.jsonObject
        assertEquals("dpmpp_2m", kSamplerInputs["sampler_name"]!!.jsonPrimitive.content)
        assertEquals("karras", kSamplerInputs["scheduler"]!!.jsonPrimitive.content)
    }

    @Test
    fun comfyUI_uses_defaults_for_minimal_meta() {
        val result = WorkflowExportService.generateComfyUIWorkflow(minimalMeta)
        val json = Json.parseToJsonElement(result).jsonObject
        val checkpoint = json["3"]!!.jsonObject["inputs"]!!.jsonObject
        assertEquals("model.safetensors", checkpoint["ckpt_name"]!!.jsonPrimitive.content)
    }

    // -- A1111 Params --

    @Test
    fun a1111_includes_all_fields() {
        val result = WorkflowExportService.generateA1111Params(fullMeta)
        assertContains(result, "a beautiful landscape")
        assertContains(result, "Negative prompt: ugly, blurry")
        assertContains(result, "Steps: 20")
        assertContains(result, "Sampler: DPM++ 2M Karras")
        assertContains(result, "CFG scale: 7.0")
        assertContains(result, "Seed: 12345")
        assertContains(result, "Size: 1024x1024")
        assertContains(result, "Model: sd_xl_base_1.0")
        assertContains(result, "Model hash: e6bb9ea85b")
    }

    @Test
    fun a1111_handles_missing_fields() {
        val result = WorkflowExportService.generateA1111Params(minimalMeta)
        // Should not crash, produces minimal output
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun a1111_prompt_only() {
        val meta = minimalMeta.copy(prompt = "hello world")
        val result = WorkflowExportService.generateA1111Params(meta)
        assertTrue(result.startsWith("hello world"))
    }

    // -- parseSize --

    @Test
    fun parseSize_valid() {
        assertEquals(1024 to 768, WorkflowExportService.parseSize("1024x768"))
    }

    @Test
    fun parseSize_null_returns_default() {
        assertEquals(512 to 512, WorkflowExportService.parseSize(null))
    }

    @Test
    fun parseSize_invalid_returns_default() {
        assertEquals(512 to 512, WorkflowExportService.parseSize("invalid"))
    }

    // -- parseSampler --

    @Test
    fun parseSampler_euler_a() {
        assertEquals("euler_ancestral" to "normal", WorkflowExportService.parseSampler("Euler a"))
    }

    @Test
    fun parseSampler_dpmpp_2m_karras() {
        assertEquals("dpmpp_2m" to "karras", WorkflowExportService.parseSampler("DPM++ 2M Karras"))
    }

    @Test
    fun parseSampler_null_returns_default() {
        assertEquals("euler" to "normal", WorkflowExportService.parseSampler(null))
    }
}
