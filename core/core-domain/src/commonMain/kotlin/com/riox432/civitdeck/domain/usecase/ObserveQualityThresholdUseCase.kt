package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.AppBehaviorPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveQualityThresholdUseCase(private val repository: AppBehaviorPreferencesRepository) {
    operator fun invoke(): Flow<Int> = repository.observeFeedQualityThreshold()
}
