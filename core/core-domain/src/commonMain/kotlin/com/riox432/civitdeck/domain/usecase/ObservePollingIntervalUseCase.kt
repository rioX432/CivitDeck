package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.PollingInterval
import com.riox432.civitdeck.domain.repository.AppBehaviorPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObservePollingIntervalUseCase(
    private val repository: AppBehaviorPreferencesRepository,
) {
    operator fun invoke(): Flow<PollingInterval> = repository.observePollingInterval()
}
