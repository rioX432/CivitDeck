package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.DisplayPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveAmoledDarkModeUseCase(private val repository: DisplayPreferencesRepository) {
    operator fun invoke(): Flow<Boolean> = repository.observeAmoledDarkMode()
}
