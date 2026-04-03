package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.InteractionType
import com.riox432.civitdeck.domain.model.RecentlyViewedModel
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrowsingHistoryUseCasesTest {

    private class FakeBrowsingHistoryRepository : BrowsingHistoryRepository {
        var trackCalled = false
        var lastTrackModelId: Long? = null
        var lastTrackType: String? = null
        var lastTrackCreator: String? = null
        var lastTrackTags: List<String>? = null
        val viewedIds = setOf(10L, 20L, 30L)
        var clearCalled = false

        override suspend fun trackView(
            modelId: Long,
            modelName: String,
            modelType: String,
            creatorName: String?,
            thumbnailUrl: String?,
            tags: List<String>,
        ) {
            trackCalled = true
            lastTrackModelId = modelId
            lastTrackType = modelType
            lastTrackCreator = creatorName
            lastTrackTags = tags
        }

        override suspend fun getRecentTypes(limit: Int): Map<String, Int> = error("not used")
        override suspend fun getRecentCreators(limit: Int): Map<String, Int> = error("not used")
        override suspend fun getRecentTags(limit: Int): Map<String, Int> = error("not used")
        override suspend fun getRecentModelIds(limit: Int): List<Long> = error("not used")
        override suspend fun getAllViewedModelIds(): Set<Long> = viewedIds
        override suspend fun clearAll() { clearCalled = true }
        override suspend fun deleteById(historyId: Long) {}
        override fun observeRecentlyViewed(limit: Int): Flow<List<RecentlyViewedModel>> = flowOf(emptyList())
        override suspend fun cleanup(cutoffMillis: Long, maxEntries: Int) {}
        override suspend fun getWeightedTypes(limit: Int): Map<String, Double> = error("not used")
        override suspend fun getWeightedTags(limit: Int): Map<String, Double> = error("not used")
        override suspend fun getWeightedCreators(limit: Int): Map<String, Double> = error("not used")
        override suspend fun updateViewDuration(modelId: Long, durationMs: Long) {}
        override suspend fun trackInteraction(modelId: Long, interactionType: InteractionType) {}
        override suspend fun getAverageViewDurationMs(): Long? = null
        override suspend fun getRecommendationClickCount(sinceMillis: Long): Int = 0
        override suspend fun getInteractionCountByType(type: InteractionType, sinceMillis: Long): Int = 0
    }

    private val repo = FakeBrowsingHistoryRepository()

    @Test
    fun trackModelView_passes_all_parameters() = runTest {
        val useCase = TrackModelViewUseCase(repo)
        useCase(
            modelId = 42L,
            modelName = "Test Model",
            modelType = "Checkpoint",
            creatorName = "artist1",
            thumbnailUrl = null,
            tags = listOf("anime", "portrait"),
        )
        assertTrue(repo.trackCalled)
        assertEquals(42L, repo.lastTrackModelId)
        assertEquals("Checkpoint", repo.lastTrackType)
        assertEquals("artist1", repo.lastTrackCreator)
        assertEquals(listOf("anime", "portrait"), repo.lastTrackTags)
    }

    @Test
    fun trackModelView_handles_null_creator() = runTest {
        val useCase = TrackModelViewUseCase(repo)
        useCase(
            modelId = 1L,
            modelName = "Test",
            modelType = "LORA",
            creatorName = null,
            thumbnailUrl = null,
            tags = emptyList(),
        )
        assertEquals(null, repo.lastTrackCreator)
    }

    @Test
    fun getViewedModelIds_returns_set() = runTest {
        val useCase = GetViewedModelIdsUseCase(repo)
        assertEquals(setOf(10L, 20L, 30L), useCase())
    }

    @Test
    fun clearBrowsingHistory_delegates() = runTest {
        val useCase = ClearBrowsingHistoryUseCase(repo)
        useCase()
        assertTrue(repo.clearCalled)
    }
}
