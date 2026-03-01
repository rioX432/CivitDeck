package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.PollingInterval
import com.riox432.civitdeck.domain.repository.AppBehaviorPreferencesRepository

class SetPollingIntervalUseCase(
    private val repository: AppBehaviorPreferencesRepository,
) {
    suspend operator fun invoke(interval: PollingInterval) =
        repository.setPollingInterval(interval)
}
