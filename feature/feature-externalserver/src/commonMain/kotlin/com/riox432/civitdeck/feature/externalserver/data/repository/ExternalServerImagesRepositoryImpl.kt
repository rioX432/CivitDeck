package com.riox432.civitdeck.feature.externalserver.data.repository

import com.riox432.civitdeck.data.api.externalserver.ExternalServerApi
import com.riox432.civitdeck.data.api.externalserver.ServerImageDto
import com.riox432.civitdeck.data.local.dao.ExternalServerConfigDao
import com.riox432.civitdeck.feature.externalserver.domain.model.ExternalServerImageFilters
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
}

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
