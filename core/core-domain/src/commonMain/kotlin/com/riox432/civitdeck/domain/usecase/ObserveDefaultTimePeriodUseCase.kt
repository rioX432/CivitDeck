package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.DisplayPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveDefaultTimePeriodUseCase(private val repository: DisplayPreferencesRepository) {
    operator fun invoke(): Flow<TimePeriod> = repository.observeDefaultTimePeriod()
}
