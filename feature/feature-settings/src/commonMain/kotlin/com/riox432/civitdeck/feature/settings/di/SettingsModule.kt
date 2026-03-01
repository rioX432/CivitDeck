package com.riox432.civitdeck.feature.settings.di

import com.riox432.civitdeck.domain.repository.AppBehaviorPreferencesRepository
import com.riox432.civitdeck.domain.repository.AuthPreferencesRepository
import com.riox432.civitdeck.domain.repository.ContentFilterPreferencesRepository
import com.riox432.civitdeck.domain.repository.DisplayPreferencesRepository
import com.riox432.civitdeck.domain.repository.StoragePreferencesRepository
import com.riox432.civitdeck.domain.usecase.ClearBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearCacheUseCase
import com.riox432.civitdeck.domain.usecase.EvictCacheUseCase
import com.riox432.civitdeck.domain.usecase.GetCacheInfoUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNetworkStatusUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwBlurSettingsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOfflineCacheEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObservePollingIntervalUseCase
import com.riox432.civitdeck.domain.usecase.ObservePowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.SetAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.SetAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.SetApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.SetCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.SetGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.SetNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwBlurSettingsUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.SetOfflineCacheEnabledUseCase
import com.riox432.civitdeck.domain.usecase.SetPollingIntervalUseCase
import com.riox432.civitdeck.domain.usecase.SetPowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.ValidateApiKeyUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.RemoveExcludedTagUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.UnhideModelUseCase
import com.riox432.civitdeck.feature.settings.data.repository.UserPreferencesRepositoryImpl
import com.riox432.civitdeck.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@Suppress("LongMethod")
val settingsModule = module {
    singleOf(::UserPreferencesRepositoryImpl) {
        bind<ContentFilterPreferencesRepository>()
        bind<DisplayPreferencesRepository>()
        bind<AuthPreferencesRepository>()
        bind<AppBehaviorPreferencesRepository>()
        bind<StoragePreferencesRepository>()
    }
    viewModel {
        val observeNsfwFilter: ObserveNsfwFilterUseCase = get()
        val setNsfwFilter: SetNsfwFilterUseCase = get()
        val observeNsfwBlur: ObserveNsfwBlurSettingsUseCase = get()
        val setNsfwBlur: SetNsfwBlurSettingsUseCase = get()
        val observeSortOrder: ObserveDefaultSortOrderUseCase = get()
        val setSortOrder: SetDefaultSortOrderUseCase = get()
        val observeTimePeriod: ObserveDefaultTimePeriodUseCase = get()
        val setTimePeriod: SetDefaultTimePeriodUseCase = get()
        val observeGridColumns: ObserveGridColumnsUseCase = get()
        val setGridColumns: SetGridColumnsUseCase = get()
        val getHiddenModels: GetHiddenModelsUseCase = get()
        val unhideModel: UnhideModelUseCase = get()
        val getExcludedTags: GetExcludedTagsUseCase = get()
        val addExcludedTag: AddExcludedTagUseCase = get()
        val removeExcludedTag: RemoveExcludedTagUseCase = get()
        val clearSearchHistory: ClearSearchHistoryUseCase = get()
        val clearBrowsingHistory: ClearBrowsingHistoryUseCase = get()
        val clearCache: ClearCacheUseCase = get()
        val observeApiKey: ObserveApiKeyUseCase = get()
        val setApiKey: SetApiKeyUseCase = get()
        val validateApiKey: ValidateApiKeyUseCase = get()
        val observePowerUserMode: ObservePowerUserModeUseCase = get()
        val setPowerUserMode: SetPowerUserModeUseCase = get()
        val observeNotificationsEnabled: ObserveNotificationsEnabledUseCase = get()
        val setNotificationsEnabled: SetNotificationsEnabledUseCase = get()
        val observePollingInterval: ObservePollingIntervalUseCase = get()
        val setPollingInterval: SetPollingIntervalUseCase = get()
        val observeAccentColor: ObserveAccentColorUseCase = get()
        val setAccentColor: SetAccentColorUseCase = get()
        val observeAmoledDarkMode: ObserveAmoledDarkModeUseCase = get()
        val setAmoledDarkMode: SetAmoledDarkModeUseCase = get()
        val observeNetworkStatus: ObserveNetworkStatusUseCase = get()
        val observeOfflineCacheEnabled: ObserveOfflineCacheEnabledUseCase = get()
        val setOfflineCacheEnabled: SetOfflineCacheEnabledUseCase = get()
        val observeCacheSizeLimit: ObserveCacheSizeLimitUseCase = get()
        val setCacheSizeLimit: SetCacheSizeLimitUseCase = get()
        val getCacheInfo: GetCacheInfoUseCase = get()
        val evictCache: EvictCacheUseCase = get()
        SettingsViewModel(
            observeNsfwFilter, setNsfwFilter, observeNsfwBlur, setNsfwBlur,
            observeSortOrder, setSortOrder, observeTimePeriod, setTimePeriod,
            observeGridColumns, setGridColumns, getHiddenModels, unhideModel,
            getExcludedTags, addExcludedTag, removeExcludedTag, clearSearchHistory,
            clearBrowsingHistory, clearCache, observeApiKey, setApiKey,
            validateApiKey, observePowerUserMode, setPowerUserMode,
            observeNotificationsEnabled, setNotificationsEnabled,
            observePollingInterval, setPollingInterval,
            observeAccentColor, setAccentColor, observeAmoledDarkMode, setAmoledDarkMode,
            observeNetworkStatus, observeOfflineCacheEnabled, setOfflineCacheEnabled,
            observeCacheSizeLimit, setCacheSizeLimit, getCacheInfo, evictCache,
        )
    }
}
