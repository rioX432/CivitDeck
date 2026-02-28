package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveCivitaiLinkKeyUseCase(private val repository: UserPreferencesRepository) {
    operator fun invoke(): Flow<String?> = repository.observeCivitaiLinkKey()
}
