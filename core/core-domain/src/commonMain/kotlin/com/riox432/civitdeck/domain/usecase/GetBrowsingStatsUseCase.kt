package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.BrowsingStats
import com.riox432.civitdeck.domain.repository.AnalyticsRepository

class GetBrowsingStatsUseCase(private val repository: AnalyticsRepository) {
    suspend operator fun invoke(): BrowsingStats = repository.getBrowsingStats()
}
