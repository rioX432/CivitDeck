package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.data.image.SaveGeneratedImageUseCase
import com.riox432.civitdeck.domain.usecase.AddImageToDatasetUseCase
import com.riox432.civitdeck.domain.usecase.AddModelDirectoryUseCase
import com.riox432.civitdeck.domain.usecase.AddPersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.BatchEditTagsUseCase
import com.riox432.civitdeck.domain.usecase.CheckModelUpdatesUseCase
import com.riox432.civitdeck.domain.usecase.ClearBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearCacheUseCase
import com.riox432.civitdeck.domain.usecase.CreateDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.DetectDuplicatesUseCase
import com.riox432.civitdeck.domain.usecase.EditCaptionUseCase
import com.riox432.civitdeck.domain.usecase.EvictCacheUseCase
import com.riox432.civitdeck.domain.usecase.ExportDatasetUseCase
import com.riox432.civitdeck.domain.usecase.FilterByResolutionUseCase
import com.riox432.civitdeck.domain.usecase.FollowCreatorUseCase
import com.riox432.civitdeck.domain.usecase.GetAllPersonalTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetBrowsingStatsUseCase
import com.riox432.civitdeck.domain.usecase.GetCacheInfoUseCase
import com.riox432.civitdeck.domain.usecase.GetCreatorFeedUseCase
import com.riox432.civitdeck.domain.usecase.GetFollowedCreatorsUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.GetModelLicenseUseCase
import com.riox432.civitdeck.domain.usecase.GetNonTrainableImagesUseCase
import com.riox432.civitdeck.domain.usecase.GetTagSuggestionsUseCase
import com.riox432.civitdeck.domain.usecase.GetUnreadFeedCountUseCase
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.IsFollowingCreatorUseCase
import com.riox432.civitdeck.domain.usecase.MarkFeedReadUseCase
import com.riox432.civitdeck.domain.usecase.MarkImageExcludedUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCivitaiLinkKeyUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCustomNavShortcutsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetImagesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveFavoritesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveLocalModelFilesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelDirectoriesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNetworkStatusUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwBlurSettingsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOfflineCacheEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOwnedModelHashesUseCase
import com.riox432.civitdeck.domain.usecase.ObservePersonalTagsUseCase
import com.riox432.civitdeck.domain.usecase.ObservePollingIntervalUseCase
import com.riox432.civitdeck.domain.usecase.ObservePowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSeenTutorialVersionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveThemeModeUseCase
import com.riox432.civitdeck.domain.usecase.RemoveImageFromDatasetUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelDirectoryUseCase
import com.riox432.civitdeck.domain.usecase.RemovePersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.RenameDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.SaveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.ScanModelDirectoriesUseCase
import com.riox432.civitdeck.domain.usecase.SearchModelsByTagUseCase
import com.riox432.civitdeck.domain.usecase.SetAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.SetAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.SetApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.SetCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.SetCivitaiLinkKeyUseCase
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
import com.riox432.civitdeck.domain.usecase.SetSeenTutorialVersionUseCase
import com.riox432.civitdeck.domain.usecase.SetThemeModeUseCase
import com.riox432.civitdeck.domain.usecase.StoreImageDimensionsUseCase
import com.riox432.civitdeck.domain.usecase.StorePHashUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import com.riox432.civitdeck.domain.usecase.UnfollowCreatorUseCase
import com.riox432.civitdeck.domain.usecase.UpdateTrainableUseCase
import com.riox432.civitdeck.domain.usecase.ValidateApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.VerifyModelHashUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.AddModelToCollectionUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.BulkMoveModelsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.BulkRemoveModelsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.CreateCollectionUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.DeleteCollectionUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.ObserveCollectionModelsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.ObserveCollectionsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.ObserveModelCollectionsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.RemoveModelFromCollectionUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.RenameCollectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ActivateComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ActivateSDWebUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ApplyWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.CancelComfyUIJobUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.CancelLinkActivityUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ConnectCivitaiLinkUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DeleteComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DeleteSDWebUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DeleteWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DisconnectCivitaiLinkUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ExportWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUICheckpointsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIControlNetsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIHistoryItemUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIHistoryUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUILorasUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchSDWebUIModelsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchSDWebUISamplersUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchSDWebUIVaesUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FindMatchingLocalModelUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.GenerateSDWebUIImageUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.GetWorkflowTemplatesUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ImportWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ImportWorkflowUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.InterruptSDWebUIGenerationUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveActiveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveActiveSDWebUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveCivitaiLinkActivitiesUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveCivitaiLinkStatusUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveComfyUIConnectionsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveComfyUIQueueUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveGenerationProgressUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveSDWebUIConnectionsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PollComfyUIResultUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PopulateGenerationFromModelUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SaveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SaveSDWebUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SaveWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SendResourceToPCUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SubmitComfyUIGenerationUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.TestComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.TestSDWebUIConnectionUseCase
import com.riox432.civitdeck.feature.creator.domain.usecase.GetCreatorModelsUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ActivateExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.DeleteExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetExternalServerCapabilitiesUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetExternalServerImagesUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ObserveActiveExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ObserveExternalServerConfigsUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.SaveExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.TestExternalServerConnectionUseCase
import com.riox432.civitdeck.feature.gallery.domain.usecase.EnrichModelImagesUseCase
import com.riox432.civitdeck.feature.gallery.domain.usecase.GetImagesUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.AutoSavePromptUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.DeleteSavedPromptUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ObserveSavedPromptsUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ObserveTemplatesUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.SavePromptUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.SearchSavedPromptsUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ToggleTemplateUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.AddSearchHistoryUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.DeleteSavedSearchFilterUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.DeleteSearchHistoryItemUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetDiscoveryModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetHiddenModelIdsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetRecommendationsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.HideModelUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.ObserveSavedSearchFiltersUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.ObserveSearchHistoryUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.RemoveExcludedTagUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.SaveSearchFilterUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.UnhideModelUseCase
import com.riox432.civitdeck.feature.settings.presentation.SettingsViewModel
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
    fun getDeleteSearchHistoryItemUseCase(): DeleteSearchHistoryItemUseCase = getKoin().get()
    fun getTrackModelViewUseCase(): TrackModelViewUseCase = getKoin().get()
    fun getRecommendationsUseCase(): GetRecommendationsUseCase = getKoin().get()
    fun getViewedModelIdsUseCase(): GetViewedModelIdsUseCase = getKoin().get()
    fun getExcludedTagsUseCase(): GetExcludedTagsUseCase = getKoin().get()
    fun getAddExcludedTagUseCase(): AddExcludedTagUseCase = getKoin().get()
    fun getRemoveExcludedTagUseCase(): RemoveExcludedTagUseCase = getKoin().get()
    fun getHiddenModelIdsUseCase(): GetHiddenModelIdsUseCase = getKoin().get()
    fun getHideModelUseCase(): HideModelUseCase = getKoin().get()
    fun getUnhideModelUseCase(): UnhideModelUseCase = getKoin().get()
    fun getObserveSavedSearchFiltersUseCase(): ObserveSavedSearchFiltersUseCase = getKoin().get()
    fun getSaveSearchFilterUseCase(): SaveSearchFilterUseCase = getKoin().get()
    fun getDeleteSavedSearchFilterUseCase(): DeleteSavedSearchFilterUseCase = getKoin().get()
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
    fun getObserveCustomNavShortcutsUseCase(): ObserveCustomNavShortcutsUseCase = getKoin().get()
    fun getSetCustomNavShortcutsUseCase(): SetCustomNavShortcutsUseCase = getKoin().get()
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
    fun getObserveThemeModeUseCase(): ObserveThemeModeUseCase = getKoin().get()
    fun getSetThemeModeUseCase(): SetThemeModeUseCase = getKoin().get()

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

    // ComfyUI history use cases
    fun getFetchComfyUIHistoryUseCase(): FetchComfyUIHistoryUseCase = getKoin().get()
    fun getFetchComfyUIHistoryItemUseCase(): FetchComfyUIHistoryItemUseCase = getKoin().get()

    // ComfyUI use cases
    fun getObserveComfyUIConnectionsUseCase(): ObserveComfyUIConnectionsUseCase = getKoin().get()
    fun getObserveActiveComfyUIConnectionUseCase(): ObserveActiveComfyUIConnectionUseCase = getKoin().get()
    fun getSaveComfyUIConnectionUseCase(): SaveComfyUIConnectionUseCase = getKoin().get()
    fun getDeleteComfyUIConnectionUseCase(): DeleteComfyUIConnectionUseCase = getKoin().get()
    fun getActivateComfyUIConnectionUseCase(): ActivateComfyUIConnectionUseCase = getKoin().get()
    fun getTestComfyUIConnectionUseCase(): TestComfyUIConnectionUseCase = getKoin().get()
    fun getFetchComfyUICheckpointsUseCase(): FetchComfyUICheckpointsUseCase = getKoin().get()
    fun getFetchComfyUILorasUseCase(): FetchComfyUILorasUseCase = getKoin().get()
    fun getFetchComfyUIControlNetsUseCase(): FetchComfyUIControlNetsUseCase = getKoin().get()
    fun getImportWorkflowUseCase(): ImportWorkflowUseCase = getKoin().get()
    fun getSubmitComfyUIGenerationUseCase(): SubmitComfyUIGenerationUseCase = getKoin().get()
    fun getPollComfyUIResultUseCase(): PollComfyUIResultUseCase = getKoin().get()
    fun getObserveGenerationProgressUseCase(): ObserveGenerationProgressUseCase = getKoin().get()
    fun getObserveComfyUIQueueUseCase(): ObserveComfyUIQueueUseCase = getKoin().get()
    fun getCancelComfyUIJobUseCase(): CancelComfyUIJobUseCase = getKoin().get()
    fun getFindMatchingLocalModelUseCase(): FindMatchingLocalModelUseCase = getKoin().get()
    fun getPopulateGenerationFromModelUseCase(): PopulateGenerationFromModelUseCase = getKoin().get()
    fun getSaveGeneratedImageUseCase(): SaveGeneratedImageUseCase = getKoin().get()

    // Workflow template use cases
    fun getGetWorkflowTemplatesUseCase(): GetWorkflowTemplatesUseCase = getKoin().get()
    fun getSaveWorkflowTemplateUseCase(): SaveWorkflowTemplateUseCase = getKoin().get()
    fun getDeleteWorkflowTemplateUseCase(): DeleteWorkflowTemplateUseCase = getKoin().get()
    fun getExportWorkflowTemplateUseCase(): ExportWorkflowTemplateUseCase = getKoin().get()
    fun getImportWorkflowTemplateUseCase(): ImportWorkflowTemplateUseCase = getKoin().get()
    fun getApplyWorkflowTemplateUseCase(): ApplyWorkflowTemplateUseCase = getKoin().get()

    // ViewModels
    fun createSettingsViewModel(): SettingsViewModel = getKoin().get()

    // Civitai Link use cases
    fun getObserveCivitaiLinkKeyUseCase(): ObserveCivitaiLinkKeyUseCase = getKoin().get()
    fun getSetCivitaiLinkKeyUseCase(): SetCivitaiLinkKeyUseCase = getKoin().get()
    fun getObserveCivitaiLinkStatusUseCase(): ObserveCivitaiLinkStatusUseCase = getKoin().get()
    fun getObserveCivitaiLinkActivitiesUseCase(): ObserveCivitaiLinkActivitiesUseCase = getKoin().get()
    fun getConnectCivitaiLinkUseCase(): ConnectCivitaiLinkUseCase = getKoin().get()
    fun getDisconnectCivitaiLinkUseCase(): DisconnectCivitaiLinkUseCase = getKoin().get()
    fun getSendResourceToPCUseCase(): SendResourceToPCUseCase = getKoin().get()
    fun getCancelLinkActivityUseCase(): CancelLinkActivityUseCase = getKoin().get()

    // SD WebUI use cases
    fun getObserveSDWebUIConnectionsUseCase(): ObserveSDWebUIConnectionsUseCase = getKoin().get()
    fun getObserveActiveSDWebUIConnectionUseCase(): ObserveActiveSDWebUIConnectionUseCase = getKoin().get()
    fun getSaveSDWebUIConnectionUseCase(): SaveSDWebUIConnectionUseCase = getKoin().get()
    fun getDeleteSDWebUIConnectionUseCase(): DeleteSDWebUIConnectionUseCase = getKoin().get()
    fun getActivateSDWebUIConnectionUseCase(): ActivateSDWebUIConnectionUseCase = getKoin().get()
    fun getTestSDWebUIConnectionUseCase(): TestSDWebUIConnectionUseCase = getKoin().get()
    fun getFetchSDWebUIModelsUseCase(): FetchSDWebUIModelsUseCase = getKoin().get()
    fun getFetchSDWebUISamplersUseCase(): FetchSDWebUISamplersUseCase = getKoin().get()
    fun getFetchSDWebUIVaesUseCase(): FetchSDWebUIVaesUseCase = getKoin().get()
    fun getGenerateSDWebUIImageUseCase(): GenerateSDWebUIImageUseCase = getKoin().get()
    fun getInterruptSDWebUIGenerationUseCase(): InterruptSDWebUIGenerationUseCase = getKoin().get()

    // Dataset use cases
    fun getObserveDatasetCollectionsUseCase(): ObserveDatasetCollectionsUseCase = getKoin().get()
    fun getCreateDatasetCollectionUseCase(): CreateDatasetCollectionUseCase = getKoin().get()
    fun getRenameDatasetCollectionUseCase(): RenameDatasetCollectionUseCase = getKoin().get()
    fun getDeleteDatasetCollectionUseCase(): DeleteDatasetCollectionUseCase = getKoin().get()
    fun getObserveDatasetImagesUseCase(): ObserveDatasetImagesUseCase = getKoin().get()
    fun getAddImageToDatasetUseCase(): AddImageToDatasetUseCase = getKoin().get()
    fun getRemoveImageFromDatasetUseCase(): RemoveImageFromDatasetUseCase = getKoin().get()
    fun getBatchEditTagsUseCase(): BatchEditTagsUseCase = getKoin().get()
    fun getEditCaptionUseCase(): EditCaptionUseCase = getKoin().get()
    fun getGetTagSuggestionsUseCase(): GetTagSuggestionsUseCase = getKoin().get()
    fun getUpdateTrainableUseCase(): UpdateTrainableUseCase = getKoin().get()
    fun getGetNonTrainableImagesUseCase(): GetNonTrainableImagesUseCase = getKoin().get()
    fun getGetModelLicenseUseCase(): GetModelLicenseUseCase = getKoin().get()
    fun getDetectDuplicatesUseCase(): DetectDuplicatesUseCase = getKoin().get()
    fun getFilterByResolutionUseCase(): FilterByResolutionUseCase = getKoin().get()
    fun getMarkImageExcludedUseCase(): MarkImageExcludedUseCase = getKoin().get()
    fun getStorePHashUseCase(): StorePHashUseCase = getKoin().get()
    fun getStoreImageDimensionsUseCase(): StoreImageDimensionsUseCase = getKoin().get()
    fun getExportDatasetUseCase(): ExportDatasetUseCase = getKoin().get()

    // External Server use cases
    fun getObserveExternalServerConfigsUseCase(): ObserveExternalServerConfigsUseCase = getKoin().get()
    fun getObserveActiveExternalServerConfigUseCase(): ObserveActiveExternalServerConfigUseCase = getKoin().get()
    fun getSaveExternalServerConfigUseCase(): SaveExternalServerConfigUseCase = getKoin().get()
    fun getDeleteExternalServerConfigUseCase(): DeleteExternalServerConfigUseCase = getKoin().get()
    fun getActivateExternalServerConfigUseCase(): ActivateExternalServerConfigUseCase = getKoin().get()
    fun getTestExternalServerConnectionUseCase(): TestExternalServerConnectionUseCase = getKoin().get()
    fun getGetExternalServerCapabilitiesUseCase(): GetExternalServerCapabilitiesUseCase = getKoin().get()
    fun getGetExternalServerImagesUseCase(): GetExternalServerImagesUseCase = getKoin().get()

    // Model notes & personal tags use cases
    fun getObserveModelNoteUseCase(): ObserveModelNoteUseCase = getKoin().get()
    fun getSaveModelNoteUseCase(): SaveModelNoteUseCase = getKoin().get()
    fun getDeleteModelNoteUseCase(): DeleteModelNoteUseCase = getKoin().get()
    fun getObservePersonalTagsUseCase(): ObservePersonalTagsUseCase = getKoin().get()
    fun getAddPersonalTagUseCase(): AddPersonalTagUseCase = getKoin().get()
    fun getRemovePersonalTagUseCase(): RemovePersonalTagUseCase = getKoin().get()
    fun getGetAllPersonalTagsUseCase(): GetAllPersonalTagsUseCase = getKoin().get()
    fun getSearchModelsByTagUseCase(): SearchModelsByTagUseCase = getKoin().get()

    // Analytics use cases
    fun getBrowsingStatsUseCase(): GetBrowsingStatsUseCase = getKoin().get()

    // Creator follow use cases
    fun getFollowCreatorUseCase(): FollowCreatorUseCase = getKoin().get()
    fun getUnfollowCreatorUseCase(): UnfollowCreatorUseCase = getKoin().get()
    fun getIsFollowingCreatorUseCase(): IsFollowingCreatorUseCase = getKoin().get()
    fun getCreatorFeedUseCase(): GetCreatorFeedUseCase = getKoin().get()
    fun getUnreadFeedCountUseCase(): GetUnreadFeedCountUseCase = getKoin().get()
    fun getMarkFeedReadUseCase(): MarkFeedReadUseCase = getKoin().get()
    fun getFollowedCreatorsUseCase(): GetFollowedCreatorsUseCase = getKoin().get()
}
