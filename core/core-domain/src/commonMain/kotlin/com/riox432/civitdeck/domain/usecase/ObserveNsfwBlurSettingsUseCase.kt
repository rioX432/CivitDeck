package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveNsfwBlurSettingsUseCase(private val repository: UserPreferencesRepository) {
    operator fun invoke(): Flow<NsfwBlurSettings> = repository.observeNsfwBlurSettings()
}
