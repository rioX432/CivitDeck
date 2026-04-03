package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.api.comfyui.ComfyUIOutputImage
import com.riox432.civitdeck.data.api.comfyui.ComfyUIWebSocketApi
import com.riox432.civitdeck.data.api.comfyui.ComfyUIWebSocketMessage
import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.ComfyUIConnectionDao
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationProgress
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.model.LoraSelection
import com.riox432.civitdeck.domain.repository.ComfyUIGenerationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ComfyUIGenerationRepositoryImpl(
    private val dao: ComfyUIConnectionDao,
    private val api: ComfyUIApi,
    private val webSocketApi: ComfyUIWebSocketApi,
    private val json: Json,
) : ComfyUIGenerationRepository {

    override suspend fun fetchCheckpoints(): List<String> {
        ensureApiConfigured()
        return api.getCheckpoints()
    }

    override suspend fun fetchLoras(): List<String> {
        ensureApiConfigured()
        return api.getLoras()
    }

    override suspend fun fetchControlNets(): List<String> {
        ensureApiConfigured()
        return api.getControlNets()
    }

    override suspend fun submitGeneration(params: ComfyUIGenerationParams): String {
        ensureApiConfigured()
        val workflow = buildWorkflow(params)
        val response = api.submitPrompt(workflow)
        return response.promptId
    }

    override suspend fun pollGenerationResult(promptId: String): GenerationResult {
        ensureApiConfigured()
        val entry = api.getHistory(promptId)
            ?: return GenerationResult(promptId, GenerationStatus.Running)

        val completed = entry.status?.completed == true ||
            entry.status?.statusStr == "success"

        val imageUrls = entry.outputs.values
            .flatMap { it.images ?: emptyList() }
            .map { api.getImageUrl(it) }

        return if (completed && imageUrls.isNotEmpty()) {
            GenerationResult(promptId, GenerationStatus.Completed, imageUrls)
        } else if (completed) {
            GenerationResult(promptId, GenerationStatus.Error, error = "No images generated")
        } else {
            GenerationResult(promptId, GenerationStatus.Running)
        }
    }

    override fun observeGenerationProgress(
        promptId: String,
        host: String,
        port: Int,
    ): Flow<GenerationProgress> {
        val clientId = "civitdeck-${currentTimeMillis()}"
        return webSocketApi.observeProgress(host, port, clientId, promptId).mapNotNull { msg ->
            when (msg) {
                is ComfyUIWebSocketMessage.Progress -> GenerationProgress(
                    promptId = msg.promptId,
                    currentStep = msg.value,
                    totalSteps = msg.max,
                    currentNode = msg.node,
                )
                is ComfyUIWebSocketMessage.PreviewImage -> GenerationProgress(
                    promptId = promptId,
                    currentStep = 0,
                    totalSteps = 0,
                    previewImageBytes = msg.imageBytes,
                )
                else -> null
            }
        }
    }

    override suspend fun interruptGeneration() {
        ensureApiConfigured()
        api.interrupt()
    }

    override fun getImageUrl(filename: String, subfolder: String, type: String): String {
        return api.getImageUrl(ComfyUIOutputImage(filename, subfolder, type))
    }

    private suspend fun ensureApiConfigured() {
        val active = dao.getActive() ?: error("No active ComfyUI connection")
        api.setBaseUrl(active.hostname, active.port)
    }

    @Suppress("LongMethod")
    private fun buildWorkflow(params: ComfyUIGenerationParams): JsonObject {
        // If custom workflow JSON is provided, use it directly
        val customJson = params.customWorkflowJson
        if (customJson != null) {
            return json.decodeFromString(customJson)
        }

        val loraChain = buildLoraChain(params.loraSelections)
        val finalModelNodeId = loraChain.lastOrNull()?.nodeId ?: "3"
        val finalClipNodeId = loraChain.lastOrNull()?.nodeId ?: "3"
        val finalModelOutput = 0 // MODEL output index (same for CheckpointLoader and LoraLoader)
        val finalClipOutput = 1 // CLIP output index (same for CheckpointLoader and LoraLoader)

        // Positive conditioning source
        val positiveCondId = if (params.controlNetEnabled && params.controlNetModel.isNotBlank()) "21" else "6"

        return buildJsonObject {
            put(
                "3",
                buildJsonObject {
                    put("class_type", "CheckpointLoaderSimple")
                    put("inputs", buildJsonObject { put("ckpt_name", params.checkpoint) })
                }
            )
            loraChain.forEach { loraNode ->
                put(loraNode.nodeId, loraNode.jsonNode)
            }
            put(
                "6",
                buildJsonObject {
                    put("class_type", "CLIPTextEncode")
                    put(
                        "inputs",
                        buildJsonObject {
                            put("text", params.prompt)
                            put("clip", nodeLink(finalClipNodeId, finalClipOutput))
                        }
                    )
                }
            )
            put(
                "7",
                buildJsonObject {
                    put("class_type", "CLIPTextEncode")
                    put(
                        "inputs",
                        buildJsonObject {
                            put("text", params.negativePrompt)
                            put("clip", nodeLink(finalClipNodeId, finalClipOutput))
                        }
                    )
                }
            )
            if (params.controlNetEnabled && params.controlNetModel.isNotBlank()) {
                put(
                    "20",
                    buildJsonObject {
                        put("class_type", "ControlNetLoader")
                        put(
                            "inputs",
                            buildJsonObject { put("control_net_name", params.controlNetModel) }
                        )
                    }
                )
                put(
                    "21",
                    buildJsonObject {
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
                )
            }
            put(
                "5",
                buildJsonObject {
                    put("class_type", "EmptyLatentImage")
                    put(
                        "inputs",
                        buildJsonObject {
                            put("width", params.width)
                            put("height", params.height)
                            put("batch_size", 1)
                        }
                    )
                }
            )
            put(
                "4",
                buildJsonObject {
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
                            put("model", nodeLink(finalModelNodeId, finalModelOutput))
                            put("positive", nodeLink(positiveCondId, 0))
                            put("negative", nodeLink("7", 0))
                            put("latent_image", nodeLink("5", 0))
                        }
                    )
                }
            )
            put(
                "8",
                buildJsonObject {
                    put("class_type", "VAEDecode")
                    put(
                        "inputs",
                        buildJsonObject {
                            put("samples", nodeLink("4", 0))
                            put("vae", nodeLink("3", 2))
                        }
                    )
                }
            )
            put(
                "9",
                buildJsonObject {
                    put("class_type", "SaveImage")
                    put(
                        "inputs",
                        buildJsonObject {
                            put("filename_prefix", "CivitDeck")
                            put("images", nodeLink("8", 0))
                        }
                    )
                }
            )
        }
    }

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
