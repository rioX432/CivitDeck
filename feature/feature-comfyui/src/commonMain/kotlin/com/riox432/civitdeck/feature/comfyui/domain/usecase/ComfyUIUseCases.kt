package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.model.QueueJob
import com.riox432.civitdeck.domain.repository.ComfyUIRepository
import com.riox432.civitdeck.domain.repository.LocalModelFileRepository
import kotlinx.coroutines.flow.Flow

// -- Connection management --

class ObserveComfyUIConnectionsUseCase(private val repository: ComfyUIRepository) {
    operator fun invoke(): Flow<List<ComfyUIConnection>> = repository.observeConnections()
}

class ObserveActiveComfyUIConnectionUseCase(private val repository: ComfyUIRepository) {
    operator fun invoke(): Flow<ComfyUIConnection?> = repository.observeActiveConnection()
}

class SaveComfyUIConnectionUseCase(private val repository: ComfyUIRepository) {
    suspend operator fun invoke(connection: ComfyUIConnection): Long =
        repository.saveConnection(connection)
}

class DeleteComfyUIConnectionUseCase(private val repository: ComfyUIRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteConnection(id)
}

class ActivateComfyUIConnectionUseCase(private val repository: ComfyUIRepository) {
    suspend operator fun invoke(id: Long) = repository.activateConnection(id)
}

class TestComfyUIConnectionUseCase(private val repository: ComfyUIRepository) {
    suspend operator fun invoke(connection: ComfyUIConnection): Boolean {
        val success = repository.testConnection(connection)
        if (connection.id != 0L) {
            repository.updateTestResult(connection.id, success)
        }
        return success
    }
}

// -- Generation --

class FetchComfyUICheckpointsUseCase(private val repository: ComfyUIRepository) {
    suspend operator fun invoke(): List<String> = repository.fetchCheckpoints()
}

class SubmitComfyUIGenerationUseCase(private val repository: ComfyUIRepository) {
    suspend operator fun invoke(params: ComfyUIGenerationParams): String =
        repository.submitGeneration(params)
}

class PollComfyUIResultUseCase(private val repository: ComfyUIRepository) {
    suspend operator fun invoke(promptId: String): GenerationResult =
        repository.pollGenerationResult(promptId)
}

// -- Queue management --

class ObserveComfyUIQueueUseCase(private val repository: ComfyUIRepository) {
    operator fun invoke(): Flow<List<QueueJob>> = repository.observeQueue()
}

class CancelComfyUIJobUseCase(private val repository: ComfyUIRepository) {
    suspend operator fun invoke(promptId: String) = repository.cancelJob(promptId)
}

// -- CivitAI model bridge --

class FindMatchingLocalModelUseCase(private val localModelFileRepository: LocalModelFileRepository) {
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
