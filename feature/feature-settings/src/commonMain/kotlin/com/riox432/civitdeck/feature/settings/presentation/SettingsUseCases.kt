package com.riox432.civitdeck.feature.settings.presentation

import com.riox432.civitdeck.domain.usecase.ClearBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearCacheUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.EvictCacheUseCase
import com.riox432.civitdeck.domain.usecase.GetCacheInfoUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCustomNavShortcutsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNetworkStatusUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOfflineCacheEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObserveThemeModeUseCase
import com.riox432.civitdeck.domain.usecase.SetAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.SetAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.SetCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.SetCustomNavShortcutsUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.SetGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.SetOfflineCacheEnabledUseCase
import com.riox432.civitdeck.domain.usecase.SetThemeModeUseCase
import com.riox432.civitdeck.domain.usecase.UnhideModelUseCase

/**
 * Browsing display preferences: default sort order, time period, and grid columns.
 */
data class DisplayPreferenceUseCases(
    val observeDefaultSortOrder: ObserveDefaultSortOrderUseCase,
    val setDefaultSortOrder: SetDefaultSortOrderUseCase,
    val observeDefaultTimePeriod: ObserveDefaultTimePeriodUseCase,
    val setDefaultTimePeriod: SetDefaultTimePeriodUseCase,
    val observeGridColumns: ObserveGridColumnsUseCase,
    val setGridColumns: SetGridColumnsUseCase,
)

/**
 * Appearance/theming preferences: accent color, AMOLED dark mode, theme mode, and the
 * custom navigation shortcuts.
 */
data class AppearanceUseCases(
    val observeAccentColor: ObserveAccentColorUseCase,
    val setAccentColor: SetAccentColorUseCase,
    val observeAmoledDarkMode: ObserveAmoledDarkModeUseCase,
    val setAmoledDarkMode: SetAmoledDarkModeUseCase,
    val observeThemeMode: ObserveThemeModeUseCase,
    val setThemeMode: SetThemeModeUseCase,
    val observeCustomNavShortcuts: ObserveCustomNavShortcutsUseCase,
    val setCustomNavShortcuts: SetCustomNavShortcutsUseCase,
)

/**
 * Cache management: network/offline status, cache size limit, cache info, and eviction/clear.
 */
data class CacheUseCases(
    val observeNetworkStatus: ObserveNetworkStatusUseCase,
    val observeOfflineCacheEnabled: ObserveOfflineCacheEnabledUseCase,
    val setOfflineCacheEnabled: SetOfflineCacheEnabledUseCase,
    val observeCacheSizeLimit: ObserveCacheSizeLimitUseCase,
    val setCacheSizeLimit: SetCacheSizeLimitUseCase,
    val getCacheInfo: GetCacheInfoUseCase,
    val evictCache: EvictCacheUseCase,
    val clearCache: ClearCacheUseCase,
)

/**
 * Stored-data management: clearing search/browsing history and managing hidden models.
 */
data class StoredDataUseCases(
    val clearSearchHistory: ClearSearchHistoryUseCase,
    val clearBrowsingHistory: ClearBrowsingHistoryUseCase,
    val getHiddenModels: GetHiddenModelsUseCase,
    val unhideModel: UnhideModelUseCase,
)
