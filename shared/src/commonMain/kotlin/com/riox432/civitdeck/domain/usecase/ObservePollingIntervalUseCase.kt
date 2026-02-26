package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.PollingInterval
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObservePollingIntervalUseCase(
    private val repository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<PollingInterval> = repository.observePollingInterval()
}
