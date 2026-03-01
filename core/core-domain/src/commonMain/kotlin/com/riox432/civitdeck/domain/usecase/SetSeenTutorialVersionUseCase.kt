package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.AppBehaviorPreferencesRepository

class SetSeenTutorialVersionUseCase(private val repository: AppBehaviorPreferencesRepository) {
    suspend operator fun invoke(version: Int) = repository.setSeenTutorialVersion(version)
}
