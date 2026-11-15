package com.riox432.civitdeck.feature.search.presentation

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelSearchQuery
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.filterNsfwImages
import com.riox432.civitdeck.domain.model.includeNsfwModels
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.QualityScoreCalculator
import com.riox432.civitdeck.domain.util.LoadResult
import com.riox432.civitdeck.feature.search.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.MultiSourceSearchContinuation
import com.riox432.civitdeck.feature.search.domain.usecase.MultiSourceSearchUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles page loading logic for model search, including CivitAI-only and multi-source search.
 *
 * [onSourceNoticesChanged] receives the current session's [SearchSourceNotice]s: empty when a
 * session starts, then the provider limitations and failures after each committed multi-source page.
 */
internal class SearchPageLoader(
    private val getModelsUseCase: GetModelsUseCase,
    private val multiSourceSearchUseCase: MultiSourceSearchUseCase,
    private val getViewedModelIdsUseCase: GetViewedModelIdsUseCase,
    private val hiddenModelIds: StateFlow<Set<Long>>,
    private val onSourceNoticesChanged: (List<SearchSourceNotice>) -> Unit = {},
) {
    var sortWatermark: Double? = null

    // Multi-source session state. PaginatedLoader only carries one opaque cursor, so the
    // per-provider continuation lives here and the cursor merely names the session page it
    // belongs to.
    private var multiSourceSession = 0L
    private var multiSourcePageCount = 0
    private var multiSourceToken: String? = null
    private var multiSourceSeenKeys: Set<String> = emptySet()
    internal var multiSourceContinuation: MultiSourceSearchContinuation? = null
        private set
    internal var sourceNotices: List<SearchSourceNotice> = emptyList()
        private set

    fun resetPagination() {
        sortWatermark = null
        clearMultiSourceSession()
    }

    private fun clearMultiSourceSession() {
        multiSourceSession++
        multiSourcePageCount = 0
        multiSourceToken = null
        multiSourceSeenKeys = emptySet()
        multiSourceContinuation = null
        publishSourceNotices(emptyList())
    }

    private fun publishSourceNotices(notices: List<SearchSourceNotice>) {
        if (notices == sourceNotices) return
        sourceNotices = notices
        onSourceNoticesChanged(notices)
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
        val sortKey = clientSortKey(filter.selectedSort)
        // Only the download count follows the API's order across pages; a watermark on any
        // other key discards valid next-page results.
        val watermarkKey = sortKey.takeIf { filter.selectedSort == SortOrder.MostDownloaded }
        val pageWatermark = sortWatermark

        var fetches = 0
        while (fetches < MAX_FETCH_ITERATIONS && accumulated.size < limit) {
            fetches++
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
                nsfw = filter.nsfwFilterLevel.includeNsfwModels(),
            )

            var filtered = applyClientFilters(result.items, filter, viewedIds)
            if (watermarkKey != null && pageWatermark != null) {
                filtered = filtered.filter { watermarkKey(it) <= pageWatermark }
            }
            for (model in filtered) {
                if (seenIds.add(model.id)) {
                    accumulated.add(model)
                }
            }
            nextCursor = result.metadata.nextCursor
            if (nextCursor == null || nextCursor == currentCursor) break
            currentCursor = nextCursor
        }

        if (sortKey != null) accumulated.sortByDescending(sortKey)

        if (watermarkKey != null && accumulated.isNotEmpty()) {
            sortWatermark = accumulated.minOf(watermarkKey)
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

        if (cursor == null) clearMultiSourceSession()
        // PaginatedLoader only hands back the token this session issued, so a mismatch is a bug;
        // failing visibly lets Refresh recover, where cancelling would leave it stuck loading.
        check(cursor == multiSourceToken) { "Unknown multi-source cursor" }
        val session = multiSourceSession
        val result = try {
            multiSourceSearchUseCase(
                query = filter.query.ifBlank { null },
                selectedSources = filter.selectedSources,
                continuation = multiSourceContinuation,
                limit = limit.coerceAtLeast(PAGE_SIZE),
                civitaiQuery = filter.toCivitaiQuery(),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            ensureCurrentSession(session)
            throw e
        }
        ensureCurrentSession(session)

        val seenKeys = multiSourceSeenKeys.toMutableSet()
        // The merged page lists Civitai first, so filtering each part keeps the merged order.
        val (civitaiModels, externalModels) = result.models.partition { it.source == ModelSource.CIVITAI }
        val civitaiFiltered = applyClientFilters(civitaiModels, filter, viewedIds).let { models ->
            clientSortKey(filter.selectedSort)?.let { models.sortedByDescending(it) } ?: models
        }
        val filtered = civitaiFiltered + SearchProviderPolicy.applyExternalFilters(externalModels, filter)
        // A model without a stable key has no exact identity, so it can never be proven a duplicate.
        val items = filtered.filter { model ->
            model.reference.stableKey?.let(seenKeys::add) ?: true
        }
        val nextToken = if (result.hasMore) {
            multiSourcePageCount++
            "$MULTI_SOURCE_TOKEN_PREFIX$session:$multiSourcePageCount"
        } else {
            null
        }
        multiSourceContinuation = result.continuation
        multiSourceSeenKeys = seenKeys
        multiSourceToken = nextToken
        publishSourceNotices(sourceNoticesFor(filter, result.continuation))

        return LoadResult(items = items, nextCursor = nextToken)
    }

    /**
     * Cancels a request whose session was reset while it ran. PaginatedLoader applies any
     * returned page or error to its current state, so a stale outcome must never reach it.
     */
    private suspend fun ensureCurrentSession(session: Long) {
        currentCoroutineContext().ensureActive()
        if (session != multiSourceSession) {
            throw CancellationException("Multi-source session was reset")
        }
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
        if (filter.isQualityFilterActive()) {
            filtered = filtered.filter { model ->
                QualityScoreCalculator.calculate(model.stats) >= filter.qualityThreshold
            }
        }
        return filtered
    }

    companion object {
        internal const val PAGE_SIZE = 20
        private const val MAX_FETCH_ITERATIONS = 5
        private const val MULTI_SOURCE_TOKEN_PREFIX = "multi-source:"
    }
}

/**
 * The Civitai request for [FilterState], matching what the Civitai-only branch sends: the first
 * included tag goes to the API and the rest are filtered client-side.
 */
private fun FilterState.toCivitaiQuery(): ModelSearchQuery = ModelSearchQuery(
    query = query.ifBlank { null },
    tag = includedTags.firstOrNull(),
    type = selectedType,
    sort = selectedSort,
    period = selectedPeriod,
    baseModels = selectedBaseModels.toList().ifEmpty { null },
    nsfw = nsfwFilterLevel.includeNsfwModels(),
)

/** Provider limitations of [filter] plus every provider that has failed in this session. */
private fun sourceNoticesFor(
    filter: FilterState,
    continuation: MultiSourceSearchContinuation,
): List<SearchSourceNotice> {
    val failures = continuation.failedSources
        .sortedBy { it.ordinal }
        .map { SearchSourceNotice(it, SearchSourceNoticeReason.SOURCE_FAILED) }
    return failures + SearchProviderPolicy.capabilityNotices(filter)
}

/**
 * Client-side ordering key for [sort], or null to keep the API's cursor order: the API orders
 * Newest by a server timestamp and Highest Rated by thumbsUpCount, and no [Model] field
 * reproduces either order.
 */
private fun clientSortKey(sort: SortOrder): ((Model) -> Double)? =
    when (sort) {
        SortOrder.MostDownloaded -> { model -> model.stats.downloadCount.toDouble() }
        SortOrder.Quality -> { model -> QualityScoreCalculator.calculate(model.stats).toDouble() }
        SortOrder.Newest, SortOrder.HighestRated -> null
    }
