package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.domain.repository.DisplayPreferencesRepository

class SetAccentColorUseCase(private val repository: DisplayPreferencesRepository) {
    suspend operator fun invoke(color: AccentColor) = repository.setAccentColor(color)
}
