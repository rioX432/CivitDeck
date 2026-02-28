package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.AddModelDirectoryUseCase
import com.riox432.civitdeck.domain.usecase.AddSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.CheckModelUpdatesUseCase
import com.riox432.civitdeck.domain.usecase.ClearBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearCacheUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.EvictCacheUseCase
import com.riox432.civitdeck.domain.usecase.GetCacheInfoUseCase
import com.riox432.civitdeck.domain.usecase.GetDiscoveryModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetRecommendationsUseCase
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.HideModelUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveFavoritesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveLocalModelFilesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelDirectoriesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNetworkStatusUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwBlurSettingsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOfflineCacheEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOwnedModelHashesUseCase
import com.riox432.civitdeck.domain.usecase.ObservePollingIntervalUseCase
import com.riox432.civitdeck.domain.usecase.ObservePowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSeenTutorialVersionUseCase
import com.riox432.civitdeck.domain.usecase.RemoveExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelDirectoryUseCase
import com.riox432.civitdeck.domain.usecase.ScanModelDirectoriesUseCase
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
import com.riox432.civitdeck.domain.usecase.SetSeenTutorialVersionUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import com.riox432.civitdeck.domain.usecase.UnhideModelUseCase
import com.riox432.civitdeck.domain.usecase.ValidateApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.VerifyModelHashUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GetModelsUseCase(get()) }
    factory { GetModelDetailUseCase(get()) }
    factory { GetDiscoveryModelsUseCase(get()) }
    factory { ToggleFavoriteUseCase(get()) }
    factory { ObserveFavoritesUseCase(get()) }
    factory { ObserveIsFavoriteUseCase(get()) }
    factory { ObserveNsfwFilterUseCase(get()) }
    factory { SetNsfwFilterUseCase(get()) }
    factory { ObserveNsfwBlurSettingsUseCase(get()) }
    factory { SetNsfwBlurSettingsUseCase(get()) }
    factory { ObserveSearchHistoryUseCase(get()) }
    factory { AddSearchHistoryUseCase(get()) }
    factory { ClearSearchHistoryUseCase(get()) }
    factory { TrackModelViewUseCase(get()) }
    factory { GetRecommendationsUseCase(get(), get(), get(), get()) }
    factory { GetViewedModelIdsUseCase(get()) }
    factory { GetExcludedTagsUseCase(get()) }
    factory { AddExcludedTagUseCase(get()) }
    factory { RemoveExcludedTagUseCase(get()) }
    factory { GetHiddenModelIdsUseCase(get()) }
    factory { HideModelUseCase(get()) }
    factory { UnhideModelUseCase(get()) }
    factory { ObserveDefaultSortOrderUseCase(get()) }
    factory { SetDefaultSortOrderUseCase(get()) }
    factory { ObserveDefaultTimePeriodUseCase(get()) }
    factory { SetDefaultTimePeriodUseCase(get()) }
    factory { ObserveGridColumnsUseCase(get()) }
    factory { SetGridColumnsUseCase(get()) }
    factory { GetHiddenModelsUseCase(get()) }
    factory { ClearBrowsingHistoryUseCase(get()) }
    factory { ClearCacheUseCase(get()) }
    factory { ObservePowerUserModeUseCase(get()) }
    factory { SetPowerUserModeUseCase(get()) }
    factory { ObserveApiKeyUseCase(get()) }
    factory { SetApiKeyUseCase(get()) }
    factory { ValidateApiKeyUseCase(get()) }
    // Local model file use cases
    factory { ObserveModelDirectoriesUseCase(get()) }
    factory { AddModelDirectoryUseCase(get()) }
    factory { RemoveModelDirectoryUseCase(get()) }
    factory { ObserveLocalModelFilesUseCase(get()) }
    factory { ScanModelDirectoriesUseCase(get()) }
    factory { VerifyModelHashUseCase(get()) }
    factory { ObserveOwnedModelHashesUseCase(get()) }
    // Theme use cases
    factory { ObserveAccentColorUseCase(get()) }
    factory { SetAccentColorUseCase(get()) }
    factory { ObserveAmoledDarkModeUseCase(get()) }
    factory { SetAmoledDarkModeUseCase(get()) }
    // Notification use cases
    factory { CheckModelUpdatesUseCase(get(), get(), get()) }
    factory { ObserveNotificationsEnabledUseCase(get()) }
    factory { SetNotificationsEnabledUseCase(get()) }
    factory { ObservePollingIntervalUseCase(get()) }
    factory { SetPollingIntervalUseCase(get()) }
    // Offline cache use cases
    factory { ObserveNetworkStatusUseCase(get()) }
    factory { GetCacheInfoUseCase(get()) }
    factory { EvictCacheUseCase(get()) }
    factory { ObserveOfflineCacheEnabledUseCase(get()) }
    factory { SetOfflineCacheEnabledUseCase(get()) }
    factory { ObserveCacheSizeLimitUseCase(get()) }
    factory { SetCacheSizeLimitUseCase(get()) }
    // Tutorial use cases
    factory { ObserveSeenTutorialVersionUseCase(get()) }
    factory { SetSeenTutorialVersionUseCase(get()) }
}
