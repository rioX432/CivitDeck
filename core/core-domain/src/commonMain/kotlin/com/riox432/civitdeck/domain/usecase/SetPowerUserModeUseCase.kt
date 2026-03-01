package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.AppBehaviorPreferencesRepository

class SetPowerUserModeUseCase(private val repository: AppBehaviorPreferencesRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setPowerUserMode(enabled)
}
