package com.riox432.civitdeck.feature.search.domain.usecase

import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.RecommendationSection
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.model.filterNsfwImages
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import com.riox432.civitdeck.domain.repository.ContentFilterPreferencesRepository
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import kotlinx.coroutines.flow.first

class GetRecommendationsUseCase(
    private val modelRepository: ModelRepository,
    private val favoriteRepository: FavoriteRepository,
    private val browsingHistoryRepository: BrowsingHistoryRepository,
    private val userPreferencesRepository: ContentFilterPreferencesRepository,
) {
    @Suppress("LongMethod")
    suspend operator fun invoke(): List<RecommendationSection> {
        val sections = mutableListOf<RecommendationSection>()
        val seenIds = mutableSetOf<Long>()
        val nsfwLevel = userPreferencesRepository.observeNsfwFilterLevel().first()

        val favIds = favoriteRepository.getAllFavoriteIds()
        seenIds.addAll(favIds)

        val viewedIds = browsingHistoryRepository.getRecentModelIds()
        seenIds.addAll(viewedIds)

        val favTypes = favoriteRepository.getFavoriteTypeCounts()

        val typeSections = buildTypeSections(seenIds, nsfwLevel, favTypes)
        sections.addAll(typeSections)

        val tagSections = buildTagSections(seenIds, nsfwLevel)
        sections.addAll(tagSections)

        val creatorSection = buildCreatorSection(seenIds, nsfwLevel)
        if (creatorSection != null) sections.add(creatorSection)

        if (sections.isEmpty()) {
            val trendingSection = buildTrendingFallback(seenIds, nsfwLevel)
            if (trendingSection != null) sections.add(trendingSection)
        }

        return sections.shuffled().take(MAX_SECTIONS)
    }

    private suspend fun buildTypeSections(
        seenIds: Set<Long>,
        nsfwLevel: NsfwFilterLevel,
        favTypes: Map<String, Int>,
    ): List<RecommendationSection> {
        val weightedTypes = browsingHistoryRepository.getWeightedTypes()
        val merged = mutableMapOf<String, Double>()
        for ((type, score) in weightedTypes) merged[type] = (merged[type] ?: 0.0) + score
        for ((type, count) in favTypes) {
            merged[type] = (merged[type] ?: 0.0) + count * FAVORITE_WEIGHT
        }

        val topTypes = merged.entries
            .sortedByDescending { it.value }
            .take(MAX_TYPE_SECTIONS)
            .mapNotNull { entry ->
                runCatching { ModelType.valueOf(entry.key) }.getOrNull()
            }

        return topTypes.mapNotNull { modelType ->
            fetchSection(
                title = "Trending ${modelType.name}",
                reason = "Based on your preferences",
                seenIds = seenIds,
                nsfwLevel = nsfwLevel,
                type = modelType,
            )
        }
    }

    private suspend fun buildTagSections(
        seenIds: Set<Long>,
        nsfwLevel: NsfwFilterLevel,
    ): List<RecommendationSection> {
        val weightedTags = browsingHistoryRepository.getWeightedTags()
        val topTags = weightedTags.entries
            .sortedByDescending { it.value }
            .take(MAX_TAG_SECTIONS)
            .map { it.key }

        return topTags.mapNotNull { tag ->
            fetchSection(
                title = "Popular in \"$tag\"",
                reason = "Based on your browsing",
                seenIds = seenIds,
                nsfwLevel = nsfwLevel,
                tag = tag,
            )
        }
    }

    private suspend fun buildCreatorSection(
        seenIds: Set<Long>,
        nsfwLevel: NsfwFilterLevel,
    ): RecommendationSection? {
        val weightedCreators = browsingHistoryRepository.getWeightedCreators()
        val topCreator = weightedCreators.entries
            .maxByOrNull { it.value }?.key ?: return null

        return fetchSection(
            title = "More from $topCreator",
            reason = "Creator you follow",
            seenIds = seenIds,
            nsfwLevel = nsfwLevel,
            username = topCreator,
        )
    }

    private suspend fun fetchSection(
        title: String,
        reason: String,
        seenIds: Set<Long>,
        nsfwLevel: NsfwFilterLevel,
        type: ModelType? = null,
        tag: String? = null,
        username: String? = null,
    ): RecommendationSection? {
        val nsfw = if (nsfwLevel == NsfwFilterLevel.Off) false else null
        val result = modelRepository.getModels(
            type = type,
            tag = tag,
            username = username,
            sort = SortOrder.HighestRated,
            period = TimePeriod.Month,
            limit = SECTION_SIZE + seenIds.size.coerceAtMost(BUFFER),
            nsfw = nsfw,
        )
        val filtered = result.items
            .filterNot { it.id in seenIds }
            .filterNsfwImages(nsfwLevel)
            .take(SECTION_SIZE)
        if (filtered.isEmpty()) return null

        return RecommendationSection(
            title = title,
            reason = reason,
            models = filtered,
        )
    }

    private suspend fun buildTrendingFallback(
        seenIds: Set<Long>,
        nsfwLevel: NsfwFilterLevel,
    ): RecommendationSection? {
        val nsfw = if (nsfwLevel == NsfwFilterLevel.Off) false else null
        val result = modelRepository.getModels(
            sort = SortOrder.MostDownloaded,
            period = TimePeriod.Week,
            limit = SECTION_SIZE + seenIds.size.coerceAtMost(BUFFER),
            nsfw = nsfw,
        )
        val filtered = result.items
            .filterNot { it.id in seenIds }
            .filterNsfwImages(nsfwLevel)
            .take(SECTION_SIZE)
        if (filtered.isEmpty()) return null

        return RecommendationSection(
            title = "Trending This Week",
            reason = "Popular models",
            models = filtered,
        )
    }

    companion object {
        private const val SECTION_SIZE = 10
        private const val BUFFER = 20
        private const val FAVORITE_WEIGHT = 3
        private const val MAX_SECTIONS = 5
        private const val MAX_TYPE_SECTIONS = 2
        private const val MAX_TAG_SECTIONS = 2
    }
}
