package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.BrowsingStats

interface AnalyticsRepository {
    suspend fun getBrowsingStats(): BrowsingStats
}
