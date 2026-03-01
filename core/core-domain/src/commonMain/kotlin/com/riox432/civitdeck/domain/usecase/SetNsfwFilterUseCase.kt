package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.repository.ContentFilterPreferencesRepository

class SetNsfwFilterUseCase(private val repository: ContentFilterPreferencesRepository) {
    suspend operator fun invoke(level: NsfwFilterLevel) = repository.setNsfwFilterLevel(level)
}
