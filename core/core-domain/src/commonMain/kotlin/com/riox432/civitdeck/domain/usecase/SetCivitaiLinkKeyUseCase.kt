package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.AuthPreferencesRepository

class SetCivitaiLinkKeyUseCase(private val repository: AuthPreferencesRepository) {
    suspend operator fun invoke(key: String?) = repository.setCivitaiLinkKey(key)
}
