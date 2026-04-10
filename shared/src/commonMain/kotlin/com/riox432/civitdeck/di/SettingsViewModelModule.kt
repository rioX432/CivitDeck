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
import com.riox432.civitdeck.feature.settings.presentation.AuthSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.ContentFilterSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.StorageSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@Suppress("LongMethod")
val settingsViewModelModule = module {
    viewModel {
        ContentFilterSettingsViewModel(
            observeNsfwFilterUseCase = get<ObserveNsfwFilterUseCase>(),
            setNsfwFilterUseCase = get<SetNsfwFilterUseCase>(),
            observeNsfwBlurSettingsUseCase = get<ObserveNsfwBlurSettingsUseCase>(),
            setNsfwBlurSettingsUseCase = get<SetNsfwBlurSettingsUseCase>(),
            getHiddenModelsUseCase = get<GetHiddenModelsUseCase>(),
            unhideModelUseCase = get<UnhideModelUseCase>(),
            getExcludedTagsUseCase = get<GetExcludedTagsUseCase>(),
            addExcludedTagUseCase = get<AddExcludedTagUseCase>(),
            removeExcludedTagUseCase = get<RemoveExcludedTagUseCase>(),
        )
    }
    viewModel {
        DisplaySettingsViewModel(
            observeDefaultSortOrderUseCase = get<ObserveDefaultSortOrderUseCase>(),
            setDefaultSortOrderUseCase = get<SetDefaultSortOrderUseCase>(),
            observeDefaultTimePeriodUseCase = get<ObserveDefaultTimePeriodUseCase>(),
            setDefaultTimePeriodUseCase = get<SetDefaultTimePeriodUseCase>(),
            observeGridColumnsUseCase = get<ObserveGridColumnsUseCase>(),
            setGridColumnsUseCase = get<SetGridColumnsUseCase>(),
            observeAccentColorUseCase = get<ObserveAccentColorUseCase>(),
            setAccentColorUseCase = get<SetAccentColorUseCase>(),
            observeAmoledDarkModeUseCase = get<ObserveAmoledDarkModeUseCase>(),
            setAmoledDarkModeUseCase = get<SetAmoledDarkModeUseCase>(),
            observeThemeModeUseCase = get<ObserveThemeModeUseCase>(),
            setThemeModeUseCase = get<SetThemeModeUseCase>(),
            observeCustomNavShortcutsUseCase = get<ObserveCustomNavShortcutsUseCase>(),
            setCustomNavShortcutsUseCase = get<SetCustomNavShortcutsUseCase>(),
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
        )
    }
    viewModel {
        AuthSettingsViewModel(
            observeApiKeyUseCase = get<ObserveApiKeyUseCase>(),
            setApiKeyUseCase = get<SetApiKeyUseCase>(),
            validateApiKeyUseCase = get<ValidateApiKeyUseCase>(),
        )
    }
    viewModel {
        StorageSettingsViewModel(
            observeNetworkStatusUseCase = get<ObserveNetworkStatusUseCase>(),
            observeOfflineCacheEnabledUseCase = get<ObserveOfflineCacheEnabledUseCase>(),
            setOfflineCacheEnabledUseCase = get<SetOfflineCacheEnabledUseCase>(),
            observeCacheSizeLimitUseCase = get<ObserveCacheSizeLimitUseCase>(),
            setCacheSizeLimitUseCase = get<SetCacheSizeLimitUseCase>(),
            getCacheInfoUseCase = get<GetCacheInfoUseCase>(),
            evictCacheUseCase = get<EvictCacheUseCase>(),
            clearSearchHistoryUseCase = get<ClearSearchHistoryUseCase>(),
            clearBrowsingHistoryUseCase = get<ClearBrowsingHistoryUseCase>(),
            clearCacheUseCase = get<ClearCacheUseCase>(),
            getHiddenModelsUseCase = get<GetHiddenModelsUseCase>(),
            unhideModelUseCase = get<UnhideModelUseCase>(),
        )
    }
}
