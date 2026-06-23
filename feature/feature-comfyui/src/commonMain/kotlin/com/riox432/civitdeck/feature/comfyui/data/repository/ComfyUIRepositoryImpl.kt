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
import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.domain.model.GenerationProgress
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.model.GenerationStatus
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
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "ComfyUIRepositoryImpl"

class ComfyUIRepositoryImpl(
    private val dao: ComfyUIConnectionDao,
    private val api: ComfyUIApi,
    private val webSocketApi: ComfyUIWebSocketApi,
    private val json: Json,
) : ComfyUIConnectionRepository, ComfyUIGenerationRepository, ComfyUIQueueRepository {

    private val workflowBuilder = ComfyUIWorkflowBuilder(json)

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
        api.setBaseUrl(connection.baseUrl)
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

    override fun getImageUrl(filename: String, subfolder: String, type: String): String {
        return api.getImageUrl(ComfyUIOutputImage(filename, subfolder, type))
    }

    override suspend fun interruptGeneration() {
        ensureApiConfigured()
        api.interrupt()
    }

    override suspend fun uploadMaskImage(maskPngBytes: ByteArray): String {
        ensureApiConfigured()
        val filename = "mask_${currentTimeMillis()}.png"
        val response = api.uploadImage(
            imageBytes = maskPngBytes,
            filename = filename,
            imageType = "input",
        )
        return response.name
    }

    override suspend fun fetchObjectInfo(): String {
        ensureApiConfigured()
        return api.getFullObjectInfo()
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
        val active = dao.getActive()
            ?: throw DomainException.ConnectionException("No active ComfyUI connection")
        api.setBaseUrl(buildBaseUrl(active))
    }

    private fun buildBaseUrl(entity: ComfyUIConnectionEntity): String {
        val scheme = if (entity.useHttps) "https" else "http"
        return "$scheme://${entity.hostname}:${entity.port}"
    }

    private fun buildWorkflow(params: ComfyUIGenerationParams): JsonObject =
        workflowBuilder.buildWorkflow(params)

    private fun ComfyUIConnectionEntity.toDomain() = ComfyUIConnection(
        id = id,
        name = name,
        hostname = hostname,
        port = port,
        isActive = isActive,
        lastTestedAt = lastTestedAt,
        lastTestSuccess = lastTestSuccess,
        useHttps = useHttps,
        acceptSelfSigned = acceptSelfSigned,
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
        useHttps = useHttps,
        acceptSelfSigned = acceptSelfSigned,
    )
}
