package com.riox432.civitdeck.domain.export

import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Generates workflow export strings for ComfyUI and A1111 from image generation metadata.
 */
object WorkflowExportService {

    private val prettyJson = Json { prettyPrint = true }

    fun generateComfyUIWorkflow(meta: ImageGenerationMeta): String {
        val (width, height) = parseSize(meta.size)
        val (samplerName, scheduler) = parseSampler(meta.sampler)

        val workflow = buildJsonObject {
            put("3", checkpointLoaderNode(meta.model))
            put("6", clipTextEncodeNode(meta.prompt ?: ""))
            put("7", clipTextEncodeNode(meta.negativePrompt ?: ""))
            put("5", emptyLatentImageNode(width, height))
            put("4", kSamplerNode(meta, samplerName, scheduler))
            put("8", buildComfyNode("VAEDecode", "samples" to nodeLink("4", 0), "vae" to nodeLink("3", 2)))
            put(
                "9",
                buildComfyNode(
                    "SaveImage",
                    "filename_prefix" to JsonPrimitive("CivitDeck"),
                    "images" to nodeLink("8", 0)
                )
            )
        }

        return prettyJson.encodeToString(JsonObject.serializer(), workflow)
    }

    fun generateA1111Params(meta: ImageGenerationMeta): String = buildString {
        meta.prompt?.let { append(it) }
        meta.negativePrompt?.let {
            appendLine()
            append("Negative prompt: $it")
        }
        val params = buildList {
            meta.steps?.let { add("Steps: $it") }
            meta.sampler?.let { add("Sampler: $it") }
            meta.cfgScale?.let { add("CFG scale: $it") }
            meta.seed?.let { add("Seed: $it") }
            meta.size?.let { add("Size: $it") }
            meta.model?.let { add("Model: $it") }
            meta.additionalParams["Model hash"]?.let { add("Model hash: $it") }
        }
        if (params.isNotEmpty()) {
            appendLine()
            append(params.joinToString(", "))
        }
    }

    // -- ComfyUI node builders --

    private fun checkpointLoaderNode(modelName: String?) = buildJsonObject {
        put("class_type", "CheckpointLoaderSimple")
        put(
            "inputs",
            buildJsonObject {
                put("ckpt_name", modelName ?: "model.safetensors")
            }
        )
    }

    private fun clipTextEncodeNode(text: String) = buildJsonObject {
        put("class_type", "CLIPTextEncode")
        put(
            "inputs",
            buildJsonObject {
                put("text", text)
                put("clip", nodeLink("3", 1))
            }
        )
    }

    private fun emptyLatentImageNode(width: Int, height: Int) = buildJsonObject {
        put("class_type", "EmptyLatentImage")
        put(
            "inputs",
            buildJsonObject {
                put("width", width)
                put("height", height)
                put("batch_size", 1)
            }
        )
    }

    private fun kSamplerNode(
        meta: ImageGenerationMeta,
        samplerName: String,
        scheduler: String,
    ) = buildJsonObject {
        put("class_type", "KSampler")
        put(
            "inputs",
            buildJsonObject {
                put("seed", meta.seed ?: 0L)
                put("steps", meta.steps ?: DEFAULT_STEPS)
                put("cfg", meta.cfgScale ?: DEFAULT_CFG)
                put("sampler_name", samplerName)
                put("scheduler", scheduler)
                put("denoise", 1.0)
                put("model", nodeLink("3", 0))
                put("positive", nodeLink("6", 0))
                put("negative", nodeLink("7", 0))
                put("latent_image", nodeLink("5", 0))
            }
        )
    }

    private fun buildComfyNode(classType: String, vararg inputs: Pair<String, kotlinx.serialization.json.JsonElement>) =
        buildJsonObject {
            put("class_type", classType)
            put("inputs", buildJsonObject { inputs.forEach { (k, v) -> put(k, v) } })
        }

    private fun nodeLink(nodeId: String, outputIndex: Int) = buildJsonArray {
        add(JsonPrimitive(nodeId))
        add(JsonPrimitive(outputIndex))
    }

    // -- Parsing helpers --

    internal fun parseSize(size: String?): Pair<Int, Int> {
        if (size == null) return DEFAULT_DIMENSION to DEFAULT_DIMENSION
        val parts = size.split("x")
        return if (parts.size == 2) {
            val w = parts[0].trim().toIntOrNull() ?: DEFAULT_DIMENSION
            val h = parts[1].trim().toIntOrNull() ?: DEFAULT_DIMENSION
            w to h
        } else {
            DEFAULT_DIMENSION to DEFAULT_DIMENSION
        }
    }

    internal fun parseSampler(sampler: String?): Pair<String, String> {
        if (sampler == null) return DEFAULT_SAMPLER to DEFAULT_SCHEDULER
        val normalized = sampler.lowercase().trim()
        val isKarras = "karras" in normalized
        val cleaned = normalized
            .replace("karras", "")
            .trim()
            .replace(" ", "_")
        val comfyName = SAMPLER_MAP[cleaned] ?: cleaned
        val scheduler = if (isKarras) "karras" else DEFAULT_SCHEDULER
        return comfyName to scheduler
    }

    private val SAMPLER_MAP = mapOf(
        "euler_a" to "euler_ancestral",
        "euler_ancestral" to "euler_ancestral",
        "euler" to "euler",
        "dpm++_2m" to "dpmpp_2m",
        "dpm++_2s_a" to "dpmpp_2s_ancestral",
        "dpm++_sde" to "dpmpp_sde",
        "dpm++_2m_sde" to "dpmpp_2m_sde",
        "dpm++_3m_sde" to "dpmpp_3m_sde",
        "ddim" to "ddim",
        "lms" to "lms",
        "heun" to "heun",
        "uni_pc" to "uni_pc",
        "dpm_2" to "dpm_2",
        "dpm_2_a" to "dpm_2_ancestral",
    )

    private const val DEFAULT_DIMENSION = 512
    private const val DEFAULT_STEPS = 20
    private const val DEFAULT_CFG = 7.0
    private const val DEFAULT_SAMPLER = "euler"
    private const val DEFAULT_SCHEDULER = "normal"
}
