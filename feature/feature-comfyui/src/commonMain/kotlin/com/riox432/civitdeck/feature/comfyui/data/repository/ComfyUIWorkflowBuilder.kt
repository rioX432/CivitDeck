package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.LoraSelection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Builds ComfyUI prompt graph JSON from [ComfyUIGenerationParams].
 * Extracted from [ComfyUIRepositoryImpl] to keep the repository focused on
 * connection/generation/queue responsibilities.
 */
internal class ComfyUIWorkflowBuilder(private val json: Json) {

    fun buildWorkflow(params: ComfyUIGenerationParams): JsonObject {
        // If custom workflow JSON is provided, use it directly
        val customJson = params.customWorkflowJson
        if (customJson != null) {
            return json.decodeFromString(customJson)
        }

        val loraChain = buildLoraChain(params.loraSelections)
        val finalModelNodeId = loraChain.lastOrNull()?.nodeId ?: "3"
        val finalClipNodeId = loraChain.lastOrNull()?.nodeId ?: "3"
        val positiveCondId =
            if (params.controlNetEnabled && params.controlNetModel.isNotBlank()) "21" else "6"

        return buildJsonObject {
            put("3", buildCheckpointNode(params.checkpoint))
            loraChain.forEach { put(it.nodeId, it.jsonNode) }
            put("6", buildClipTextNode(params.prompt, finalClipNodeId))
            put("7", buildClipTextNode(params.negativePrompt, finalClipNodeId))
            if (params.controlNetEnabled && params.controlNetModel.isNotBlank()) {
                buildControlNetNodes(params).forEach { (id, node) -> put(id, node) }
            }
            put("5", buildLatentImageNode(params.width, params.height))
            put("4", buildKSamplerNode(params, finalModelNodeId, positiveCondId))
            put("8", buildVAEDecodeNode())
            put("9", buildSaveImageNode())
        }
    }

    private fun buildCheckpointNode(checkpoint: String) = buildJsonObject {
        put("class_type", "CheckpointLoaderSimple")
        put("inputs", buildJsonObject { put("ckpt_name", checkpoint) })
    }

    private fun buildClipTextNode(text: String, clipNodeId: String) = buildJsonObject {
        put("class_type", "CLIPTextEncode")
        put(
            "inputs",
            buildJsonObject {
                put("text", text)
                put("clip", nodeLink(clipNodeId, 1))
            }
        )
    }

    private fun buildControlNetNodes(
        params: ComfyUIGenerationParams,
    ): List<Pair<String, JsonObject>> {
        val loader = buildJsonObject {
            put("class_type", "ControlNetLoader")
            put("inputs", buildJsonObject { put("control_net_name", params.controlNetModel) })
        }
        val apply = buildJsonObject {
            put("class_type", "ControlNetApply")
            put(
                "inputs",
                buildJsonObject {
                    put("conditioning", nodeLink("6", 0))
                    put("control_net", nodeLink("20", 0))
                    put("image", buildJsonArray { })
                    put("strength", params.controlNetStrength.toDouble())
                }
            )
        }
        return listOf("20" to loader, "21" to apply)
    }

    private fun buildLatentImageNode(width: Int, height: Int) = buildJsonObject {
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

    private fun buildKSamplerNode(
        params: ComfyUIGenerationParams,
        modelNodeId: String,
        positiveCondId: String,
    ) = buildJsonObject {
        put("class_type", "KSampler")
        put(
            "inputs",
            buildJsonObject {
                put("seed", params.seed)
                put("steps", params.steps)
                put("cfg", params.cfgScale)
                put("sampler_name", params.samplerName)
                put("scheduler", params.scheduler)
                put("denoise", 1.0)
                put("model", nodeLink(modelNodeId, 0))
                put("positive", nodeLink(positiveCondId, 0))
                put("negative", nodeLink("7", 0))
                put("latent_image", nodeLink("5", 0))
            }
        )
    }

    private fun buildVAEDecodeNode() = buildJsonObject {
        put("class_type", "VAEDecode")
        put(
            "inputs",
            buildJsonObject {
                put("samples", nodeLink("4", 0))
                put("vae", nodeLink("3", 2))
            }
        )
    }

    private fun buildSaveImageNode() = buildJsonObject {
        put("class_type", "SaveImage")
        put(
            "inputs",
            buildJsonObject {
                put("filename_prefix", "CivitDeck")
                put("images", nodeLink("8", 0))
            }
        )
    }

    /**
     * Builds a chain of LoraLoader nodes. Each LoRA takes the model/clip output
     * of the previous node (or the checkpoint loader for the first LoRA).
     * Returns a list of (nodeId, jsonNode) pairs.
     */
    private data class LoraNodeEntry(val nodeId: String, val jsonNode: JsonObject)

    private fun buildLoraChain(loras: List<LoraSelection>): List<LoraNodeEntry> {
        if (loras.isEmpty()) return emptyList()
        val entries = mutableListOf<LoraNodeEntry>()
        loras.forEachIndexed { index, lora ->
            val nodeId = (10 + index).toString()
            val prevNodeId = if (index == 0) "3" else (10 + index - 1).toString()
            val node = buildJsonObject {
                put("class_type", "LoraLoader")
                put(
                    "inputs",
                    buildJsonObject {
                        put("lora_name", lora.name)
                        put("strength_model", lora.strengthModel.toDouble())
                        put("strength_clip", lora.strengthClip.toDouble())
                        put("model", nodeLink(prevNodeId, 0))
                        put("clip", nodeLink(prevNodeId, 1))
                    }
                )
            }
            entries.add(LoraNodeEntry(nodeId, node))
        }
        return entries
    }

    private fun nodeLink(nodeId: String, outputIndex: Int) = buildJsonArray {
        add(JsonPrimitive(nodeId))
        add(JsonPrimitive(outputIndex))
    }
}
