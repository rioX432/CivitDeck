package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository

class SetDefaultTimePeriodUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(period: TimePeriod) = repository.setDefaultTimePeriod(period)
}
