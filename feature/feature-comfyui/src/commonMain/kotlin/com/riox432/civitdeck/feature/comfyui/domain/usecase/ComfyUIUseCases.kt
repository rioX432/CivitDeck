package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.repository.ComfyUIRepository
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
