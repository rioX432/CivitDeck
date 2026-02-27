package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveAccentColorUseCase(private val repository: UserPreferencesRepository) {
    operator fun invoke(): Flow<AccentColor> = repository.observeAccentColor()
}

class SetAccentColorUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(color: AccentColor) = repository.setAccentColor(color)
}

class ObserveAmoledDarkModeUseCase(private val repository: UserPreferencesRepository) {
    operator fun invoke(): Flow<Boolean> = repository.observeAmoledDarkMode()
}

class SetAmoledDarkModeUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setAmoledDarkMode(enabled)
}
