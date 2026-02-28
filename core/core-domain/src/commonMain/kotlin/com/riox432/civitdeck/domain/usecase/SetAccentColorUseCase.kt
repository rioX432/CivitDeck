package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository

class SetAccentColorUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(color: AccentColor) = repository.setAccentColor(color)
}
