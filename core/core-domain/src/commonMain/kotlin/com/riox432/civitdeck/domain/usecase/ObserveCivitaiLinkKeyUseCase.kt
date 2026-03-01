package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.AuthPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveCivitaiLinkKeyUseCase(private val repository: AuthPreferencesRepository) {
    operator fun invoke(): Flow<String?> = repository.observeCivitaiLinkKey()
}
