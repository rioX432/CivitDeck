package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository

class SetNsfwBlurSettingsUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(settings: NsfwBlurSettings) = repository.setNsfwBlurSettings(settings)
}
