package com.riox432.civitdeck.feature.search.presentation

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.filterNsfwImages
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.QualityScoreCalculator
import com.riox432.civitdeck.domain.util.LoadResult
import com.riox432.civitdeck.feature.search.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.MultiSourceSearchUseCase
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles page loading logic for model search, including CivitAI-only and multi-source search.
 */
internal class SearchPageLoader(
    private val getModelsUseCase: GetModelsUseCase,
    private val multiSourceSearchUseCase: MultiSourceSearchUseCase,
    private val getViewedModelIdsUseCase: GetViewedModelIdsUseCase,
    private val hiddenModelIds: StateFlow<Set<Long>>,
) {
    var sortWatermark: Double? = null
    var multiSourcePage: Int = 1

    fun resetPagination() {
        sortWatermark = null
        multiSourcePage = 1
    }

    suspend fun loadPage(
        filter: FilterState,
        cursor: String?,
        limit: Int,
    ): LoadResult<Model> {
        val isCivitaiOnly = filter.selectedSources == setOf(ModelSource.CIVITAI)
        return if (isCivitaiOnly) {
            loadCivitaiPage(filter, cursor, limit)
        } else {
            loadMultiSourcePage(filter, cursor, limit)
        }
    }

    private suspend fun loadCivitaiPage(
        filter: FilterState,
        cursor: String?,
        limit: Int,
    ): LoadResult<Model> {
        val viewedIds = if (filter.isFreshFindEnabled) {
            getViewedModelIdsUseCase()
        } else {
            emptySet()
        }

        val accumulated = mutableListOf<Model>()
        val seenIds = mutableSetOf<Long>()
        var currentCursor = cursor
        var nextCursor: String? = null
        val pageWatermark = sortWatermark

        repeat(MAX_FETCH_ITERATIONS) {
            if (accumulated.size >= limit) return@repeat

            val remaining = limit - accumulated.size
            val result = getModelsUseCase(
                query = filter.query.ifBlank { null },
                tag = filter.includedTags.firstOrNull(),
                type = filter.selectedType,
                sort = filter.selectedSort,
                period = filter.selectedPeriod,
                baseModels = filter.selectedBaseModels.toList().ifEmpty { null },
                cursor = currentCursor,
                limit = remaining.coerceAtLeast(PAGE_SIZE),
                nsfw = if (filter.nsfwFilterLevel == NsfwFilterLevel.Off) false else null,
            )

            var filtered = applyClientFilters(result.items, filter, viewedIds)
            if (pageWatermark != null) {
                filtered = filtered.filter { sortValueOf(it, filter) <= pageWatermark }
            }
            for (model in filtered) {
                if (seenIds.add(model.id)) {
                    accumulated.add(model)
                }
            }
            nextCursor = result.metadata.nextCursor
            if (nextCursor == null || nextCursor == currentCursor) return@repeat
            currentCursor = nextCursor
        }

        accumulated.sortByDescending { sortValueOf(it, filter) }

        if (accumulated.isNotEmpty()) {
            sortWatermark = accumulated.minOf { sortValueOf(it, filter) }
        }

        return LoadResult(items = accumulated, nextCursor = nextCursor)
    }

    private suspend fun loadMultiSourcePage(
        filter: FilterState,
        cursor: String?,
        limit: Int,
    ): LoadResult<Model> {
        val viewedIds = if (filter.isFreshFindEnabled) {
            getViewedModelIdsUseCase()
        } else {
            emptySet()
        }

        if (cursor == null) multiSourcePage = 1
        val result = multiSourceSearchUseCase(
            query = filter.query.ifBlank { null },
            selectedSources = filter.selectedSources,
            cursor = cursor,
            page = multiSourcePage,
            limit = limit.coerceAtLeast(PAGE_SIZE),
        )

        val filtered = applyClientFilters(result.models, filter, viewedIds)
        multiSourcePage++

        return LoadResult(items = filtered, nextCursor = result.nextCursor)
    }

    private fun applyClientFilters(
        models: List<Model>,
        filter: FilterState,
        viewedIds: Set<Long>,
    ): List<Model> {
        val hiddenIds = hiddenModelIds.value
        var filtered = models.filterNsfwImages(filter.nsfwFilterLevel)
        if (filter.isFreshFindEnabled) {
            filtered = filtered.filter { it.id !in viewedIds }
        }
        if (filter.includedTags.size > 1) {
            val remaining = filter.includedTags.drop(1).map { it.lowercase() }.toSet()
            filtered = filtered.filter { model ->
                val modelTags = model.tags.map { it.lowercase() }.toSet()
                modelTags.containsAll(remaining)
            }
        }
        if (filter.excludedTags.isNotEmpty()) {
            val excluded = filter.excludedTags.toSet()
            filtered = filtered.filter { model ->
                model.tags.none { it.lowercase() in excluded }
            }
        }
        if (hiddenIds.isNotEmpty()) {
            filtered = filtered.filter { it.id !in hiddenIds }
        }
        if (filter.isQualityFilterEnabled && filter.qualityThreshold > 0) {
            filtered = filtered.filter { model ->
                QualityScoreCalculator.calculate(model.stats) >= filter.qualityThreshold
            }
        }
        return filtered
    }

    companion object {
        internal const val PAGE_SIZE = 20
        private const val MAX_FETCH_ITERATIONS = 5
    }
}

private fun sortValueOf(model: Model, filter: FilterState): Double =
    when (filter.selectedSort) {
        SortOrder.MostDownloaded -> model.stats.downloadCount.toDouble()
        SortOrder.HighestRated -> model.stats.rating
        SortOrder.Newest -> model.id.toDouble()
        SortOrder.Quality -> QualityScoreCalculator.calculate(model.stats).toDouble()
    }
