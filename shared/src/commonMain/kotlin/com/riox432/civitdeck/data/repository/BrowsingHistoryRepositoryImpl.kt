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

    override suspend fun deleteOlderThan(cutoffMillis: Long): Int {
        return dao.deleteOlderThan(cutoffMillis)
    }

    override suspend fun deleteExcessEntries(maxCount: Int): Int {
        return dao.deleteExcessEntries(maxCount)
    }

    override suspend fun getWeightedTypes(limit: Int): Map<String, Double> {
        return computeWeightedScores(limit) { it.modelType }
    }

    override suspend fun getWeightedTags(limit: Int): Map<String, Double> {
        val now = currentTimeMillis()
        val entries = dao.getRecentSince(now - DECAY_WINDOW_MS)
        val scores = mutableMapOf<String, Double>()
        for (entry in entries) {
            val weight = decayWeight(now, entry.viewedAt)
            val tags = entry.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
            for (tag in tags) {
                scores[tag] = (scores[tag] ?: 0.0) + weight
            }
        }
        return scores.entries
            .sortedByDescending { it.value }
            .take(limit)
            .associate { it.key to it.value }
    }

    override suspend fun getWeightedCreators(limit: Int): Map<String, Double> {
        return computeWeightedScores(limit) { it.creatorName ?: return@computeWeightedScores null }
    }

    private suspend fun computeWeightedScores(
        limit: Int,
        keySelector: (BrowsingHistoryEntity) -> String?,
    ): Map<String, Double> {
        val now = currentTimeMillis()
        val entries = dao.getRecentSince(now - DECAY_WINDOW_MS)
        val scores = mutableMapOf<String, Double>()
        for (entry in entries) {
            val key = keySelector(entry) ?: continue
            val weight = decayWeight(now, entry.viewedAt)
            scores[key] = (scores[key] ?: 0.0) + weight
        }
        return scores.entries
            .sortedByDescending { it.value }
            .take(limit)
            .associate { it.key to it.value }
    }

    companion object {
        private const val DAY_MS = 86_400_000L
        private const val DECAY_WINDOW_MS = 90 * DAY_MS

        internal fun decayWeight(now: Long, viewedAt: Long): Double {
            val ageMs = now - viewedAt
            return when {
                ageMs < 7 * DAY_MS -> 1.0
                ageMs < 30 * DAY_MS -> 0.5
                ageMs < 90 * DAY_MS -> 0.2
                else -> 0.05
            }
        }
    }
}
