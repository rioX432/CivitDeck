package com.riox432.civitdeck.feature.search.domain.usecase

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.RecommendationSection
import com.riox432.civitdeck.domain.model.RecommendationSectionType
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.model.filterNsfwImages
import com.riox432.civitdeck.domain.repository.AppBehaviorPreferencesRepository
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import com.riox432.civitdeck.domain.repository.ContentFilterPreferencesRepository
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.usecase.QualityScoreCalculator
import kotlinx.coroutines.flow.first

class GetRecommendationsUseCase(
    private val modelRepository: ModelRepository,
    private val favoriteRepository: FavoriteRepository,
    private val browsingHistoryRepository: BrowsingHistoryRepository,
    private val userPreferencesRepository: ContentFilterPreferencesRepository,
    private val appBehaviorRepository: AppBehaviorPreferencesRepository,
) {
    suspend operator fun invoke(): List<RecommendationSection> {
        val sections = mutableListOf<RecommendationSection>()
        val seenIds = mutableSetOf<Long>()
        val nsfwLevel = userPreferencesRepository.observeNsfwFilterLevel().first()
        val qualityThreshold = appBehaviorRepository.observeFeedQualityThreshold().first()

        val favIds = favoriteRepository.getAllFavoriteIds()
        seenIds.addAll(favIds)

        val viewedIds = browsingHistoryRepository.getRecentModelIds()
        seenIds.addAll(viewedIds)

        val favTypes = favoriteRepository.getFavoriteTypeCounts()

        // Affinity-based "Recommended for You" section
        val affinitySection = buildAffinitySection(seenIds, nsfwLevel, favTypes, qualityThreshold)
        if (affinitySection != null) sections.add(affinitySection)

        val typeSections = buildTypeSections(seenIds, nsfwLevel, favTypes, qualityThreshold)
        sections.addAll(typeSections)

        val tagSections = buildTagSections(seenIds, nsfwLevel, qualityThreshold)
        sections.addAll(tagSections)

        val creatorSection = buildCreatorSection(seenIds, nsfwLevel, qualityThreshold)
        if (creatorSection != null) sections.add(creatorSection)

        // Trending velocity section — models gaining traction fast
        val trendingSection = buildTrendingVelocitySection(seenIds, nsfwLevel, qualityThreshold)
        if (trendingSection != null) sections.add(trendingSection)

        // Diversity: inject exploration section if too homogeneous
        val explorationSection = buildExplorationSection(seenIds, nsfwLevel, sections, qualityThreshold)
        if (explorationSection != null) sections.add(explorationSection)

        if (sections.isEmpty()) {
            val fallback = buildTrendingFallback(seenIds, nsfwLevel, qualityThreshold)
            if (fallback != null) sections.add(fallback)
        }

        return sections.take(MAX_SECTIONS)
    }

    /**
     * "Recommended for You" — combines top affinity tag + type into a single query
     * for the user's most engaged content profile.
     */
    private suspend fun buildAffinitySection(
        seenIds: Set<Long>,
        nsfwLevel: NsfwFilterLevel,
        favTypes: Map<String, Int>,
        qualityThreshold: Int,
    ): RecommendationSection? {
        val weightedTags = browsingHistoryRepository.getWeightedTags()
        val topTag = weightedTags.entries.maxByOrNull { it.value }?.key ?: return null

        val weightedTypes = browsingHistoryRepository.getWeightedTypes()
        val mergedTypes = mutableMapOf<String, Double>()
        for ((type, score) in weightedTypes) mergedTypes[type] = (mergedTypes[type] ?: 0.0) + score
        for ((type, count) in favTypes) {
            mergedTypes[type] = (mergedTypes[type] ?: 0.0) + count * FAVORITE_WEIGHT
        }
        val topType = mergedTypes.entries
            .maxByOrNull { it.value }
            ?.let { runCatching { ModelType.valueOf(it.key) }.getOrNull() }

        return fetchSection(
            title = "Recommended for You",
            reason = "Based on your activity",
            seenIds = seenIds,
            nsfwLevel = nsfwLevel,
            type = topType,
            tag = topTag,
            sectionType = RecommendationSectionType.PERSONALIZED,
            qualityThreshold = qualityThreshold,
        )
    }

    private suspend fun buildTypeSections(
        seenIds: Set<Long>,
        nsfwLevel: NsfwFilterLevel,
        favTypes: Map<String, Int>,
        qualityThreshold: Int,
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
                qualityThreshold = qualityThreshold,
            )
        }
    }

    private suspend fun buildTagSections(
        seenIds: Set<Long>,
        nsfwLevel: NsfwFilterLevel,
        qualityThreshold: Int,
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
                qualityThreshold = qualityThreshold,
            )
        }
    }

    private suspend fun buildCreatorSection(
        seenIds: Set<Long>,
        nsfwLevel: NsfwFilterLevel,
        qualityThreshold: Int,
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
            qualityThreshold = qualityThreshold,
        )
    }

    /**
     * Trending velocity — models sorted by most downloads this day,
     * representing fast-rising content rather than all-time popular.
     */
    private suspend fun buildTrendingVelocitySection(
        seenIds: Set<Long>,
        nsfwLevel: NsfwFilterLevel,
        qualityThreshold: Int,
    ): RecommendationSection? {
        val nsfw = if (nsfwLevel == NsfwFilterLevel.Off) false else null
        val result = modelRepository.getModels(
            sort = SortOrder.MostDownloaded,
            period = TimePeriod.Day,
            limit = SECTION_SIZE + seenIds.size.coerceAtMost(BUFFER),
            nsfw = nsfw,
        )
        val filtered = result.items
            .filterNot { it.id in seenIds }
            .filterNsfwImages(nsfwLevel)
            .filterByQuality(qualityThreshold)
            .take(SECTION_SIZE)
        if (filtered.isEmpty()) return null

        return RecommendationSection(
            title = "Rising Fast",
            reason = "Trending in the last 24 hours",
            models = filtered,
            sectionType = RecommendationSectionType.TRENDING,
        )
    }

    /**
     * Diversity control — if all personalized sections share the same type,
     * inject an exploration section from a different category to prevent filter bubbles.
     */
    private suspend fun buildExplorationSection(
        seenIds: Set<Long>,
        nsfwLevel: NsfwFilterLevel,
        existingSections: List<RecommendationSection>,
        qualityThreshold: Int,
    ): RecommendationSection? {
        if (existingSections.size < MIN_SECTIONS_FOR_EXPLORATION) return null

        // Collect types already represented in sections
        val coveredTypes = existingSections.flatMap { section ->
            section.models.map { it.type }
        }.toSet()

        // Find a type NOT yet represented
        val unexploredType = ModelType.entries
            .filter { it !in coveredTypes }
            .randomOrNull() ?: return null

        return fetchSection(
            title = "Explore ${unexploredType.name}",
            reason = "Something different to try",
            seenIds = seenIds,
            nsfwLevel = nsfwLevel,
            type = unexploredType,
            sort = SortOrder.MostDownloaded,
            period = TimePeriod.Week,
            sectionType = RecommendationSectionType.EXPLORATION,
            qualityThreshold = qualityThreshold,
        )
    }

    @Suppress("LongParameterList")
    private suspend fun fetchSection(
        title: String,
        reason: String,
        seenIds: Set<Long>,
        nsfwLevel: NsfwFilterLevel,
        type: ModelType? = null,
        tag: String? = null,
        username: String? = null,
        sort: SortOrder? = SortOrder.HighestRated,
        period: TimePeriod? = TimePeriod.Month,
        sectionType: RecommendationSectionType = RecommendationSectionType.PERSONALIZED,
        qualityThreshold: Int = 0,
    ): RecommendationSection? {
        val nsfw = if (nsfwLevel == NsfwFilterLevel.Off) false else null
        val result = modelRepository.getModels(
            type = type,
            tag = tag,
            username = username,
            sort = sort,
            period = period,
            limit = SECTION_SIZE + seenIds.size.coerceAtMost(BUFFER),
            nsfw = nsfw,
        )
        val filtered = result.items
            .filterNot { it.id in seenIds }
            .filterNsfwImages(nsfwLevel)
            .filterByQuality(qualityThreshold)
            .take(SECTION_SIZE)
        if (filtered.isEmpty()) return null

        return RecommendationSection(
            title = title,
            reason = reason,
            models = filtered,
            sectionType = sectionType,
        )
    }

    private suspend fun buildTrendingFallback(
        seenIds: Set<Long>,
        nsfwLevel: NsfwFilterLevel,
        qualityThreshold: Int,
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
            .filterByQuality(qualityThreshold)
            .take(SECTION_SIZE)
        if (filtered.isEmpty()) return null

        return RecommendationSection(
            title = "Trending This Week",
            reason = "Popular models",
            models = filtered,
            sectionType = RecommendationSectionType.TRENDING,
        )
    }

    /**
     * Filters models below the quality threshold.
     * Threshold of 0 means no filtering (disabled).
     */
    private fun List<Model>.filterByQuality(threshold: Int): List<Model> {
        if (threshold <= 0) return this
        return filter { QualityScoreCalculator.calculate(it.stats) >= threshold }
    }

    companion object {
        private const val SECTION_SIZE = 10
        private const val BUFFER = 20
        private const val FAVORITE_WEIGHT = 3
        private const val MAX_SECTIONS = 6
        private const val MAX_TYPE_SECTIONS = 2
        private const val MAX_TAG_SECTIONS = 2
        private const val MIN_SECTIONS_FOR_EXPLORATION = 2
    }
}
