package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.HiddenModel
import com.riox432.civitdeck.domain.repository.HiddenModelRepository

class GetHiddenModelsUseCase(private val repository: HiddenModelRepository) {
    suspend operator fun invoke(): List<HiddenModel> = repository.getHiddenModels()
}
