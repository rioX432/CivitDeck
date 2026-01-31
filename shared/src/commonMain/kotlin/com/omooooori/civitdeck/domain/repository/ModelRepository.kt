package com.omooooori.civitdeck.domain.repository

import com.omooooori.civitdeck.domain.model.Model
import com.omooooori.civitdeck.domain.model.ModelType
import com.omooooori.civitdeck.domain.model.ModelVersion
import com.omooooori.civitdeck.domain.model.PaginatedResult
import com.omooooori.civitdeck.domain.model.SortOrder
import com.omooooori.civitdeck.domain.model.TimePeriod

interface ModelRepository {
    suspend fun getModels(
        query: String? = null,
        tag: String? = null,
        type: ModelType? = null,
        sort: SortOrder? = null,
        period: TimePeriod? = null,
        page: Int? = null,
        limit: Int? = null,
    ): PaginatedResult<Model>

    suspend fun getModel(id: Long): Model

    suspend fun getModelVersion(id: Long): ModelVersion

    suspend fun getModelVersionByHash(hash: String): ModelVersion
}
