package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.FrontDoorMode
import com.riox432.civitdeck.domain.repository.ContentFilterPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveFrontDoorModeUseCase(private val repository: ContentFilterPreferencesRepository) {
    operator fun invoke(): Flow<FrontDoorMode> = repository.observeFrontDoorMode()
}
