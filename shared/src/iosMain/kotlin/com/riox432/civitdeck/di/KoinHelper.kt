package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.domain.usecase.ActivateComfyUIConnectionUseCase
import com.riox432.civitdeck.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.AddModelDirectoryUseCase
import com.riox432.civitdeck.domain.usecase.AddModelToCollectionUseCase
import com.riox432.civitdeck.domain.usecase.AddSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.AutoSavePromptUseCase
import com.riox432.civitdeck.domain.usecase.BulkMoveModelsUseCase
import com.riox432.civitdeck.domain.usecase.BulkRemoveModelsUseCase
import com.riox432.civitdeck.domain.usecase.CheckModelUpdatesUseCase
import com.riox432.civitdeck.domain.usecase.ClearBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearCacheUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.CreateCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteComfyUIConnectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteSavedPromptUseCase
import com.riox432.civitdeck.domain.usecase.EnrichModelImagesUseCase
import com.riox432.civitdeck.domain.usecase.EvictCacheUseCase
import com.riox432.civitdeck.domain.usecase.FetchComfyUICheckpointsUseCase
import com.riox432.civitdeck.domain.usecase.GetCacheInfoUseCase
import com.riox432.civitdeck.domain.usecase.GetCreatorModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetDiscoveryModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetImagesUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetRecommendationsUseCase
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.HideModelUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.ObserveActiveComfyUIConnectionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCollectionModelsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveComfyUIConnectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveFavoritesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveLocalModelFilesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelDirectoriesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNetworkStatusUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwBlurSettingsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOfflineCacheEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOwnedModelHashesUseCase
import com.riox432.civitdeck.domain.usecase.ObservePollingIntervalUseCase
import com.riox432.civitdeck.domain.usecase.ObservePowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSavedPromptsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSeenTutorialVersionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveTemplatesUseCase
import com.riox432.civitdeck.domain.usecase.PollComfyUIResultUseCase
import com.riox432.civitdeck.domain.usecase.RemoveExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelDirectoryUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelFromCollectionUseCase
import com.riox432.civitdeck.domain.usecase.RenameCollectionUseCase
import com.riox432.civitdeck.domain.usecase.SaveComfyUIConnectionUseCase
import com.riox432.civitdeck.domain.usecase.SavePromptUseCase
import com.riox432.civitdeck.domain.usecase.ScanModelDirectoriesUseCase
import com.riox432.civitdeck.domain.usecase.SearchSavedPromptsUseCase
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
import com.riox432.civitdeck.domain.usecase.SubmitComfyUIGenerationUseCase
import com.riox432.civitdeck.domain.usecase.TestComfyUIConnectionUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ToggleTemplateUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import com.riox432.civitdeck.domain.usecase.UnhideModelUseCase
import com.riox432.civitdeck.domain.usecase.ValidateApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.VerifyModelHashUseCase
import com.riox432.civitdeck.ui.settings.SettingsViewModel
import org.koin.mp.KoinPlatform.getKoin

@Suppress("TooManyFunctions")
object KoinHelper {
    fun getModelsUseCase(): GetModelsUseCase = getKoin().get()
    fun getCreatorModelsUseCase(): GetCreatorModelsUseCase = getKoin().get()
    fun getModelDetailUseCase(): GetModelDetailUseCase = getKoin().get()
    fun getImagesUseCase(): GetImagesUseCase = getKoin().get()
    fun getToggleFavoriteUseCase(): ToggleFavoriteUseCase = getKoin().get()
    fun getObserveFavoritesUseCase(): ObserveFavoritesUseCase = getKoin().get()
    fun getObserveIsFavoriteUseCase(): ObserveIsFavoriteUseCase = getKoin().get()
    fun getObserveNsfwFilterUseCase(): ObserveNsfwFilterUseCase = getKoin().get()
    fun getSetNsfwFilterUseCase(): SetNsfwFilterUseCase = getKoin().get()
    fun getObserveNsfwBlurSettingsUseCase(): ObserveNsfwBlurSettingsUseCase = getKoin().get()
    fun getSetNsfwBlurSettingsUseCase(): SetNsfwBlurSettingsUseCase = getKoin().get()
    fun getSavePromptUseCase(): SavePromptUseCase = getKoin().get()
    fun getObserveSavedPromptsUseCase(): ObserveSavedPromptsUseCase = getKoin().get()
    fun getDeleteSavedPromptUseCase(): DeleteSavedPromptUseCase = getKoin().get()
    fun getObserveSearchHistoryUseCase(): ObserveSearchHistoryUseCase = getKoin().get()
    fun getAddSearchHistoryUseCase(): AddSearchHistoryUseCase = getKoin().get()
    fun getClearSearchHistoryUseCase(): ClearSearchHistoryUseCase = getKoin().get()
    fun getTrackModelViewUseCase(): TrackModelViewUseCase = getKoin().get()
    fun getRecommendationsUseCase(): GetRecommendationsUseCase = getKoin().get()
    fun getViewedModelIdsUseCase(): GetViewedModelIdsUseCase = getKoin().get()
    fun getExcludedTagsUseCase(): GetExcludedTagsUseCase = getKoin().get()
    fun getAddExcludedTagUseCase(): AddExcludedTagUseCase = getKoin().get()
    fun getRemoveExcludedTagUseCase(): RemoveExcludedTagUseCase = getKoin().get()
    fun getHiddenModelIdsUseCase(): GetHiddenModelIdsUseCase = getKoin().get()
    fun getHideModelUseCase(): HideModelUseCase = getKoin().get()
    fun getUnhideModelUseCase(): UnhideModelUseCase = getKoin().get()
    fun getObserveDefaultSortOrderUseCase(): ObserveDefaultSortOrderUseCase = getKoin().get()
    fun getSetDefaultSortOrderUseCase(): SetDefaultSortOrderUseCase = getKoin().get()
    fun getObserveDefaultTimePeriodUseCase(): ObserveDefaultTimePeriodUseCase = getKoin().get()
    fun getSetDefaultTimePeriodUseCase(): SetDefaultTimePeriodUseCase = getKoin().get()
    fun getObserveGridColumnsUseCase(): ObserveGridColumnsUseCase = getKoin().get()
    fun getSetGridColumnsUseCase(): SetGridColumnsUseCase = getKoin().get()
    fun getHiddenModelsUseCase(): GetHiddenModelsUseCase = getKoin().get()
    fun getClearBrowsingHistoryUseCase(): ClearBrowsingHistoryUseCase = getKoin().get()
    fun getClearCacheUseCase(): ClearCacheUseCase = getKoin().get()
    fun getObservePowerUserModeUseCase(): ObservePowerUserModeUseCase = getKoin().get()
    fun getSetPowerUserModeUseCase(): SetPowerUserModeUseCase = getKoin().get()
    fun getEnrichModelImagesUseCase(): EnrichModelImagesUseCase = getKoin().get()
    fun getDiscoveryModelsUseCase(): GetDiscoveryModelsUseCase = getKoin().get()
    fun getObserveApiKeyUseCase(): ObserveApiKeyUseCase = getKoin().get()
    fun getSetApiKeyUseCase(): SetApiKeyUseCase = getKoin().get()
    fun getValidateApiKeyUseCase(): ValidateApiKeyUseCase = getKoin().get()
    fun getApiKeyProvider(): ApiKeyProvider = getKoin().get()

    // Collection use cases
    fun getObserveCollectionsUseCase(): ObserveCollectionsUseCase = getKoin().get()
    fun getCreateCollectionUseCase(): CreateCollectionUseCase = getKoin().get()
    fun getRenameCollectionUseCase(): RenameCollectionUseCase = getKoin().get()
    fun getDeleteCollectionUseCase(): DeleteCollectionUseCase = getKoin().get()
    fun getObserveCollectionModelsUseCase(): ObserveCollectionModelsUseCase = getKoin().get()
    fun getAddModelToCollectionUseCase(): AddModelToCollectionUseCase = getKoin().get()
    fun getRemoveModelFromCollectionUseCase(): RemoveModelFromCollectionUseCase = getKoin().get()
    fun getObserveModelCollectionsUseCase(): ObserveModelCollectionsUseCase = getKoin().get()
    fun getBulkMoveModelsUseCase(): BulkMoveModelsUseCase = getKoin().get()
    fun getBulkRemoveModelsUseCase(): BulkRemoveModelsUseCase = getKoin().get()

    // Local model file use cases
    fun getObserveModelDirectoriesUseCase(): ObserveModelDirectoriesUseCase = getKoin().get()
    fun getAddModelDirectoryUseCase(): AddModelDirectoryUseCase = getKoin().get()
    fun getRemoveModelDirectoryUseCase(): RemoveModelDirectoryUseCase = getKoin().get()
    fun getObserveLocalModelFilesUseCase(): ObserveLocalModelFilesUseCase = getKoin().get()
    fun getScanModelDirectoriesUseCase(): ScanModelDirectoriesUseCase = getKoin().get()
    fun getVerifyModelHashUseCase(): VerifyModelHashUseCase = getKoin().get()
    fun getObserveOwnedModelHashesUseCase(): ObserveOwnedModelHashesUseCase = getKoin().get()

    // Prompt template use cases
    fun getAutoSavePromptUseCase(): AutoSavePromptUseCase = getKoin().get()
    fun getToggleTemplateUseCase(): ToggleTemplateUseCase = getKoin().get()
    fun getSearchSavedPromptsUseCase(): SearchSavedPromptsUseCase = getKoin().get()
    fun getObserveTemplatesUseCase(): ObserveTemplatesUseCase = getKoin().get()

    // Theme use cases
    fun getObserveAccentColorUseCase(): ObserveAccentColorUseCase = getKoin().get()
    fun getSetAccentColorUseCase(): SetAccentColorUseCase = getKoin().get()
    fun getObserveAmoledDarkModeUseCase(): ObserveAmoledDarkModeUseCase = getKoin().get()
    fun getSetAmoledDarkModeUseCase(): SetAmoledDarkModeUseCase = getKoin().get()

    // Notification use cases
    fun getCheckModelUpdatesUseCase(): CheckModelUpdatesUseCase = getKoin().get()
    fun getObserveNotificationsEnabledUseCase(): ObserveNotificationsEnabledUseCase = getKoin().get()
    fun getSetNotificationsEnabledUseCase(): SetNotificationsEnabledUseCase = getKoin().get()
    fun getObservePollingIntervalUseCase(): ObservePollingIntervalUseCase = getKoin().get()
    fun getSetPollingIntervalUseCase(): SetPollingIntervalUseCase = getKoin().get()

    // Offline cache use cases
    fun getObserveNetworkStatusUseCase(): ObserveNetworkStatusUseCase = getKoin().get()
    fun getCacheInfoUseCase(): GetCacheInfoUseCase = getKoin().get()
    fun getEvictCacheUseCase(): EvictCacheUseCase = getKoin().get()
    fun getObserveOfflineCacheEnabledUseCase(): ObserveOfflineCacheEnabledUseCase = getKoin().get()
    fun getSetOfflineCacheEnabledUseCase(): SetOfflineCacheEnabledUseCase = getKoin().get()
    fun getObserveCacheSizeLimitUseCase(): ObserveCacheSizeLimitUseCase = getKoin().get()
    fun getSetCacheSizeLimitUseCase(): SetCacheSizeLimitUseCase = getKoin().get()

    // Tutorial use cases
    fun getObserveSeenTutorialVersionUseCase(): ObserveSeenTutorialVersionUseCase = getKoin().get()
    fun getSetSeenTutorialVersionUseCase(): SetSeenTutorialVersionUseCase = getKoin().get()

    // ComfyUI use cases
    fun getObserveComfyUIConnectionsUseCase(): ObserveComfyUIConnectionsUseCase = getKoin().get()
    fun getObserveActiveComfyUIConnectionUseCase(): ObserveActiveComfyUIConnectionUseCase = getKoin().get()
    fun getSaveComfyUIConnectionUseCase(): SaveComfyUIConnectionUseCase = getKoin().get()
    fun getDeleteComfyUIConnectionUseCase(): DeleteComfyUIConnectionUseCase = getKoin().get()
    fun getActivateComfyUIConnectionUseCase(): ActivateComfyUIConnectionUseCase = getKoin().get()
    fun getTestComfyUIConnectionUseCase(): TestComfyUIConnectionUseCase = getKoin().get()
    fun getFetchComfyUICheckpointsUseCase(): FetchComfyUICheckpointsUseCase = getKoin().get()
    fun getSubmitComfyUIGenerationUseCase(): SubmitComfyUIGenerationUseCase = getKoin().get()
    fun getPollComfyUIResultUseCase(): PollComfyUIResultUseCase = getKoin().get()

    // ViewModels
    fun createSettingsViewModel(): SettingsViewModel = getKoin().get()
}
