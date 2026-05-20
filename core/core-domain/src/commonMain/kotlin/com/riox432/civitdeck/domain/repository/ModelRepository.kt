package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelLicenseInfo
import com.riox432.civitdeck.domain.model.ModelSearchQuery
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.PaginatedResult

interface ModelRepository {
    suspend fun getModels(query: ModelSearchQuery = ModelSearchQuery()): PaginatedResult<Model>

    suspend fun getModel(id: Long): Model

    suspend fun getModelVersion(id: Long): ModelVersion

    suspend fun getModelVersionByHash(hash: String): ModelVersion

    suspend fun getModelLicense(versionId: Long): ModelLicenseInfo?
}
