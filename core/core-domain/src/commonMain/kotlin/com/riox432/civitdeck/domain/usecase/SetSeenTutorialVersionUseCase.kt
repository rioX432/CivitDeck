package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.UserPreferencesRepository

class SetSeenTutorialVersionUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(version: Int) = repository.setSeenTutorialVersion(version)
}
