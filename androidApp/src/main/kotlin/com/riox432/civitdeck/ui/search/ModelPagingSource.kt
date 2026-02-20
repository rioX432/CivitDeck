package com.riox432.civitdeck.ui.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.filterNsfwImages
import com.riox432.civitdeck.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase

internal class ModelPagingSource(
    private val getModelsUseCase: GetModelsUseCase,
    private val getViewedModelIdsUseCase: GetViewedModelIdsUseCase,
    private val filterState: FilterState,
    private val hiddenModelIds: Set<Long>,
) : PagingSource<String, Model>() {

    private var sortWatermark: Double? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Model> {
        return try {
            val viewedIds = if (filterState.isFreshFindEnabled) {
                getViewedModelIdsUseCase()
            } else {
                emptySet()
            }

            val accumulated = mutableListOf<Model>()
            val seenIds = mutableSetOf<Long>()
            var cursor = params.key
            var nextCursor: String? = null
            val targetSize = params.loadSize
            val pageWatermark = sortWatermark

            repeat(MAX_FETCH_ITERATIONS) {
                if (accumulated.size >= targetSize) return@repeat

                val remaining = targetSize - accumulated.size
                val result = getModelsUseCase(
                    query = filterState.query.ifBlank { null },
                    tag = filterState.includedTags.firstOrNull(),
                    type = filterState.selectedType,
                    sort = filterState.selectedSort,
                    period = filterState.selectedPeriod,
                    baseModels = filterState.selectedBaseModels.toList().ifEmpty { null },
                    cursor = cursor,
                    limit = remaining.coerceAtLeast(PAGE_SIZE),
                    nsfw = if (filterState.nsfwFilterLevel == NsfwFilterLevel.Off) false else null,
                )

                var filtered = applyClientFilters(result.items, viewedIds)
                if (pageWatermark != null) {
                    filtered = filtered.filter { sortValueOf(it) <= pageWatermark }
                }
                for (model in filtered) {
                    if (seenIds.add(model.id)) {
                        accumulated.add(model)
                    }
                }
                nextCursor = result.metadata.nextCursor
                if (nextCursor == null || nextCursor == cursor) return@repeat
                cursor = nextCursor
            }

            accumulated.sortByDescending { sortValueOf(it) }

            if (accumulated.isNotEmpty()) {
                sortWatermark = accumulated.minOf { sortValueOf(it) }
            }

            LoadResult.Page(
                data = accumulated,
                prevKey = null,
                nextKey = nextCursor,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Model>): String? = null

    private fun applyClientFilters(
        models: List<Model>,
        viewedIds: Set<Long>,
    ): List<Model> {
        var filtered = models.filterNsfwImages(filterState.nsfwFilterLevel)
        if (filterState.isFreshFindEnabled) {
            filtered = filtered.filter { it.id !in viewedIds }
        }
        if (filterState.includedTags.size > 1) {
            val remaining = filterState.includedTags.drop(1).map { it.lowercase() }.toSet()
            filtered = filtered.filter { model ->
                val modelTags = model.tags.map { it.lowercase() }.toSet()
                modelTags.containsAll(remaining)
            }
        }
        if (filterState.excludedTags.isNotEmpty()) {
            val excluded = filterState.excludedTags.toSet()
            filtered = filtered.filter { model ->
                model.tags.none { it.lowercase() in excluded }
            }
        }
        if (hiddenModelIds.isNotEmpty()) {
            filtered = filtered.filter { it.id !in hiddenModelIds }
        }
        return filtered
    }

    private fun sortValueOf(model: Model): Double = when (filterState.selectedSort) {
        SortOrder.MostDownloaded -> model.stats.downloadCount.toDouble()
        SortOrder.HighestRated -> model.stats.rating
        SortOrder.Newest -> model.id.toDouble()
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val MAX_FETCH_ITERATIONS = 5
    }
}
