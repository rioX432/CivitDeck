package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository

class CleanupBrowsingHistoryUseCase(private val repository: BrowsingHistoryRepository) {

    suspend operator fun invoke(nowMillis: Long, retentionDays: Int = DEFAULT_RETENTION_DAYS) {
        val cutoffMillis = nowMillis - retentionDays * DAY_MS
        repository.deleteOlderThan(cutoffMillis)
        repository.deleteExcessEntries(DEFAULT_MAX_ENTRIES)
    }

    companion object {
        const val DEFAULT_RETENTION_DAYS = 90
        private const val DEFAULT_MAX_ENTRIES = 5000
        private const val DAY_MS = 86_400_000L
    }
}
