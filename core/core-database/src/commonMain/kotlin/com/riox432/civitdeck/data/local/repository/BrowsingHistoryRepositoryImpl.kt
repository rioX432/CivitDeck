package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.BrowsingHistoryDao
import com.riox432.civitdeck.data.local.dao.InteractionEventDao
import com.riox432.civitdeck.data.local.entity.BrowsingHistoryEntity
import com.riox432.civitdeck.data.local.entity.InteractionEventEntity
import com.riox432.civitdeck.domain.model.InteractionType
import com.riox432.civitdeck.domain.model.RecentlyViewedModel
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.pow

class BrowsingHistoryRepositoryImpl(
    private val dao: BrowsingHistoryDao,
    private val interactionEventDao: InteractionEventDao,
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
        interactionEventDao.deleteAll()
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
        interactionEventDao.deleteOlderThan(cutoffMillis)
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
        // Append-only: every interaction is retained, even when no browsing-history row
        // exists yet for the model. Never overwrites, so signals accumulate.
        interactionEventDao.insert(
            InteractionEventEntity(
                modelId = modelId,
                type = interactionType.name,
                timestamp = currentTimeMillis(),
            ),
        )
    }

    override suspend fun getAverageViewDurationMs(): Long? {
        return dao.getAverageViewDuration()
    }

    override suspend fun getRecommendationClickCount(sinceMillis: Long): Int {
        return interactionEventDao.getCountByTypeSince(
            InteractionType.RECOMMENDATION_CLICK.name,
            sinceMillis,
        )
    }

    override suspend fun getInteractionCountByType(type: InteractionType, sinceMillis: Long): Int {
        return interactionEventDao.getCountByTypeSince(type.name, sinceMillis)
    }

    /**
     * Derives affinity scores from both passive views (browsing_history) and the
     * append-only interaction event log. View rows supply the metadata (tags/type/creator)
     * and a base decayed weight; each interaction event adds engagement weight attributed
     * to the keys of that model's most recent view row. Events for a model that was never
     * viewed are still persisted but cannot be attributed to any tag/type/creator until the
     * model is opened (its metadata is unknown until then). A model's metadata is stable
     * across views, so attributing to the most recent row introduces no meaningful skew.
     */
    private suspend fun computeWeightedScores(
        limit: Int,
        keysSelector: (BrowsingHistoryEntity) -> List<String>,
    ): Map<String, Double> {
        val now = currentTimeMillis()
        val since = now - DECAY_WINDOW_MS
        val entries = dao.getRecentSince(since)
        val scores = mutableMapOf<String, Double>()

        for (entry in entries) {
            val weight = decayWeight(now, entry.viewedAt) * durationWeight(entry.durationMs)
            for (key in keysSelector(entry).distinct()) {
                scores[key] = (scores[key] ?: 0.0) + weight
            }
        }

        val latestEntryByModel = entries
            .groupBy { it.modelId }
            .mapValues { (_, rows) -> rows.maxBy { it.viewedAt } }
        for (event in interactionEventDao.getEventsSince(since)) {
            val entry = latestEntryByModel[event.modelId] ?: continue
            val weight = decayWeight(now, event.timestamp) * eventWeight(event.type)
            for (key in keysSelector(entry).distinct()) {
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
        private const val DECAY_HALF_LIFE_MS = 14.0 * DAY_MS
        private const val LONG_VIEW_THRESHOLD_MS = 30_000L
        private const val SHORT_VIEW_THRESHOLD_MS = 5_000L
        private const val DOWNLOAD_WEIGHT = 5.0
        private const val FAVORITE_WEIGHT = 3.0
        private const val LONG_VIEW_WEIGHT = 2.0
        private const val SHORT_VIEW_WEIGHT = 0.5

        /**
         * Continuous exponential time decay: weight halves every [DECAY_HALF_LIFE_MS].
         * Fresh signals weigh ~1.0; a 14-day-old signal ~0.5, replacing the old
         * 7/30/90-day step function with a smooth curve.
         */
        internal fun decayWeight(now: Long, timestamp: Long): Double {
            val ageMs = (now - timestamp).coerceAtLeast(0L).toDouble()
            return 2.0.pow(-ageMs / DECAY_HALF_LIFE_MS)
        }

        internal fun eventWeight(type: String): Double {
            return when (runCatching { InteractionType.valueOf(type) }.getOrNull()) {
                InteractionType.DOWNLOAD -> DOWNLOAD_WEIGHT
                InteractionType.FAVORITE -> FAVORITE_WEIGHT
                InteractionType.SHARE -> FAVORITE_WEIGHT
                InteractionType.RECOMMENDATION_CLICK -> FAVORITE_WEIGHT
                // Views are already represented by browsing_history rows; unknown types
                // contribute nothing so a bad value can never inflate a score.
                InteractionType.VIEW, null -> 0.0
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
