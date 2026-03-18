package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository

class CleanupBrowsingHistoryUseCase(private val repository: BrowsingHistoryRepository) {

    suspend operator fun invoke(nowMillis: Long) {
        val cutoffMillis = nowMillis - RETENTION_DAYS * DAY_MS
        repository.cleanup(cutoffMillis, MAX_ENTRIES)
    }

    companion object {
        private const val RETENTION_DAYS = 90
        private const val MAX_ENTRIES = 5000
        private const val DAY_MS = 86_400_000L
    }
}
