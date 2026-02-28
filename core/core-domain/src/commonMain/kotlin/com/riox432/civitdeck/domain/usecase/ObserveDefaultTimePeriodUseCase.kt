package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveDefaultTimePeriodUseCase(private val repository: UserPreferencesRepository) {
    operator fun invoke(): Flow<TimePeriod> = repository.observeDefaultTimePeriod()
}
