package com.riox432.civitdeck.testing

import com.riox432.civitdeck.domain.model.InteractionType
import com.riox432.civitdeck.domain.model.RecentlyViewedModel
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [BrowsingHistoryRepository] for ViewModel tests.
 *
 * Records the last `updateViewDuration` call (end-view tracking) and exposes the
 * set of viewed model ids so "fresh find" code paths can be exercised.
 */
class FakeBrowsingHistoryRepository(
    var viewedModelIds: Set<Long> = emptySet(),
) : BrowsingHistoryRepository {

    val recentlyViewedFlow = MutableStateFlow(emptyList<RecentlyViewedModel>())

    var trackViewCount: Int = 0
    var endViewModelId: Long? = null
    var endViewDurationMs: Long? = null

    override suspend fun trackView(
        modelId: Long,
        modelName: String,
        modelType: String,
        creatorName: String?,
        thumbnailUrl: String?,
        tags: List<String>,
    ) {
        trackViewCount++
    }

    override suspend fun getRecentTypes(limit: Int): Map<String, Int> = emptyMap()
    override suspend fun getRecentCreators(limit: Int): Map<String, Int> = emptyMap()
    override suspend fun getRecentTags(limit: Int): Map<String, Int> = emptyMap()
    override suspend fun getRecentModelIds(limit: Int): List<Long> = emptyList()
    override suspend fun getAllViewedModelIds(): Set<Long> = viewedModelIds
    override suspend fun clearAll() = Unit
    override suspend fun deleteById(historyId: Long) = Unit

    override fun observeRecentlyViewed(limit: Int): Flow<List<RecentlyViewedModel>> =
        recentlyViewedFlow

    override suspend fun cleanup(cutoffMillis: Long, maxEntries: Int) = Unit

    override suspend fun getWeightedTypes(limit: Int): Map<String, Double> = emptyMap()
    override suspend fun getWeightedTags(limit: Int): Map<String, Double> = emptyMap()
    override suspend fun getWeightedCreators(limit: Int): Map<String, Double> = emptyMap()

    override suspend fun updateViewDuration(modelId: Long, durationMs: Long) {
        endViewModelId = modelId
        endViewDurationMs = durationMs
    }

    override suspend fun trackInteraction(modelId: Long, interactionType: InteractionType) = Unit
    override suspend fun getAverageViewDurationMs(): Long? = null
    override suspend fun getRecommendationClickCount(sinceMillis: Long): Int = 0
    override suspend fun getInteractionCountByType(type: InteractionType, sinceMillis: Long): Int = 0
}
