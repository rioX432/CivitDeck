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
    ): Flow<GenerationProgress> = observeGenerationProgress(promptId, "http://$host:$port", "ws")

    override fun observeGenerationProgress(
        promptId: String,
        baseUrl: String,
        wsScheme: String,
    ): Flow<GenerationProgress> {
        val clientId = "civitdeck-${currentTimeMillis()}"
        return webSocketApi.observeProgress(baseUrl, wsScheme, clientId, promptId).mapNotNull { msg ->
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

    override suspend fun uploadMaskImage(maskPngBytes: ByteArray): String {
        ensureApiConfigured()
        val filename = "mask_${com.riox432.civitdeck.data.local.currentTimeMillis()}.png"
        val response = api.uploadImage(
            imageBytes = maskPngBytes,
            filename = filename,
            imageType = "input",
        )
        return response.name
    }

    override fun getImageUrl(filename: String, subfolder: String, type: String): String {
        return api.getImageUrl(ComfyUIOutputImage(filename, subfolder, type))
    }

    private suspend fun ensureApiConfigured() {
        val active = dao.getActive() ?: error("No active ComfyUI connection")
        val scheme = if (active.useHttps) "https" else "http"
        api.setBaseUrl("$scheme://${active.hostname}:${active.port}")
    }

    @Suppress("LongMethod")
    private fun buildWorkflow(params: ComfyUIGenerationParams): JsonObject {
        // If custom workflow JSON is provided, use it directly
        val customJson = params.customWorkflowJson
        if (customJson != null) {
            return json.decodeFromString(customJson)
        }

        // Use inpainting workflow when both init image and mask are provided
        val isInpainting = params.initImageFilename != null &&
            params.maskImageFilename != null
        if (isInpainting) {
            return buildInpaintingWorkflow(params)
        }

        val loraChain = buildLoraChain(params.loraSelections)
        val finalModelNodeId = loraChain.lastOrNull()?.nodeId ?: "3"
        val finalClipNodeId = loraChain.lastOrNull()?.nodeId ?: "3"
        val finalModelOutput = 0 // MODEL output index
        val finalClipOutput = 1 // CLIP output index

        // Positive conditioning source
        val positiveCondId = if (params.controlNetEnabled && params.controlNetModel.isNotBlank()) "21" else "6"

        return buildJsonObject {
            put("3", buildCheckpointNode(params.checkpoint))
            loraChain.forEach { loraNode ->
                put(loraNode.nodeId, loraNode.jsonNode)
            }
            put("6", buildClipEncode(params.prompt, finalClipNodeId, finalClipOutput))
            put("7", buildClipEncode(params.negativePrompt, finalClipNodeId, finalClipOutput))
            if (params.controlNetEnabled && params.controlNetModel.isNotBlank()) {
                put("20", buildControlNetLoader(params.controlNetModel))
                put("21", buildControlNetApply(params.controlNetStrength))
            }
            put("5", buildEmptyLatent(params.width, params.height))
            put(
                "4",
                buildKSampler(
                    params = params,
                    modelNodeId = finalModelNodeId,
                    modelOutput = finalModelOutput,
                    positiveCondId = positiveCondId,
                    latentNodeId = "5",
                    denoise = 1.0,
                ),
            )
            put("8", buildVaeDecode(samplerNodeId = "4", vaeSourceId = "3"))
            put("9", buildSaveImage(imageNodeId = "8"))
        }
    }

    @Suppress("LongMethod")
    private fun buildInpaintingWorkflow(params: ComfyUIGenerationParams): JsonObject {
        val loraChain = buildLoraChain(params.loraSelections)
        val finalModelNodeId = loraChain.lastOrNull()?.nodeId ?: "3"
        val finalClipNodeId = loraChain.lastOrNull()?.nodeId ?: "3"
        val finalModelOutput = 0
        val finalClipOutput = 1

        return buildJsonObject {
            // Checkpoint loader
            put("3", buildCheckpointNode(params.checkpoint))
            loraChain.forEach { loraNode ->
                put(loraNode.nodeId, loraNode.jsonNode)
            }
            // Load init image
            put(
                "30",
                buildJsonObject {
                    put("class_type", "LoadImage")
                    put(
                        "inputs",
                        buildJsonObject {
                            put("image", requireNotNull(params.initImageFilename))
                        }
                    )
                },
            )
            // Load mask image
            put(
                "31",
                buildJsonObject {
                    put("class_type", "LoadImage")
                    put(
                        "inputs",
                        buildJsonObject {
                            put("image", requireNotNull(params.maskImageFilename))
                        }
                    )
                },
            )
            // Set latent via VAEEncode with mask
            put(
                "32",
                buildJsonObject {
                    put("class_type", "VAEEncodeForInpaint")
                    put(
                        "inputs",
                        buildJsonObject {
                            put("pixels", nodeLink("30", 0))
                            put("vae", nodeLink("3", 2))
                            put("mask", nodeLink("31", 0))
                            put("grow_mask_by", 6)
                        }
                    )
                },
            )
            // Conditioning
            put("6", buildClipEncode(params.prompt, finalClipNodeId, finalClipOutput))
            put("7", buildClipEncode(params.negativePrompt, finalClipNodeId, finalClipOutput))
            // KSampler with lower denoise for inpainting
            put(
                "4",
                buildKSampler(
                    params = params,
                    modelNodeId = finalModelNodeId,
                    modelOutput = finalModelOutput,
                    positiveCondId = "6",
                    latentNodeId = "32",
                    denoise = params.denoiseStrength,
                ),
            )
            // VAE Decode
            put("8", buildVaeDecode(samplerNodeId = "4", vaeSourceId = "3"))
            // Save
            put("9", buildSaveImage(imageNodeId = "8"))
        }
    }

    // -- Workflow node builder helpers --

    private fun buildCheckpointNode(checkpoint: String) = buildJsonObject {
        put("class_type", "CheckpointLoaderSimple")
        put("inputs", buildJsonObject { put("ckpt_name", checkpoint) })
    }

    private fun buildClipEncode(text: String, clipNodeId: String, clipOutput: Int) =
        buildJsonObject {
            put("class_type", "CLIPTextEncode")
            put(
                "inputs",
                buildJsonObject {
                    put("text", text)
                    put("clip", nodeLink(clipNodeId, clipOutput))
                }
            )
        }

    private fun buildControlNetLoader(model: String) = buildJsonObject {
        put("class_type", "ControlNetLoader")
        put("inputs", buildJsonObject { put("control_net_name", model) })
    }

    private fun buildControlNetApply(strength: Float) = buildJsonObject {
        put("class_type", "ControlNetApply")
        put(
            "inputs",
            buildJsonObject {
                put("conditioning", nodeLink("6", 0))
                put("control_net", nodeLink("20", 0))
                put("image", buildJsonArray { })
                put("strength", strength.toDouble())
            }
        )
    }

    private fun buildEmptyLatent(width: Int, height: Int) = buildJsonObject {
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

    @Suppress("LongParameterList")
    private fun buildKSampler(
        params: ComfyUIGenerationParams,
        modelNodeId: String,
        modelOutput: Int,
        positiveCondId: String,
        latentNodeId: String,
        denoise: Double,
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
                put("denoise", denoise)
                put("model", nodeLink(modelNodeId, modelOutput))
                put("positive", nodeLink(positiveCondId, 0))
                put("negative", nodeLink("7", 0))
                put("latent_image", nodeLink(latentNodeId, 0))
            }
        )
    }

    private fun buildVaeDecode(samplerNodeId: String, vaeSourceId: String) =
        buildJsonObject {
            put("class_type", "VAEDecode")
            put(
                "inputs",
                buildJsonObject {
                    put("samples", nodeLink(samplerNodeId, 0))
                    put("vae", nodeLink(vaeSourceId, 2))
                }
            )
        }

    private fun buildSaveImage(imageNodeId: String) = buildJsonObject {
        put("class_type", "SaveImage")
        put(
            "inputs",
            buildJsonObject {
                put("filename_prefix", "CivitDeck")
                put("images", nodeLink(imageNodeId, 0))
            }
        )
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
