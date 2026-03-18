package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.RecentlyViewedModel
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import kotlinx.coroutines.flow.Flow

class ObserveRecentlyViewedUseCase(private val repository: BrowsingHistoryRepository) {
    operator fun invoke(limit: Int = 10): Flow<List<RecentlyViewedModel>> =
        repository.observeRecentlyViewed(limit)
}
