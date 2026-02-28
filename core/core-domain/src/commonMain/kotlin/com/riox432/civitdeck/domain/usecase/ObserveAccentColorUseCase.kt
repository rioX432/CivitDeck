package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveAccentColorUseCase(private val repository: UserPreferencesRepository) {
    operator fun invoke(): Flow<AccentColor> = repository.observeAccentColor()
}
