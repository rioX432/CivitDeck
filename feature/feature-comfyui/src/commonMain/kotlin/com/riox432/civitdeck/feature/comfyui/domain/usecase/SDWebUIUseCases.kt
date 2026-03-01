package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.SDWebUIConnection
import com.riox432.civitdeck.domain.model.SDWebUIGenerationParams
import com.riox432.civitdeck.domain.model.SDWebUIGenerationProgress
import com.riox432.civitdeck.domain.repository.SDWebUIAssetRepository
import com.riox432.civitdeck.domain.repository.SDWebUIConnectionRepository
import com.riox432.civitdeck.domain.repository.SDWebUIGenerationRepository
import kotlinx.coroutines.flow.Flow

class ObserveSDWebUIConnectionsUseCase(private val repository: SDWebUIConnectionRepository) {
    operator fun invoke(): Flow<List<SDWebUIConnection>> = repository.observeConnections()
}

class ObserveActiveSDWebUIConnectionUseCase(private val repository: SDWebUIConnectionRepository) {
    operator fun invoke(): Flow<SDWebUIConnection?> = repository.observeActiveConnection()
}

class SaveSDWebUIConnectionUseCase(private val repository: SDWebUIConnectionRepository) {
    suspend operator fun invoke(connection: SDWebUIConnection): Long =
        repository.saveConnection(connection)
}

class DeleteSDWebUIConnectionUseCase(private val repository: SDWebUIConnectionRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteConnection(id)
}

class ActivateSDWebUIConnectionUseCase(private val repository: SDWebUIConnectionRepository) {
    suspend operator fun invoke(id: Long) = repository.activateConnection(id)
}

class TestSDWebUIConnectionUseCase(private val repository: SDWebUIConnectionRepository) {
    suspend operator fun invoke(connection: SDWebUIConnection): Boolean {
        val success = repository.testConnection(connection)
        if (connection.id != 0L) repository.updateTestResult(connection.id, success)
        return success
    }
}

class FetchSDWebUIModelsUseCase(private val repository: SDWebUIAssetRepository) {
    suspend operator fun invoke(): List<String> = repository.fetchModels()
}

class FetchSDWebUISamplersUseCase(private val repository: SDWebUIAssetRepository) {
    suspend operator fun invoke(): List<String> = repository.fetchSamplers()
}

class FetchSDWebUIVaesUseCase(private val repository: SDWebUIAssetRepository) {
    suspend operator fun invoke(): List<String> = repository.fetchVaes()
}

class GenerateSDWebUIImageUseCase(private val repository: SDWebUIGenerationRepository) {
    operator fun invoke(params: SDWebUIGenerationParams): Flow<SDWebUIGenerationProgress> =
        repository.generateImage(params)
}

class InterruptSDWebUIGenerationUseCase(private val repository: SDWebUIGenerationRepository) {
    suspend operator fun invoke() = repository.interruptGeneration()
}
