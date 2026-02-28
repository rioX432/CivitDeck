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
import com.riox432.civitdeck.domain.repository.ComfyUIRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ComfyUIRepositoryImpl(
    private val dao: ComfyUIConnectionDao,
    private val api: ComfyUIApi,
    private val webSocketApi: ComfyUIWebSocketApi,
) : ComfyUIRepository {

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

    override suspend fun deleteConnection(id: Long) = dao.deleteById(id)

    override suspend fun activateConnection(id: Long) {
        dao.deactivateAll()
        dao.activate(id)
    }

    override suspend fun testConnection(connection: ComfyUIConnection): Boolean {
        api.setBaseUrl(connection.hostname, connection.port)
        return try {
            api.getQueue()
            true
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
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
                else -> null
            }
        }
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
        return buildJsonObject {
            put(
                "3",
                buildJsonObject {
                    put("class_type", "CheckpointLoaderSimple")
                    put(
                        "inputs",
                        buildJsonObject {
                            put("ckpt_name", params.checkpoint)
                        }
                    )
                }
            )
            put(
                "6",
                buildJsonObject {
                    put("class_type", "CLIPTextEncode")
                    put(
                        "inputs",
                        buildJsonObject {
                            put("text", params.prompt)
                            put("clip", nodeLink("3", 1))
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
                            put("clip", nodeLink("3", 1))
                        }
                    )
                }
            )
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
                            put("model", nodeLink("3", 0))
                            put("positive", nodeLink("6", 0))
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
