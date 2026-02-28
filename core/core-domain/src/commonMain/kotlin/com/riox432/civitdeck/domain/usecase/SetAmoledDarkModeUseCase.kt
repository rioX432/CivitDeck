package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.UserPreferencesRepository

class SetAmoledDarkModeUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setAmoledDarkMode(enabled)
}
