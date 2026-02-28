package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.ModelRepository

class GetModelsUseCase(private val repository: ModelRepository) {
    suspend operator fun invoke(
        query: String? = null,
        tag: String? = null,
        type: ModelType? = null,
        sort: SortOrder? = null,
        period: TimePeriod? = null,
        baseModels: List<BaseModel>? = null,
        cursor: String? = null,
        limit: Int? = null,
        nsfw: Boolean? = null,
    ): PaginatedResult<Model> = repository.getModels(
        query = query,
        tag = tag,
        type = type,
        sort = sort,
        period = period,
        baseModels = baseModels,
        cursor = cursor,
        limit = limit,
        nsfw = nsfw,
    )
}
