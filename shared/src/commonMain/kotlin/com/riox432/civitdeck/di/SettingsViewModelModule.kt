package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.ClearBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearCacheUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.EvictCacheUseCase
import com.riox432.civitdeck.domain.usecase.GetCacheInfoUseCase
import com.riox432.civitdeck.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCustomNavShortcutsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveFrontDoorModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGenerationNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNetworkStatusUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwBlurSettingsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOfflineCacheEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObservePollingIntervalUseCase
import com.riox432.civitdeck.domain.usecase.ObservePowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveQualityThresholdUseCase
import com.riox432.civitdeck.domain.usecase.ObserveThemeModeUseCase
import com.riox432.civitdeck.domain.usecase.RemoveExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.SetAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.SetAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.SetApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.SetCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.SetCustomNavShortcutsUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.SetFrontDoorModeUseCase
import com.riox432.civitdeck.domain.usecase.SetGenerationNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.SetGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.SetNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwBlurSettingsUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.SetOfflineCacheEnabledUseCase
import com.riox432.civitdeck.domain.usecase.SetPollingIntervalUseCase
import com.riox432.civitdeck.domain.usecase.SetPowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.SetQualityThresholdUseCase
import com.riox432.civitdeck.domain.usecase.SetThemeModeUseCase
import com.riox432.civitdeck.domain.usecase.UnhideModelUseCase
import com.riox432.civitdeck.domain.usecase.ValidateApiKeyUseCase
import com.riox432.civitdeck.feature.settings.presentation.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.AppearanceUseCases
import com.riox432.civitdeck.feature.settings.presentation.AuthSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.CacheUseCases
import com.riox432.civitdeck.feature.settings.presentation.ContentFilterSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.DisplayPreferenceUseCases
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.StorageSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.StoredDataUseCases
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsViewModelModule = module {
    viewModel {
        ContentFilterSettingsViewModel(
            observeNsfwFilterUseCase = get<ObserveNsfwFilterUseCase>(),
            setNsfwFilterUseCase = get<SetNsfwFilterUseCase>(),
            observeNsfwBlurSettingsUseCase = get<ObserveNsfwBlurSettingsUseCase>(),
            setNsfwBlurSettingsUseCase = get<SetNsfwBlurSettingsUseCase>(),
            observeFrontDoorModeUseCase = get<ObserveFrontDoorModeUseCase>(),
            setFrontDoorModeUseCase = get<SetFrontDoorModeUseCase>(),
            getHiddenModelsUseCase = get<GetHiddenModelsUseCase>(),
            unhideModelUseCase = get<UnhideModelUseCase>(),
            getExcludedTagsUseCase = get<GetExcludedTagsUseCase>(),
            addExcludedTagUseCase = get<AddExcludedTagUseCase>(),
            removeExcludedTagUseCase = get<RemoveExcludedTagUseCase>(),
        )
    }
    factory {
        DisplayPreferenceUseCases(
            observeDefaultSortOrder = get<ObserveDefaultSortOrderUseCase>(),
            setDefaultSortOrder = get<SetDefaultSortOrderUseCase>(),
            observeDefaultTimePeriod = get<ObserveDefaultTimePeriodUseCase>(),
            setDefaultTimePeriod = get<SetDefaultTimePeriodUseCase>(),
            observeGridColumns = get<ObserveGridColumnsUseCase>(),
            setGridColumns = get<SetGridColumnsUseCase>(),
        )
    }
    factory {
        AppearanceUseCases(
            observeAccentColor = get<ObserveAccentColorUseCase>(),
            setAccentColor = get<SetAccentColorUseCase>(),
            observeAmoledDarkMode = get<ObserveAmoledDarkModeUseCase>(),
            setAmoledDarkMode = get<SetAmoledDarkModeUseCase>(),
            observeThemeMode = get<ObserveThemeModeUseCase>(),
            setThemeMode = get<SetThemeModeUseCase>(),
            observeCustomNavShortcuts = get<ObserveCustomNavShortcutsUseCase>(),
            setCustomNavShortcuts = get<SetCustomNavShortcutsUseCase>(),
        )
    }
    viewModel {
        DisplaySettingsViewModel(
            displayUseCases = get(),
            appearanceUseCases = get(),
        )
    }
    viewModel {
        AppBehaviorSettingsViewModel(
            observePowerUserModeUseCase = get<ObservePowerUserModeUseCase>(),
            setPowerUserModeUseCase = get<SetPowerUserModeUseCase>(),
            observeNotificationsEnabledUseCase = get<ObserveNotificationsEnabledUseCase>(),
            setNotificationsEnabledUseCase = get<SetNotificationsEnabledUseCase>(),
            observePollingIntervalUseCase = get<ObservePollingIntervalUseCase>(),
            setPollingIntervalUseCase = get<SetPollingIntervalUseCase>(),
            observeQualityThresholdUseCase = get<ObserveQualityThresholdUseCase>(),
            setQualityThresholdUseCase = get<SetQualityThresholdUseCase>(),
            observeGenNotifUseCase = get<ObserveGenerationNotificationsEnabledUseCase>(),
            setGenNotifUseCase = get<SetGenerationNotificationsEnabledUseCase>(),
        )
    }
    viewModel {
        AuthSettingsViewModel(
            observeApiKeyUseCase = get<ObserveApiKeyUseCase>(),
            setApiKeyUseCase = get<SetApiKeyUseCase>(),
            validateApiKeyUseCase = get<ValidateApiKeyUseCase>(),
        )
    }
    factory {
        CacheUseCases(
            observeNetworkStatus = get<ObserveNetworkStatusUseCase>(),
            observeOfflineCacheEnabled = get<ObserveOfflineCacheEnabledUseCase>(),
            setOfflineCacheEnabled = get<SetOfflineCacheEnabledUseCase>(),
            observeCacheSizeLimit = get<ObserveCacheSizeLimitUseCase>(),
            setCacheSizeLimit = get<SetCacheSizeLimitUseCase>(),
            getCacheInfo = get<GetCacheInfoUseCase>(),
            evictCache = get<EvictCacheUseCase>(),
            clearCache = get<ClearCacheUseCase>(),
        )
    }
    factory {
        StoredDataUseCases(
            clearSearchHistory = get<ClearSearchHistoryUseCase>(),
            clearBrowsingHistory = get<ClearBrowsingHistoryUseCase>(),
            getHiddenModels = get<GetHiddenModelsUseCase>(),
            unhideModel = get<UnhideModelUseCase>(),
        )
    }
    viewModel {
        StorageSettingsViewModel(
            cacheUseCases = get(),
            storedDataUseCases = get(),
        )
    }
}
