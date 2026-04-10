package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.api.comfyui.ComfyUIOutputImage
import com.riox432.civitdeck.data.api.comfyui.ComfyUIWebSocketApi
import com.riox432.civitdeck.data.api.comfyui.ComfyUIWebSocketMessage
import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.ComfyUIConnectionDao
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationProgress
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.model.LoraSelection
import com.riox432.civitdeck.domain.model.QueueJob
import com.riox432.civitdeck.domain.model.QueueJobStatus
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.domain.repository.ComfyUIGenerationRepository
import com.riox432.civitdeck.domain.repository.ComfyUIQueueRepository
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private const val TAG = "ComfyUIRepositoryImpl"

@Suppress("TooManyFunctions")
class ComfyUIRepositoryImpl(
    private val dao: ComfyUIConnectionDao,
    private val api: ComfyUIApi,
    private val webSocketApi: ComfyUIWebSocketApi,
    private val json: Json,
) : ComfyUIConnectionRepository, ComfyUIGenerationRepository, ComfyUIQueueRepository {

    override fun observeConnections(): Flow<List<ComfyUIConnection>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeActiveConnection(): Flow<ComfyUIConnection?> =
        dao.observeActive().map { it?.toDomain() }

    override suspend fun getActiveConnection(): ComfyUIConnection? =
        dao.getActive()?.toDomain()

    override suspend fun saveConnection(connection: ComfyUIConnection): Long {
        val entity = connection.toEntity()
        return if (connection.id == 0L) {
            val id = dao.insert(entity)
            // If this is the first connection, activate it
            if (dao.getActive() == null) {
                dao.activate(id)
            }
            id
        } else {
            dao.update(entity)
            connection.id
        }
    }

    override suspend fun deleteConnection(id: Long) { dao.deleteById(id) }

    override suspend fun activateConnection(id: Long) {
        dao.deactivateAll()
        dao.activate(id)
    }

    override suspend fun testConnection(connection: ComfyUIConnection): Boolean {
        api.setBaseUrl(connection.hostname, connection.port)
        return try {
            api.getQueue()
            true
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(TAG, "Connection test failed: ${e.message}")
            false
        }
    }

    override suspend fun updateTestResult(id: Long, success: Boolean) {
        dao.updateTestResult(id, currentTimeMillis(), success)
    }

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

    override fun getImageUrl(filename: String, subfolder: String, type: String): String {
        return api.getImageUrl(ComfyUIOutputImage(filename, subfolder, type))
    }

    override suspend fun interruptGeneration() {
        ensureApiConfigured()
        api.interrupt()
    }

    override fun observeQueue(intervalMs: Long): Flow<List<QueueJob>> = flow {
        while (true) {
            try {
                ensureApiConfigured()
                val response = api.getQueue()
                val jobs = mutableListOf<QueueJob>()
                response.running.forEachIndexed { index, entry ->
                    jobs.add(QueueJob(extractPromptId(entry), index, QueueJobStatus.Running))
                }
                response.pending.forEachIndexed { index, entry ->
                    jobs.add(QueueJob(extractPromptId(entry), index, QueueJobStatus.Queued))
                }
                emit(jobs)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w(TAG, "Failed to poll queue: ${e.message}")
                throw e
            }
            delay(intervalMs)
        }
    }

    override suspend fun cancelJob(promptId: String) {
        ensureApiConfigured()
        api.deleteQueue(listOf(promptId))
    }

    /**
     * ComfyUI queue entries are arrays: [queue_number, prompt_id, prompt, extra_data, outputs].
     * Extract prompt_id (index 1) from the array element.
     */
    private fun extractPromptId(entry: JsonElement): String {
        return try {
            when (entry) {
                is JsonArray -> entry.getOrNull(1)?.jsonPrimitive?.content ?: ""
                is JsonObject -> entry["prompt_id"]?.jsonPrimitive?.content ?: ""
                else -> entry.jsonPrimitive.content
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(TAG, "Failed to extract prompt ID: ${e.message}")
            ""
        }
    }

    private suspend fun ensureApiConfigured() {
        val active = dao.getActive() ?: error("No active ComfyUI connection")
        api.setBaseUrl(active.hostname, active.port)
    }

    private fun buildWorkflow(params: ComfyUIGenerationParams): JsonObject {
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

    private fun ComfyUIConnectionEntity.toDomain() = ComfyUIConnection(
        id = id,
        name = name,
        hostname = hostname,
        port = port,
        isActive = isActive,
        lastTestedAt = lastTestedAt,
        lastTestSuccess = lastTestSuccess,
    )

    private fun ComfyUIConnection.toEntity() = ComfyUIConnectionEntity(
        id = id,
        name = name,
        hostname = hostname,
        port = port,
        isActive = isActive,
        lastTestedAt = lastTestedAt,
        lastTestSuccess = lastTestSuccess,
        createdAt = currentTimeMillis(),
    )
}
