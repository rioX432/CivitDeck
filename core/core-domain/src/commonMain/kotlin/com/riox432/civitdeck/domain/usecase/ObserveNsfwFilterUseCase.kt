package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveNsfwFilterUseCase(private val repository: UserPreferencesRepository) {
    operator fun invoke(): Flow<NsfwFilterLevel> = repository.observeNsfwFilterLevel()
}
