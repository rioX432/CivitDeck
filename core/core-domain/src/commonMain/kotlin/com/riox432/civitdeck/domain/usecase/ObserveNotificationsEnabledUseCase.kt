package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.AppBehaviorPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveNotificationsEnabledUseCase(
    private val repository: AppBehaviorPreferencesRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeNotificationsEnabled()
}
