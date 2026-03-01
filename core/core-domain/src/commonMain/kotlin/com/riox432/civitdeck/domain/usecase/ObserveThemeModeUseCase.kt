package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ThemeMode
import com.riox432.civitdeck.domain.repository.DisplayPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveThemeModeUseCase(private val repository: DisplayPreferencesRepository) {
    operator fun invoke(): Flow<ThemeMode> = repository.observeThemeMode()
}
