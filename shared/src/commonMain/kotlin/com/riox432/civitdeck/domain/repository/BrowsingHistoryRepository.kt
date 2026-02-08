package com.riox432.civitdeck.domain.repository

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
}
