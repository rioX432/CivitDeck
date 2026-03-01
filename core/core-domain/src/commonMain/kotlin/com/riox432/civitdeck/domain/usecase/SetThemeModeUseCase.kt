package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ThemeMode
import com.riox432.civitdeck.domain.repository.DisplayPreferencesRepository

class SetThemeModeUseCase(private val repository: DisplayPreferencesRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
