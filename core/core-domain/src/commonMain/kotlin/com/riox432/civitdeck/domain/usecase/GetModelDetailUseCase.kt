package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.repository.ModelRepository

class GetModelDetailUseCase(private val repository: ModelRepository) {
    suspend operator fun invoke(modelId: Long): Model = repository.getModel(modelId)
}
