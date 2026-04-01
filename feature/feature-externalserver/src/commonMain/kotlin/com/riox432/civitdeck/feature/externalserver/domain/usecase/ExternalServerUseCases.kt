package com.riox432.civitdeck.feature.externalserver.domain.usecase

import com.riox432.civitdeck.domain.model.ExternalServerConfig
import com.riox432.civitdeck.feature.externalserver.domain.model.ExternalServerImageFilters
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationChoice
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationJob
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationOption
import com.riox432.civitdeck.feature.externalserver.domain.model.PaginatedImagesResponse
import com.riox432.civitdeck.feature.externalserver.domain.model.ServerCapabilities
import com.riox432.civitdeck.feature.externalserver.domain.repository.ExternalServerConfigRepository
import com.riox432.civitdeck.feature.externalserver.domain.repository.ExternalServerImagesRepository
import kotlinx.coroutines.flow.Flow

class ObserveExternalServerConfigsUseCase(
    private val repository: ExternalServerConfigRepository,
) {
    operator fun invoke(): Flow<List<ExternalServerConfig>> = repository.observeConfigs()
}

class ObserveActiveExternalServerConfigUseCase(
    private val repository: ExternalServerConfigRepository,
) {
    operator fun invoke(): Flow<ExternalServerConfig?> = repository.observeActiveConfig()
}

class SaveExternalServerConfigUseCase(
    private val repository: ExternalServerConfigRepository,
) {
    suspend operator fun invoke(config: ExternalServerConfig): Long = repository.saveConfig(config)
}

class DeleteExternalServerConfigUseCase(
    private val repository: ExternalServerConfigRepository,
) {
    suspend operator fun invoke(id: Long) = repository.deleteConfig(id)
}

class ActivateExternalServerConfigUseCase(
    private val repository: ExternalServerConfigRepository,
) {
    suspend operator fun invoke(id: Long) = repository.activateConfig(id)
}

class TestExternalServerConnectionUseCase(
    private val configRepository: ExternalServerConfigRepository,
    private val imagesRepository: ExternalServerImagesRepository,
) {
    suspend operator fun invoke(config: ExternalServerConfig): Boolean {
        val success = imagesRepository.testConnection()
        if (config.id != 0L) {
            configRepository.updateTestResult(config.id, success)
        }
        return success
    }
}

class GetExternalServerCapabilitiesUseCase(
    private val repository: ExternalServerImagesRepository,
) {
    suspend operator fun invoke(): ServerCapabilities = repository.getCapabilities()
}

class GetExternalServerImagesUseCase(
    private val repository: ExternalServerImagesRepository,
) {
    suspend operator fun invoke(
        page: Int,
        perPage: Int,
        filters: ExternalServerImageFilters,
    ): PaginatedImagesResponse = repository.getImages(page, perPage, filters)
}

class GetGenerationOptionsUseCase(
    private val repository: ExternalServerImagesRepository,
) {
    suspend operator fun invoke(): List<GenerationOption> = repository.getGenerationOptions()
}

class GetDependentChoicesUseCase(
    private val repository: ExternalServerImagesRepository,
) {
    suspend operator fun invoke(endpoint: String): List<GenerationChoice> =
        repository.getDependentChoices(endpoint)
}

class ExecuteGenerationUseCase(
    private val repository: ExternalServerImagesRepository,
) {
    suspend operator fun invoke(params: Map<String, String>): GenerationJob =
        repository.executeGeneration(params)
}

class GetGenerationStatusUseCase(
    private val repository: ExternalServerImagesRepository,
) {
    suspend operator fun invoke(jobId: String): GenerationJob =
        repository.getGenerationStatus(jobId)
}

class DeleteServerImagesUseCase(
    private val repository: ExternalServerImagesRepository,
) {
    suspend operator fun invoke(cloudKeys: List<String>) {
        if (cloudKeys.size == 1) {
            repository.deleteImage(cloudKeys.first())
        } else {
            repository.deleteImages(cloudKeys)
        }
    }
}
