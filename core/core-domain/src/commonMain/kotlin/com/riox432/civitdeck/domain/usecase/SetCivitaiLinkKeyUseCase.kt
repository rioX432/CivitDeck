package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.UserPreferencesRepository

class SetCivitaiLinkKeyUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(key: String?) = repository.setCivitaiLinkKey(key)
}
