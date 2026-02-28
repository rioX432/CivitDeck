package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.repository.ModelRepository

class GetCreatorModelsUseCase(private val repository: ModelRepository) {
    suspend operator fun invoke(
        username: String,
        cursor: String? = null,
        limit: Int? = null,
    ): PaginatedResult<Model> = repository.getModels(username = username, cursor = cursor, limit = limit)
}
