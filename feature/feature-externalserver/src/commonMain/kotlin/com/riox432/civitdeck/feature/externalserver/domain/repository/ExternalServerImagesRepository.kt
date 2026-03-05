package com.riox432.civitdeck.feature.externalserver.domain.repository

import com.riox432.civitdeck.feature.externalserver.domain.model.ExternalServerImageFilters
import com.riox432.civitdeck.feature.externalserver.domain.model.PaginatedImagesResponse
import com.riox432.civitdeck.feature.externalserver.domain.model.ServerCapabilities

interface ExternalServerImagesRepository {
    suspend fun getCapabilities(): ServerCapabilities
    suspend fun getImages(
        page: Int,
        perPage: Int,
        filters: ExternalServerImageFilters,
    ): PaginatedImagesResponse
    suspend fun testConnection(): Boolean
}
