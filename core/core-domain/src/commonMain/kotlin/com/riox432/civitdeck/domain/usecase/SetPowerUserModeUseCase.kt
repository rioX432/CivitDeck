package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.UserPreferencesRepository

class SetPowerUserModeUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setPowerUserMode(enabled)
}
