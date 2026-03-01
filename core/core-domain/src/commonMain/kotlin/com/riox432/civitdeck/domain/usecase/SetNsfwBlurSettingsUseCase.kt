package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.repository.ContentFilterPreferencesRepository

class SetNsfwBlurSettingsUseCase(private val repository: ContentFilterPreferencesRepository) {
    suspend operator fun invoke(settings: NsfwBlurSettings) = repository.setNsfwBlurSettings(settings)
}
