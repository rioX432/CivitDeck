package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.RecommendationSection
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.model.filterNsfwImages
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class GetRecommendationsUseCase(
    private val modelRepository: ModelRepository,
    private val favoriteRepository: FavoriteRepository,
    private val browsingHistoryRepository: BrowsingHistoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(): List<RecommendationSection> {
        val sections = mutableListOf<RecommendationSection>()
        val seenIds = mutableSetOf<Long>()
        val nsfwLevel = userPreferencesRepository.observeNsfwFilterLevel().first()

        val favIds = favoriteRepository.getAllFavoriteIds()
        seenIds.addAll(favIds)

        val viewedIds = browsingHistoryRepository.getRecentModelIds()
        seenIds.addAll(viewedIds)

        val typeSection = buildTypeSection(seenIds, nsfwLevel)
        if (typeSection != null) sections.add(typeSection)

        val tagSection = buildTagSection(seenIds, nsfwLevel)
        if (tagSection != null) sections.add(tagSection)

        if (sections.isEmpty()) {
            val trendingSection = buildTrendingFallback(seenIds, nsfwLevel)
            if (trendingSection != null) sections.add(trendingSection)
        }

        return sections
    }

    private suspend fun buildTypeSection(
        seenIds: Set<Long>,
        nsfwLevel: NsfwFilterLevel,
    ): RecommendationSection? {
        val favTypes = favoriteRepository.getFavoriteTypeCounts()
        val browseTypes = browsingHistoryRepository.getRecentTypes()

        val merged = mutableMapOf<String, Int>()
        for ((type, count) in favTypes) merged[type] = (merged[type] ?: 0) + count * FAVORITE_WEIGHT
        for ((type, count) in browseTypes) merged[type] = (merged[type] ?: 0) + count

        val topType = merged.entries.maxByOrNull { it.value }?.key ?: return null
        val modelType = runCatching { ModelType.valueOf(topType) }.getOrNull() ?: return null

        val nsfw = if (nsfwLevel == NsfwFilterLevel.Off) false else null
        val result = modelRepository.getModels(
            type = modelType,
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
            title = "Trending ${modelType.name}",
            reason = "Based on your preferences",
            models = filtered,
        )
    }

    private suspend fun buildTagSection(
        seenIds: Set<Long>,
        nsfwLevel: NsfwFilterLevel,
    ): RecommendationSection? {
        val browseTags = browsingHistoryRepository.getRecentTags()
        val topTag = browseTags.entries.maxByOrNull { it.value }?.key ?: return null

        val nsfw = if (nsfwLevel == NsfwFilterLevel.Off) false else null
        val result = modelRepository.getModels(
            tag = topTag,
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
            title = "Popular in \"$topTag\"",
            reason = "Based on your browsing",
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
    }
}
