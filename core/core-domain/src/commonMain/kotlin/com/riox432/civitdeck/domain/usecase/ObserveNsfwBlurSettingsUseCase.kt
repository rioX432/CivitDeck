package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.repository.ContentFilterPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveNsfwBlurSettingsUseCase(private val repository: ContentFilterPreferencesRepository) {
    operator fun invoke(): Flow<NsfwBlurSettings> = repository.observeNsfwBlurSettings()
}
