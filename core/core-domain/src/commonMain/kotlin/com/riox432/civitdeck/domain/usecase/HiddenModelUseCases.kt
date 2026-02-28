package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.HiddenModelRepository

class GetHiddenModelIdsUseCase(private val repository: HiddenModelRepository) {
    suspend operator fun invoke(): Set<Long> = repository.getHiddenModelIds()
}

class HideModelUseCase(private val repository: HiddenModelRepository) {
    suspend operator fun invoke(modelId: Long, modelName: String) =
        repository.hideModel(modelId, modelName)
}

class UnhideModelUseCase(private val repository: HiddenModelRepository) {
    suspend operator fun invoke(modelId: Long) = repository.unhideModel(modelId)
}
