package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.DisplayPreferencesRepository

class SetAmoledDarkModeUseCase(private val repository: DisplayPreferencesRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setAmoledDarkMode(enabled)
}
