package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIGeneratedImage
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.model.QueueJob
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.domain.repository.ComfyUIGenerationRepository
import com.riox432.civitdeck.domain.repository.ComfyUIHistoryRepository
import com.riox432.civitdeck.domain.repository.ComfyUIQueueRepository
import com.riox432.civitdeck.domain.repository.ModelFileHashRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

// -- Connection management --

class ObserveComfyUIConnectionsUseCase(private val repository: ComfyUIConnectionRepository) {
    operator fun invoke(): Flow<List<ComfyUIConnection>> = repository.observeConnections()
}

class ObserveActiveComfyUIConnectionUseCase(private val repository: ComfyUIConnectionRepository) {
    operator fun invoke(): Flow<ComfyUIConnection?> = repository.observeActiveConnection()
}

class SaveComfyUIConnectionUseCase(private val repository: ComfyUIConnectionRepository) {
    suspend operator fun invoke(connection: ComfyUIConnection): Long =
        repository.saveConnection(connection)
}

class DeleteComfyUIConnectionUseCase(private val repository: ComfyUIConnectionRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteConnection(id)
}

class ActivateComfyUIConnectionUseCase(private val repository: ComfyUIConnectionRepository) {
    suspend operator fun invoke(id: Long) = repository.activateConnection(id)
}

class TestComfyUIConnectionUseCase(private val repository: ComfyUIConnectionRepository) {
    suspend operator fun invoke(connection: ComfyUIConnection): Boolean {
        val success = repository.testConnection(connection)
        if (connection.id != 0L) {
            repository.updateTestResult(connection.id, success)
        }
        return success
    }
}

// -- Generation --

class FetchComfyUICheckpointsUseCase(private val repository: ComfyUIGenerationRepository) {
    suspend operator fun invoke(): List<String> = repository.fetchCheckpoints()
}

class FetchComfyUILorasUseCase(private val repository: ComfyUIGenerationRepository) {
    suspend operator fun invoke(): List<String> = repository.fetchLoras()
}

class FetchComfyUIControlNetsUseCase(private val repository: ComfyUIGenerationRepository) {
    suspend operator fun invoke(): List<String> = repository.fetchControlNets()
}

class ImportWorkflowUseCase {
    /**
     * Validates that the given JSON string is a valid ComfyUI workflow (a JSON object with
     * at least one node entry). Returns the cleaned JSON on success, or throws on invalid input.
     */
    operator fun invoke(jsonString: String): String {
        val trimmed = jsonString.trim()
        if (trimmed.isBlank()) error("Workflow JSON is empty")
        val decoded = try {
            Json.parseToJsonElement(trimmed)
        } catch (e: Exception) {
            error("Invalid JSON: ${e.message}")
        }
        if (decoded !is kotlinx.serialization.json.JsonObject) {
            error("Workflow must be a JSON object")
        }
        if (decoded.isEmpty()) error("Workflow JSON has no nodes")
        return trimmed
    }
}

class SubmitComfyUIGenerationUseCase(private val repository: ComfyUIGenerationRepository) {
    suspend operator fun invoke(params: ComfyUIGenerationParams): String =
        repository.submitGeneration(params)
}

class PollComfyUIResultUseCase(private val repository: ComfyUIGenerationRepository) {
    suspend operator fun invoke(promptId: String): GenerationResult =
        repository.pollGenerationResult(promptId)
}

class InterruptComfyUIGenerationUseCase(private val repository: ComfyUIGenerationRepository) {
    suspend operator fun invoke() = repository.interruptGeneration()
}

// -- History --

class FetchComfyUIHistoryUseCase(private val repository: ComfyUIHistoryRepository) {
    operator fun invoke(): Flow<List<ComfyUIGeneratedImage>> = repository.fetchHistory()
}

class FetchComfyUIHistoryItemUseCase(private val repository: ComfyUIHistoryRepository) {
    operator fun invoke(promptId: String): Flow<List<ComfyUIGeneratedImage>> =
        repository.fetchHistoryItem(promptId)
}

// -- Queue management --

class ObserveComfyUIQueueUseCase(private val repository: ComfyUIQueueRepository) {
    operator fun invoke(): Flow<List<QueueJob>> = repository.observeQueue()
}

class CancelComfyUIJobUseCase(private val repository: ComfyUIQueueRepository) {
    suspend operator fun invoke(promptId: String) = repository.cancelJob(promptId)
}

// -- CivitAI model bridge --

class FindMatchingLocalModelUseCase(private val localModelFileRepository: ModelFileHashRepository) {
    /**
     * Returns the file path of the local model file matching the given SHA256 hash, or null
     * if no matching file is found locally.
     */
    suspend operator fun invoke(sha256Hash: String): String? {
        val ownedHashes = localModelFileRepository.getOwnedHashes()
        return if (sha256Hash.uppercase() in ownedHashes.map { it.uppercase() }) {
            sha256Hash
        } else {
            null
        }
    }
}

class PopulateGenerationFromModelUseCase {
    /**
     * Maps CivitAI image generation metadata to [ComfyUIGenerationParams].
     * Falls back to defaults when metadata is absent.
     */
    operator fun invoke(
        prompt: String?,
        negativePrompt: String?,
        steps: Int?,
        cfgScale: Double?,
        seed: Long?,
        sampler: String?,
        checkpointName: String,
    ): ComfyUIGenerationParams = ComfyUIGenerationParams(
        checkpoint = checkpointName,
        prompt = prompt ?: "",
        negativePrompt = negativePrompt ?: "",
        steps = steps ?: ComfyUIGenerationParams.DEFAULT_STEPS,
        cfgScale = cfgScale ?: ComfyUIGenerationParams.DEFAULT_CFG,
        seed = seed ?: -1L,
        samplerName = normalizeSamplerName(sampler),
    )

    private fun normalizeSamplerName(sampler: String?): String {
        if (sampler == null) return ComfyUIGenerationParams.DEFAULT_SAMPLER
        return sampler.lowercase().replace(" ", "_").replace("-", "_")
    }
}
