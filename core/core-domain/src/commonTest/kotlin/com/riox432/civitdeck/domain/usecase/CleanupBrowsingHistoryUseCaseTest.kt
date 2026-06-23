package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.InteractionType
import com.riox432.civitdeck.domain.model.RecentlyViewedModel
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers [CleanupBrowsingHistoryUseCase], whose only logic is deriving the
 * retention cutoff (now - 90 days) and forwarding the fixed max-entries cap.
 */
class CleanupBrowsingHistoryUseCaseTest {

    @Test
    fun derives_cutoff_90_days_before_now_and_caps_entries() = runTest {
        // Arrange: a fixed "now"; the cutoff must be exactly 90 days earlier in millis.
        val now = 1_000_000_000_000L
        val ninetyDaysMs = 90L * 86_400_000L
        val repo = RecordingBrowsingHistoryRepo()
        val useCase = CleanupBrowsingHistoryUseCase(repo)

        // Act
        useCase(nowMillis = now)

        // Assert: cutoff = now - 90d, and the 5000-entry cap is forwarded.
        assertEquals(now - ninetyDaysMs, repo.lastCutoff)
        assertEquals(5000, repo.lastMaxEntries)
    }

    private class RecordingBrowsingHistoryRepo : BrowsingHistoryRepository {
        var lastCutoff: Long? = null
        var lastMaxEntries: Int? = null

        override suspend fun cleanup(cutoffMillis: Long, maxEntries: Int) {
            lastCutoff = cutoffMillis
            lastMaxEntries = maxEntries
        }

        // --- unused members ---
        override suspend fun trackView(
            modelId: Long,
            modelName: String,
            modelType: String,
            creatorName: String?,
            thumbnailUrl: String?,
            tags: List<String>,
        ) = Unit
        override suspend fun getRecentTypes(limit: Int): Map<String, Int> = emptyMap()
        override suspend fun getRecentCreators(limit: Int): Map<String, Int> = emptyMap()
        override suspend fun getRecentTags(limit: Int): Map<String, Int> = emptyMap()
        override suspend fun getRecentModelIds(limit: Int): List<Long> = emptyList()
        override suspend fun getAllViewedModelIds(): Set<Long> = emptySet()
        override suspend fun clearAll() = Unit
        override suspend fun deleteById(historyId: Long) = Unit
        override fun observeRecentlyViewed(limit: Int): Flow<List<RecentlyViewedModel>> =
            flowOf(emptyList())
        override suspend fun getWeightedTypes(limit: Int): Map<String, Double> = emptyMap()
        override suspend fun getWeightedTags(limit: Int): Map<String, Double> = emptyMap()
        override suspend fun getWeightedCreators(limit: Int): Map<String, Double> = emptyMap()
        override suspend fun updateViewDuration(modelId: Long, durationMs: Long) = Unit
        override suspend fun trackInteraction(modelId: Long, interactionType: InteractionType) = Unit
        override suspend fun getAverageViewDurationMs(): Long? = null
        override suspend fun getRecommendationClickCount(sinceMillis: Long): Int = 0
        override suspend fun getInteractionCountByType(type: InteractionType, sinceMillis: Long): Int = 0
    }
}
