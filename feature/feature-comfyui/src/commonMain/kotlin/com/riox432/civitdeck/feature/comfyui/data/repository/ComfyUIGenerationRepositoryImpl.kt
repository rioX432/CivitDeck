package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIOutputImage
import com.riox432.civitdeck.data.api.comfyui.ComfyUIWebSocketMessage
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.DiffusionLatentNode
import com.riox432.civitdeck.domain.model.DiffusionModelResources
import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.domain.model.GenerationProgress
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.model.LoraSelection
import com.riox432.civitdeck.domain.repository.ComfyUIGenerationRepository
import com.riox432.civitdeck.feature.comfyui.data.ComfyUIApiProvider
import com.riox432.civitdeck.feature.comfyui.data.ComfyUIEndpoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.concurrent.Volatile
import kotlin.random.Random

class ComfyUIGenerationRepositoryImpl(
    private val apiProvider: ComfyUIApiProvider,
    private val json: Json,
) : ComfyUIGenerationRepository {

    // ComfyUI sends a job's completion events only to the socket whose clientId matches the
    // client_id submitted with the prompt, so submission and progress must share this id.
    private val clientId = "civitdeck-${Random.nextLong(0, Long.MAX_VALUE)}"

    // Only for getImageUrl, which cannot suspend to read the active row.
    @Volatile
    private var lastEndpoint: ComfyUIEndpoint? = null

    override suspend fun fetchCheckpoints(): List<String> = activeEndpoint().api.getCheckpoints()

    override suspend fun fetchLoras(): List<String> = activeEndpoint().api.getLoras()

    override suspend fun fetchControlNets(): List<String> = activeEndpoint().api.getControlNets()

    override suspend fun fetchDiffusionModelResources(): DiffusionModelResources {
        val api = activeEndpoint().api
        return DiffusionModelResources(
            diffusionModels = api.getDiffusionModels(),
            textEncoders = api.getTextEncoders(),
            vaes = api.getVaes(),
            clipTypes = api.getClipTypes(),
        )
    }

    override suspend fun submitGeneration(params: ComfyUIGenerationParams): String {
        val api = activeEndpoint().api
        val workflow = buildWorkflow(params)
        val response = api.submitPrompt(workflow, clientId)
        return response.promptId
    }

    override suspend fun pollGenerationResult(promptId: String): GenerationResult {
        val api = activeEndpoint().api
        val entry = api.getHistory(promptId)
            ?: return GenerationResult(promptId, GenerationStatus.Running)

        // ComfyUI marks both a failed and an interrupted job `error` with `completed: false`.
        val status = entry.status
        if (status?.statusStr == "error") {
            return GenerationResult(promptId, GenerationStatus.Error, error = status.executionErrorMessage)
        }

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

    /**
     * [baseUrl] is the URL of the connection the job was submitted on, and its trust is looked
     * up by host and port. The socket scheme is derived from [baseUrl], which [wsScheme] mirrors.
     */
    override fun observeGenerationProgress(
        promptId: String,
        baseUrl: String,
        wsScheme: String,
    ): Flow<GenerationProgress> {
        val messages = flow { emitAll(apiProvider.forUrl(baseUrl).observeProgress(clientId, promptId)) }
        return messages.mapNotNull { msg ->
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
        activeEndpoint().api.interrupt()
    }

    override suspend fun uploadMaskImage(maskPngBytes: ByteArray): String {
        val api = activeEndpoint().api
        val filename = "mask_${com.riox432.civitdeck.data.local.currentTimeMillis()}.png"
        val response = api.uploadImage(
            imageBytes = maskPngBytes,
            filename = filename,
            imageType = "input",
        )
        return response.name
    }

    /** Builds the URL on the server of this repository's latest call. */
    override fun getImageUrl(filename: String, subfolder: String, type: String): String {
        val endpoint = lastEndpoint
            ?: throw DomainException.ConnectionException("No active ComfyUI connection")
        return endpoint.api.getImageUrl(ComfyUIOutputImage(filename, subfolder, type))
    }

    override suspend fun fetchObjectInfo(): String = activeEndpoint().api.getFullObjectInfo()

    private suspend fun activeEndpoint(): ComfyUIEndpoint =
        apiProvider.forActive().also { lastEndpoint = it }

    private fun buildWorkflow(params: ComfyUIGenerationParams): JsonObject {
        // If custom workflow JSON is provided, use it directly
        val customJson = params.customWorkflowJson
        if (customJson != null) {
            return json.decodeFromString(customJson)
        }

        val hasControlNet = params.controlNetEnabled && params.controlNetModel.isNotBlank()
        requireCheckpointOnlyInputsUnset(params, hasControlNet)
        // ControlNetApply needs a control image linked into its `image` input and the params carry
        // none; ComfyUI rejects a prompt without that link at validation (bad_linked_input).
        require(!hasControlNet) { "ControlNet needs a control image, which cannot be sent yet" }

        // Use inpainting workflow when both init image and mask are provided
        val isInpainting = params.initImageFilename != null &&
            params.maskImageFilename != null
        if (isInpainting) {
            return buildInpaintingWorkflow(params)
        }

        val loaders = if (params.diffusionModel != null) splitLoaderOutputs else checkpointOutputs
        val loraChain = buildLoraChain(params.loraSelections, loaders)
        val finalModel = loraChain.lastOrNull()?.let { nodeLink(it.nodeId, 0) } ?: loaders.model
        val finalClip = loraChain.lastOrNull()?.let { nodeLink(it.nodeId, 1) } ?: loaders.clip
        val latentNode = params.diffusionModel?.latentNode ?: DiffusionLatentNode.EMPTY_LATENT_IMAGE

        return buildJsonObject {
            putLoaders(params)
            loraChain.forEach { loraNode ->
                put(loraNode.nodeId, loraNode.jsonNode)
            }
            val samplerModel = putModelSampling(params.diffusionModel?.auraFlowShift, finalModel)
            put("6", buildClipEncode(params.prompt, finalClip))
            put("7", buildClipEncode(params.negativePrompt, finalClip))
            put("5", buildEmptyLatent(latentNode, params.width, params.height))
            put(
                "4",
                buildKSampler(
                    params = params,
                    model = samplerModel,
                    positiveCondId = "6",
                    latentNodeId = "5",
                    denoise = 1.0,
                ),
            )
            put("8", buildVaeDecode(samplerNodeId = "4", vae = loaders.vae))
            put("9", buildSaveImage(imageNodeId = "8"))
        }
    }

    private fun buildInpaintingWorkflow(params: ComfyUIGenerationParams): JsonObject {
        val loraChain = buildLoraChain(params.loraSelections, checkpointOutputs)
        val finalModel = loraChain.lastOrNull()?.let { nodeLink(it.nodeId, 0) } ?: checkpointOutputs.model
        val finalClip = loraChain.lastOrNull()?.let { nodeLink(it.nodeId, 1) } ?: checkpointOutputs.clip

        return buildJsonObject {
            // Checkpoint loader
            put("3", buildCheckpointNode(params.checkpoint))
            loraChain.forEach { loraNode ->
                put(loraNode.nodeId, loraNode.jsonNode)
            }
            // Load init image
            put("30", buildLoadImageNode(requireNotNull(params.initImageFilename)))
            // Load mask image
            put("31", buildLoadImageNode(requireNotNull(params.maskImageFilename)))
            // Set latent via VAEEncode with mask
            put("32", buildVaeEncodeForInpaint())
            // Conditioning
            put("6", buildClipEncode(params.prompt, finalClip))
            put("7", buildClipEncode(params.negativePrompt, finalClip))
            // KSampler with lower denoise for inpainting
            put(
                "4",
                buildKSampler(
                    params = params,
                    model = finalModel,
                    positiveCondId = "6",
                    latentNodeId = "32",
                    denoise = params.denoiseStrength,
                ),
            )
            // VAE Decode
            put("8", buildVaeDecode(samplerNodeId = "4", vae = checkpointOutputs.vae))
            // Save
            put("9", buildSaveImage(imageNodeId = "8"))
        }
    }

    // -- Workflow node builder helpers --

    /** MODEL, CLIP and VAE outputs the graph reads before any LoRA is applied. */
    private class LoaderOutputs(val model: JsonArray, val clip: JsonArray, val vae: JsonArray)

    private val checkpointOutputs = LoaderOutputs(nodeLink("3", 0), nodeLink("3", 1), nodeLink("3", 2))

    // The model stays at node "3" so the LoRA chain and KSampler wiring match the checkpoint graph.
    private val splitLoaderOutputs = LoaderOutputs(nodeLink("3", 0), nodeLink("40", 0), nodeLink("41", 0))

    private fun JsonObjectBuilder.putLoaders(params: ComfyUIGenerationParams) {
        val diffusionModel = params.diffusionModel
        if (diffusionModel == null) {
            put("3", buildCheckpointNode(params.checkpoint))
            return
        }
        put(
            "3",
            buildJsonObject {
                put("class_type", "UNETLoader")
                put(
                    "inputs",
                    buildJsonObject {
                        put("unet_name", diffusionModel.unetName)
                        // Both official Krea 2 templates, int8 included, use "default": ComfyUI
                        // detects quantized weights from the file.
                        put("weight_dtype", "default")
                    },
                )
            },
        )
        put(
            "40",
            buildJsonObject {
                put("class_type", "CLIPLoader")
                put(
                    "inputs",
                    buildJsonObject {
                        put("clip_name", diffusionModel.textEncoderName)
                        put("type", diffusionModel.clipType)
                    },
                )
            },
        )
        put(
            "41",
            buildJsonObject {
                put("class_type", "VAELoader")
                put("inputs", buildJsonObject { put("vae_name", diffusionModel.vaeName) })
            },
        )
    }

    private fun buildCheckpointNode(checkpoint: String) = buildJsonObject {
        put("class_type", "CheckpointLoaderSimple")
        put("inputs", buildJsonObject { put("ckpt_name", checkpoint) })
    }

    private fun buildLoadImageNode(filename: String) = buildJsonObject {
        put("class_type", "LoadImage")
        put("inputs", buildJsonObject { put("image", filename) })
    }

    private fun buildVaeEncodeForInpaint() = buildJsonObject {
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
    }

    private fun buildClipEncode(text: String, clip: JsonArray) =
        buildJsonObject {
            put("class_type", "CLIPTextEncode")
            put(
                "inputs",
                buildJsonObject {
                    put("text", text)
                    put("clip", clip)
                }
            )
        }

    /**
     * Adds `ModelSamplingAuraFlow` between [model] and the KSampler when [auraFlowShift] is set,
     * and returns the MODEL link the KSampler reads. It is fed by the end of the LoRA chain, so
     * the chain still starts at the UNET in node "3".
     */
    private fun JsonObjectBuilder.putModelSampling(auraFlowShift: Double?, model: JsonArray): JsonArray {
        if (auraFlowShift == null) return model
        put(
            "42",
            buildJsonObject {
                put("class_type", "ModelSamplingAuraFlow")
                put(
                    "inputs",
                    buildJsonObject {
                        put("model", model)
                        put("shift", auraFlowShift)
                    }
                )
            },
        )
        return nodeLink("42", 0)
    }

    // Both latent nodes take the same width, height and batch_size inputs.
    private fun buildEmptyLatent(node: DiffusionLatentNode, width: Int, height: Int) = buildJsonObject {
        put("class_type", node.classType)
        put(
            "inputs",
            buildJsonObject {
                put("width", width)
                put("height", height)
                put("batch_size", 1)
            }
        )
    }

    private fun buildKSampler(
        params: ComfyUIGenerationParams,
        model: JsonArray,
        positiveCondId: String,
        latentNodeId: String,
        denoise: Double,
    ) = buildJsonObject {
        put("class_type", "KSampler")
        put(
            "inputs",
            buildJsonObject {
                // ComfyUI's KSampler requires seed >= 0; the app's "random" sentinel is -1.
                val seed = if (params.seed < 0) Random.nextLong(0, Long.MAX_VALUE) else params.seed
                put("seed", seed)
                put("steps", params.steps)
                put("cfg", params.cfgScale)
                put("sampler_name", params.samplerName)
                put("scheduler", params.scheduler)
                put("denoise", denoise)
                put("model", model)
                put("positive", nodeLink(positiveCondId, 0))
                put("negative", nodeLink("7", 0))
                put("latent_image", nodeLink(latentNodeId, 0))
            }
        )
    }

    private fun buildVaeDecode(samplerNodeId: String, vae: JsonArray) =
        buildJsonObject {
            put("class_type", "VAEDecode")
            put(
                "inputs",
                buildJsonObject {
                    put("samples", nodeLink(samplerNodeId, 0))
                    put("vae", vae)
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

    /** The first LoRA reads [loaders]; each later one reads the LoRA before it. */
    private fun buildLoraChain(loras: List<LoraSelection>, loaders: LoaderOutputs): List<LoraNodeEntry> {
        if (loras.isEmpty()) return emptyList()
        val entries = mutableListOf<LoraNodeEntry>()
        loras.forEachIndexed { index, lora ->
            val nodeId = (10 + index).toString()
            val prevNodeId = entries.lastOrNull()?.nodeId
            val node = buildJsonObject {
                put("class_type", "LoraLoader")
                put(
                    "inputs",
                    buildJsonObject {
                        put("lora_name", lora.name)
                        put("strength_model", lora.strengthModel.toDouble())
                        put("strength_clip", lora.strengthClip.toDouble())
                        put("model", prevNodeId?.let { nodeLink(it, 0) } ?: loaders.model)
                        put("clip", prevNodeId?.let { nodeLink(it, 1) } ?: loaders.clip)
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

private fun requireCheckpointOnlyInputsUnset(params: ComfyUIGenerationParams, hasControlNet: Boolean) {
    if (params.diffusionModel == null) return
    // The ControlNet and inpainting graphs exist only for checkpoints; building either
    // without its nodes would submit a different generation than the user asked for.
    require(!hasControlNet) { "ControlNet cannot be combined with a diffusion model" }
    require(params.maskImageFilename == null) { "Inpainting cannot be combined with a diffusion model" }
}
