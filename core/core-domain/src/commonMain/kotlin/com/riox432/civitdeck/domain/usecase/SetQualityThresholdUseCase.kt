package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.AppBehaviorPreferencesRepository

class SetQualityThresholdUseCase(private val repository: AppBehaviorPreferencesRepository) {
    suspend operator fun invoke(threshold: Int) {
        repository.setFeedQualityThreshold(threshold.coerceIn(0, 100))
    }
}
