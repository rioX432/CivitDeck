package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.BrowsingHistoryDao
import com.riox432.civitdeck.data.local.entity.BrowsingHistoryEntity
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository

class BrowsingHistoryRepositoryImpl(
    private val dao: BrowsingHistoryDao,
) : BrowsingHistoryRepository {

    override suspend fun trackView(
        modelId: Long,
        modelType: String,
        creatorName: String?,
        tags: List<String>,
    ) {
        dao.insert(
            BrowsingHistoryEntity(
                modelId = modelId,
                modelType = modelType,
                creatorName = creatorName,
                tags = tags.joinToString(","),
                viewedAt = currentTimeMillis(),
            ),
        )
    }

    override suspend fun getRecentTypes(limit: Int): Map<String, Int> {
        return dao.getRecent(limit)
            .groupingBy { it.modelType }
            .eachCount()
    }

    override suspend fun getRecentCreators(limit: Int): Map<String, Int> {
        return dao.getRecent(limit)
            .mapNotNull { it.creatorName }
            .groupingBy { it }
            .eachCount()
    }

    override suspend fun getRecentTags(limit: Int): Map<String, Int> {
        return dao.getRecent(limit)
            .flatMap { it.tags.split(",").map { tag -> tag.trim() }.filter { tag -> tag.isNotBlank() } }
            .groupingBy { it }
            .eachCount()
    }

    override suspend fun getRecentModelIds(limit: Int): List<Long> {
        return dao.getRecentModelIds(limit)
    }

    override suspend fun getAllViewedModelIds(): Set<Long> {
        return dao.getAllModelIds().toSet()
    }

    override suspend fun clearAll() {
        dao.deleteAll()
    }
}
