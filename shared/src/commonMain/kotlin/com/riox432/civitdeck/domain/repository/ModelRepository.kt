package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod

interface ModelRepository {
    suspend fun getModels(
        query: String? = null,
        tag: String? = null,
        type: ModelType? = null,
        sort: SortOrder? = null,
        period: TimePeriod? = null,
        cursor: String? = null,
        limit: Int? = null,
    ): PaginatedResult<Model>

    suspend fun getModel(id: Long): Model

    suspend fun getModelVersion(id: Long): ModelVersion

    suspend fun getModelVersionByHash(hash: String): ModelVersion
}
