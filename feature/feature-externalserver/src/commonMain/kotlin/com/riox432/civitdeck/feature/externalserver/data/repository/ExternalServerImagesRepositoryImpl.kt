package com.riox432.civitdeck.feature.externalserver.data.repository

import com.riox432.civitdeck.data.api.externalserver.ExternalServerApi
import com.riox432.civitdeck.data.api.externalserver.GenerationChoiceDto
import com.riox432.civitdeck.data.api.externalserver.GenerationOptionDto
import com.riox432.civitdeck.data.api.externalserver.ServerImageDto
import com.riox432.civitdeck.data.local.dao.ExternalServerConfigDao
import com.riox432.civitdeck.feature.externalserver.domain.model.ExternalServerImageFilters
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationChoice
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationJob
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationJobStatus
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationOption
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationOptionType
import com.riox432.civitdeck.feature.externalserver.domain.model.PaginatedImagesResponse
import com.riox432.civitdeck.feature.externalserver.domain.model.ServerCapabilities
import com.riox432.civitdeck.feature.externalserver.domain.model.ServerImage
import com.riox432.civitdeck.feature.externalserver.domain.repository.ExternalServerImagesRepository

class ExternalServerImagesRepositoryImpl(
    private val api: ExternalServerApi,
    private val dao: ExternalServerConfigDao,
) : ExternalServerImagesRepository {

    private suspend fun ensureApiConfigured() {
        val active = dao.getActive() ?: error("No active external server config")
        api.configure(active.baseUrl.trimEnd('/'), active.apiKey)
    }

    override suspend fun getCapabilities(): ServerCapabilities {
        ensureApiConfigured()
        val dto = api.getCapabilities()
        return ServerCapabilities(
            endpoints = dto.endpoints,
            name = dto.name,
            version = dto.version,
        )
    }

    override suspend fun getImages(
        page: Int,
        perPage: Int,
        filters: ExternalServerImageFilters,
    ): PaginatedImagesResponse {
        ensureApiConfigured()
        val dto = api.getImages(page, perPage, filters.toMap())
        return PaginatedImagesResponse(
            images = dto.images.map { it.toDomain() },
            total = dto.total,
            page = dto.page,
            perPage = dto.perPage,
            totalPages = dto.totalPages,
        )
    }

    override suspend fun testConnection(): Boolean {
        ensureApiConfigured()
        return api.testConnection()
    }

    override suspend fun getGenerationOptions(): List<GenerationOption> {
        ensureApiConfigured()
        return api.getGenerationOptions().options.map { it.toDomain() }
    }

    override suspend fun getDependentChoices(endpoint: String): List<GenerationChoice> {
        ensureApiConfigured()
        return api.getDependentChoices(endpoint).map { it.toDomain() }
    }

    override suspend fun executeGeneration(params: Map<String, String>): GenerationJob {
        ensureApiConfigured()
        val dto = api.executeGeneration(params)
        return GenerationJob(
            jobId = dto.jobId,
            status = GenerationJobStatus.fromString(dto.status),
            message = dto.message,
        )
    }

    override suspend fun getGenerationStatus(jobId: String): GenerationJob {
        ensureApiConfigured()
        val dto = api.getGenerationStatus(jobId)
        return GenerationJob(
            jobId = dto.jobId,
            status = GenerationJobStatus.fromString(dto.status),
            progress = dto.progress,
            completed = dto.completed,
            total = dto.total,
            message = dto.message,
        )
    }
}

private fun GenerationOptionDto.toDomain() = GenerationOption(
    key = key,
    label = label,
    type = GenerationOptionType.fromString(type),
    choices = choices.map { it.toDomain() },
    dependsOn = dependsOn,
    choicesEndpoint = choicesEndpoint,
    placeholder = placeholder,
    defaultValue = default?.content,
    min = min,
    max = max,
)

private fun GenerationChoiceDto.toDomain() = GenerationChoice(
    value = value,
    label = label,
    description = description,
)

private fun ServerImageDto.toDomain() = ServerImage(
    id = id,
    file = file,
    thumbUrl = thumbUrl,
    character = character,
    costume = costume,
    scenario = scenario,
    nsfw = nsfw,
    selected = selected,
    postStatus = postStatus,
    aestheticScore = aestheticScore,
    createdAt = createdAt,
    seed = seed,
    prompt = prompt,
)
