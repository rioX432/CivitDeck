package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.InteractionType
import com.riox432.civitdeck.domain.model.RecentlyViewedModel
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface BrowsingHistoryRepository {
    suspend fun trackView(
        modelId: Long,
        modelName: String,
        modelType: String,
        creatorName: String?,
        thumbnailUrl: String?,
        tags: List<String>,
    )

    suspend fun getRecentTypes(limit: Int = 100): Map<String, Int>
    suspend fun getRecentCreators(limit: Int = 100): Map<String, Int>
    suspend fun getRecentTags(limit: Int = 100): Map<String, Int>
    suspend fun getRecentModelIds(limit: Int = 50): List<Long>
    suspend fun getAllViewedModelIds(): Set<Long>
    suspend fun clearAll()
    suspend fun deleteById(historyId: Long)

    fun observeRecentlyViewed(limit: Int = 50): Flow<List<RecentlyViewedModel>>

    suspend fun cleanup(cutoffMillis: Long, maxEntries: Int)

    suspend fun getWeightedTypes(limit: Int = 10): Map<String, Double>
    suspend fun getWeightedTags(limit: Int = 10): Map<String, Double>
    suspend fun getWeightedCreators(limit: Int = 10): Map<String, Double>

    suspend fun updateViewDuration(modelId: Long, durationMs: Long)
    suspend fun trackInteraction(modelId: Long, interactionType: InteractionType)
    suspend fun getAverageViewDurationMs(): Long?
}
