package com.omooooori.civitdeck.domain.usecase

import com.omooooori.civitdeck.domain.model.Model
import com.omooooori.civitdeck.domain.model.ModelType
import com.omooooori.civitdeck.domain.model.PaginatedResult
import com.omooooori.civitdeck.domain.model.SortOrder
import com.omooooori.civitdeck.domain.model.TimePeriod
import com.omooooori.civitdeck.domain.repository.ModelRepository

class GetModelsUseCase(private val repository: ModelRepository) {
    suspend operator fun invoke(
        query: String? = null,
        tag: String? = null,
        type: ModelType? = null,
        sort: SortOrder? = null,
        period: TimePeriod? = null,
        page: Int? = null,
        limit: Int? = null,
    ): PaginatedResult<Model> = repository.getModels(
        query = query,
        tag = tag,
        type = type,
        sort = sort,
        period = period,
        page = page,
        limit = limit,
    )
}
