package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.FrontDoorMode
import com.riox432.civitdeck.domain.repository.ContentFilterPreferencesRepository

class SetFrontDoorModeUseCase(private val repository: ContentFilterPreferencesRepository) {
    suspend operator fun invoke(mode: FrontDoorMode) = repository.setFrontDoorMode(mode)
}
