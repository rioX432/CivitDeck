package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.BrowsingHistoryDao
import com.riox432.civitdeck.data.local.entity.BrowsingHistoryEntity
import com.riox432.civitdeck.domain.model.InteractionType
import com.riox432.civitdeck.domain.model.RecentlyViewedModel
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BrowsingHistoryRepositoryImpl(
    private val dao: BrowsingHistoryDao,
) : BrowsingHistoryRepository {

    override suspend fun trackView(
        modelId: Long,
        modelName: String,
        modelType: String,
        creatorName: String?,
        thumbnailUrl: String?,
        tags: List<String>,
    ) {
        dao.insert(
            BrowsingHistoryEntity(
                modelId = modelId,
                modelName = modelName,
                modelType = modelType,
                creatorName = creatorName,
                thumbnailUrl = thumbnailUrl,
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

    override suspend fun deleteById(historyId: Long) {
        dao.deleteById(historyId)
    }

    override fun observeRecentlyViewed(limit: Int): Flow<List<RecentlyViewedModel>> {
        return dao.observeRecent(limit).map { entities ->
            entities
                .filter { it.modelName.isNotBlank() }
                .map { it.toRecentlyViewedModel() }
        }
    }

    override suspend fun cleanup(cutoffMillis: Long, maxEntries: Int) {
        dao.deleteOlderThan(cutoffMillis)
        dao.deleteExcessEntries(maxEntries)
    }

    override suspend fun getWeightedTypes(limit: Int): Map<String, Double> {
        return computeWeightedScores(limit) { entry ->
            listOf(entry.modelType)
        }
    }

    override suspend fun getWeightedTags(limit: Int): Map<String, Double> {
        return computeWeightedScores(limit) { entry ->
            entry.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }
    }

    override suspend fun getWeightedCreators(limit: Int): Map<String, Double> {
        return computeWeightedScores(limit) { entry ->
            listOfNotNull(entry.creatorName)
        }
    }

    override suspend fun updateViewDuration(modelId: Long, durationMs: Long) {
        val id = dao.getLatestIdForModel(modelId) ?: return
        dao.updateDuration(id, durationMs)
    }

    override suspend fun trackInteraction(modelId: Long, interactionType: InteractionType) {
        val id = dao.getLatestIdForModel(modelId) ?: return
        dao.updateInteractionType(id, interactionType.name)
    }

    override suspend fun getAverageViewDurationMs(): Long? {
        return dao.getAverageViewDuration()
    }

    override suspend fun getRecommendationClickCount(sinceMillis: Long): Int {
        return dao.getInteractionCountByType(InteractionType.RECOMMENDATION_CLICK.name, sinceMillis)
    }

    override suspend fun getInteractionCountByType(type: InteractionType, sinceMillis: Long): Int {
        return dao.getInteractionCountByType(type.name, sinceMillis)
    }

    private suspend fun computeWeightedScores(
        limit: Int,
        keysSelector: (BrowsingHistoryEntity) -> List<String>,
    ): Map<String, Double> {
        val now = currentTimeMillis()
        val entries = dao.getRecentSince(now - DECAY_WINDOW_MS)
        val scores = mutableMapOf<String, Double>()
        for (entry in entries) {
            val weight = decayWeight(now, entry.viewedAt) * engagementWeight(entry)
            for (key in keysSelector(entry)) {
                scores[key] = (scores[key] ?: 0.0) + weight
            }
        }
        return scores.entries
            .sortedByDescending { it.value }
            .take(limit)
            .associate { it.key to it.value }
    }

    companion object {
        private const val DAY_MS = 86_400_000L
        private const val DECAY_WINDOW_MS = 90 * DAY_MS
        private const val LONG_VIEW_THRESHOLD_MS = 30_000L
        private const val SHORT_VIEW_THRESHOLD_MS = 5_000L
        private const val DOWNLOAD_WEIGHT = 5.0
        private const val FAVORITE_WEIGHT = 3.0
        private const val LONG_VIEW_WEIGHT = 2.0
        private const val SHORT_VIEW_WEIGHT = 0.5

        internal fun decayWeight(now: Long, viewedAt: Long): Double {
            val ageMs = now - viewedAt
            return when {
                ageMs < 7 * DAY_MS -> 1.0
                ageMs < 30 * DAY_MS -> 0.5
                ageMs < 90 * DAY_MS -> 0.2
                else -> 0.05
            }
        }

        internal fun engagementWeight(entry: BrowsingHistoryEntity): Double {
            val interaction = entry.interactionType?.let {
                runCatching { InteractionType.valueOf(it) }.getOrNull()
            }
            return when (interaction) {
                InteractionType.DOWNLOAD -> DOWNLOAD_WEIGHT
                InteractionType.FAVORITE -> FAVORITE_WEIGHT
                InteractionType.SHARE -> FAVORITE_WEIGHT
                InteractionType.RECOMMENDATION_CLICK -> FAVORITE_WEIGHT
                InteractionType.VIEW, null -> durationWeight(entry.durationMs)
            }
        }

        private fun durationWeight(durationMs: Long?): Double {
            if (durationMs == null) return 1.0
            return when {
                durationMs >= LONG_VIEW_THRESHOLD_MS -> LONG_VIEW_WEIGHT
                durationMs < SHORT_VIEW_THRESHOLD_MS -> SHORT_VIEW_WEIGHT
                else -> 1.0
            }
        }
    }
}

private fun BrowsingHistoryEntity.toRecentlyViewedModel() = RecentlyViewedModel(
    historyId = id,
    modelId = modelId,
    modelName = modelName,
    modelType = modelType,
    creatorName = creatorName,
    thumbnailUrl = thumbnailUrl,
    viewedAt = viewedAt,
)
