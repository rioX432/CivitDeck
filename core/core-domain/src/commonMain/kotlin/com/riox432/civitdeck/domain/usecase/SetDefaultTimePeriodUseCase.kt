package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.DisplayPreferencesRepository

class SetDefaultTimePeriodUseCase(private val repository: DisplayPreferencesRepository) {
    suspend operator fun invoke(period: TimePeriod) = repository.setDefaultTimePeriod(period)
}
