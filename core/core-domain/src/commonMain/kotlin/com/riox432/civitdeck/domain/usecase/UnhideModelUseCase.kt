package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.HiddenModelRepository

class UnhideModelUseCase(private val repository: HiddenModelRepository) {
    suspend operator fun invoke(modelId: Long) = repository.unhideModel(modelId)
}
