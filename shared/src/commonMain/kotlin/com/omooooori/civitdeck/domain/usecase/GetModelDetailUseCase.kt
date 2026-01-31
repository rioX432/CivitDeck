package com.omooooori.civitdeck.domain.usecase

import com.omooooori.civitdeck.domain.model.Model
import com.omooooori.civitdeck.domain.repository.ModelRepository

class GetModelDetailUseCase(private val repository: ModelRepository) {
    suspend operator fun invoke(modelId: Long): Model = repository.getModel(modelId)
}
