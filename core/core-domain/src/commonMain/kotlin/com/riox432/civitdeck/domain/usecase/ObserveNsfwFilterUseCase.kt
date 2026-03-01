package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.repository.ContentFilterPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveNsfwFilterUseCase(private val repository: ContentFilterPreferencesRepository) {
    operator fun invoke(): Flow<NsfwFilterLevel> = repository.observeNsfwFilterLevel()
}
