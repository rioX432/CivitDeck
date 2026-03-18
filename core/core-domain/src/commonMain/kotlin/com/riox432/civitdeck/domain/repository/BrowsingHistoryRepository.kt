package com.riox432.civitdeck.domain.repository

@Suppress("TooManyFunctions")
interface BrowsingHistoryRepository {
    suspend fun trackView(
        modelId: Long,
        modelType: String,
        creatorName: String?,
        tags: List<String>,
    )

    suspend fun getRecentTypes(limit: Int = 100): Map<String, Int>
    suspend fun getRecentCreators(limit: Int = 100): Map<String, Int>
    suspend fun getRecentTags(limit: Int = 100): Map<String, Int>
    suspend fun getRecentModelIds(limit: Int = 50): List<Long>
    suspend fun getAllViewedModelIds(): Set<Long>
    suspend fun clearAll()

    suspend fun deleteOlderThan(cutoffMillis: Long): Int
    suspend fun deleteExcessEntries(maxCount: Int): Int

    suspend fun getWeightedTypes(limit: Int = 10): Map<String, Double>
    suspend fun getWeightedTags(limit: Int = 10): Map<String, Double>
    suspend fun getWeightedCreators(limit: Int = 10): Map<String, Double>
}
