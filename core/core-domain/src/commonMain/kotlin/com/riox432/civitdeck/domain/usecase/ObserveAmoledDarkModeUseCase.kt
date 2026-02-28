package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveAmoledDarkModeUseCase(private val repository: UserPreferencesRepository) {
    operator fun invoke(): Flow<Boolean> = repository.observeAmoledDarkMode()
}
