package com.riox432.civitdeck.feature.externalserver.domain.repository

import com.riox432.civitdeck.feature.externalserver.domain.model.ExternalServerImageFilters
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationChoice
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationJob
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationOption
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
    suspend fun getGenerationOptions(): List<GenerationOption>
    suspend fun getDependentChoices(endpoint: String): List<GenerationChoice>
    suspend fun executeGeneration(params: Map<String, String>): GenerationJob
    suspend fun getGenerationStatus(jobId: String): GenerationJob
    suspend fun deleteImage(cloudKey: String)
    suspend fun deleteImages(cloudKeys: List<String>)
}
