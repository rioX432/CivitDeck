package com.riox432.civitdeck.feature.search.presentation

import com.riox432.civitdeck.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveFavoritesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOwnedModelHashesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveQualityThresholdUseCase
import com.riox432.civitdeck.domain.usecase.RemoveExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.AddSearchHistoryUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.DeleteSavedSearchFilterUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.DeleteSearchHistoryItemUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetHiddenModelIdsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetRecommendationsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.HideModelUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.MultiSourceSearchUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.ObserveSavedSearchFiltersUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.ObserveSearchHistoryUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.SaveSearchFilterUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.TrackRecommendationClickUseCase

/**
 * Core search & pagination operations: fetching models, multi-source search,
 * recommendations, viewed-model tracking.
 */
data class SearchCoreUseCases(
    val getModels: GetModelsUseCase,
    val multiSourceSearch: MultiSourceSearchUseCase,
    val getRecommendations: GetRecommendationsUseCase,
    val getViewedModelIds: GetViewedModelIdsUseCase,
    val trackRecommendationClick: TrackRecommendationClickUseCase,
)

/**
 * Search history: observing, adding, deleting individual items, and clearing all.
 */
data class SearchHistoryUseCases(
    val observeSearchHistory: ObserveSearchHistoryUseCase,
    val addSearchHistory: AddSearchHistoryUseCase,
    val deleteSearchHistoryItem: DeleteSearchHistoryItemUseCase,
    val clearSearchHistory: ClearSearchHistoryUseCase,
)

/**
 * Filter management: excluded tags, hidden models, and saved search filters.
 */
data class SearchFilterUseCases(
    val getExcludedTags: GetExcludedTagsUseCase,
    val addExcludedTag: AddExcludedTagUseCase,
    val removeExcludedTag: RemoveExcludedTagUseCase,
    val getHiddenModelIds: GetHiddenModelIdsUseCase,
    val hideModel: HideModelUseCase,
    val observeSavedSearchFilters: ObserveSavedSearchFiltersUseCase,
    val saveSearchFilter: SaveSearchFilterUseCase,
    val deleteSavedSearchFilter: DeleteSavedSearchFilterUseCase,
)

/**
 * Settings observers driving the search experience: NSFW filter, grid columns,
 * default sort/period, and the quality threshold.
 */
data class SearchPreferencesUseCases(
    val observeNsfwFilter: ObserveNsfwFilterUseCase,
    val observeGridColumns: ObserveGridColumnsUseCase,
    val observeDefaultSortOrder: ObserveDefaultSortOrderUseCase,
    val observeDefaultTimePeriod: ObserveDefaultTimePeriodUseCase,
    val observeQualityThreshold: ObserveQualityThresholdUseCase,
)

/**
 * Favorites & ownership: toggling favorites, observing favorites, and owned hashes.
 */
data class SearchFavoritesUseCases(
    val toggleFavorite: ToggleFavoriteUseCase,
    val observeFavorites: ObserveFavoritesUseCase,
    val observeOwnedModelHashes: ObserveOwnedModelHashesUseCase,
)
