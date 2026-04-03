package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.data.image.SaveGeneratedImageUseCase
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.domain.repository.ModelDownloadRepository
import com.riox432.civitdeck.domain.usecase.ActivatePluginUseCase
import com.riox432.civitdeck.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.AddImageToDatasetUseCase
import com.riox432.civitdeck.domain.usecase.AddModelDirectoryUseCase
import com.riox432.civitdeck.domain.usecase.AddModelToCollectionUseCase
import com.riox432.civitdeck.domain.usecase.AddPersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.AddShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.AutoSavePromptUseCase
import com.riox432.civitdeck.domain.usecase.BatchEditTagsUseCase
import com.riox432.civitdeck.domain.usecase.CancelDownloadUseCase
import com.riox432.civitdeck.domain.usecase.CheckAndStoreModelUpdatesUseCase
import com.riox432.civitdeck.domain.usecase.CheckModelUpdatesUseCase
import com.riox432.civitdeck.domain.usecase.CleanupBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearCacheUseCase
import com.riox432.civitdeck.domain.usecase.ClearCompletedDownloadsUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.CreateBackupUseCase
import com.riox432.civitdeck.domain.usecase.CreateCollectionUseCase
import com.riox432.civitdeck.domain.usecase.CreateDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeactivatePluginUseCase
import com.riox432.civitdeck.domain.usecase.DeleteBrowsingHistoryItemUseCase
import com.riox432.civitdeck.domain.usecase.DeleteDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteDownloadUseCase
import com.riox432.civitdeck.domain.usecase.DeleteModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.DetectDuplicatesUseCase
import com.riox432.civitdeck.domain.usecase.EditCaptionUseCase
import com.riox432.civitdeck.domain.usecase.EnqueueDownloadUseCase
import com.riox432.civitdeck.domain.usecase.EnrichModelImagesUseCase
import com.riox432.civitdeck.domain.usecase.EvictCacheUseCase
import com.riox432.civitdeck.domain.usecase.ExportDatasetUseCase
import com.riox432.civitdeck.domain.usecase.FilterByResolutionUseCase
import com.riox432.civitdeck.domain.usecase.FollowCreatorUseCase
import com.riox432.civitdeck.domain.usecase.GetAllPersonalTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetBrowsingStatsUseCase
import com.riox432.civitdeck.domain.usecase.GetCacheInfoUseCase
import com.riox432.civitdeck.domain.usecase.GetCreatorFeedUseCase
import com.riox432.civitdeck.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetFollowedCreatorsUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.GetModelLicenseUseCase
import com.riox432.civitdeck.domain.usecase.GetModelReviewsUseCase
import com.riox432.civitdeck.domain.usecase.GetModelUpdateNotificationsUseCase
import com.riox432.civitdeck.domain.usecase.GetNonTrainableImagesUseCase
import com.riox432.civitdeck.domain.usecase.GetPluginConfigUseCase
import com.riox432.civitdeck.domain.usecase.GetRatingTotalsUseCase
import com.riox432.civitdeck.domain.usecase.GetSimilarModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetTagSuggestionsUseCase
import com.riox432.civitdeck.domain.usecase.GetUnreadFeedCountUseCase
import com.riox432.civitdeck.domain.usecase.GetUnreadNotificationCountUseCase
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.InstallPluginUseCase
import com.riox432.civitdeck.domain.usecase.IsFollowingCreatorUseCase
import com.riox432.civitdeck.domain.usecase.MarkAllNotificationsReadUseCase
import com.riox432.civitdeck.domain.usecase.MarkFeedReadUseCase
import com.riox432.civitdeck.domain.usecase.MarkImageExcludedUseCase
import com.riox432.civitdeck.domain.usecase.MarkNotificationReadUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCivitaiLinkKeyUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCustomNavShortcutsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetImagesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDownloadsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveFavoritesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveInstalledPluginsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveLocalModelFilesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelDirectoriesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelDownloadsUseCase
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
import com.riox432.civitdeck.domain.usecase.ObserveQualityThresholdUseCase
import com.riox432.civitdeck.domain.usecase.ObserveRecentlyViewedUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSeenTutorialVersionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveShareHashtagsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveThemeModeUseCase
import com.riox432.civitdeck.domain.usecase.ParseBackupUseCase
import com.riox432.civitdeck.domain.usecase.PauseDownloadUseCase
import com.riox432.civitdeck.domain.usecase.RemoveExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.RemoveImageFromDatasetUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelDirectoryUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelFromCollectionUseCase
import com.riox432.civitdeck.domain.usecase.RemovePersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.RemoveShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.RenameDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.RestoreBackupUseCase
import com.riox432.civitdeck.domain.usecase.ResumeDownloadUseCase
import com.riox432.civitdeck.domain.usecase.SaveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.SavePromptUseCase
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
import com.riox432.civitdeck.domain.usecase.SetQualityThresholdUseCase
import com.riox432.civitdeck.domain.usecase.SetSeenTutorialVersionUseCase
import com.riox432.civitdeck.domain.usecase.SetThemeModeUseCase
import com.riox432.civitdeck.domain.usecase.StoreImageDimensionsUseCase
import com.riox432.civitdeck.domain.usecase.StorePHashUseCase
import com.riox432.civitdeck.domain.usecase.SubmitReviewUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ToggleShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import com.riox432.civitdeck.domain.usecase.VerifyDownloadHashUseCase
import com.riox432.civitdeck.domain.usecase.UnfollowCreatorUseCase
import com.riox432.civitdeck.domain.usecase.UnhideModelUseCase
import com.riox432.civitdeck.domain.usecase.UninstallPluginUseCase
import com.riox432.civitdeck.domain.usecase.UpdatePluginConfigUseCase
import com.riox432.civitdeck.domain.usecase.UpdateTrainableUseCase
import com.riox432.civitdeck.domain.usecase.ValidateApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.VerifyModelHashUseCase
import com.riox432.civitdeck.feature.collections.presentation.CollectionDetailViewModel
import com.riox432.civitdeck.feature.collections.presentation.CollectionsViewModel
import com.riox432.civitdeck.feature.collections.domain.usecase.BulkMoveModelsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.BulkRemoveModelsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.DeleteCollectionUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.ObserveCollectionModelsUseCase
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
import com.riox432.civitdeck.feature.comfyui.domain.usecase.GetComfyHubWorkflowDetailUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.GetWorkflowTemplatesUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ImportComfyHubWorkflowUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ImportWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ImportWorkflowUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.InterruptComfyUIGenerationUseCase
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
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SearchComfyHubWorkflowsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SendResourceToPCUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SubmitComfyUIGenerationUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.TestComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.TestSDWebUIConnectionUseCase
import com.riox432.civitdeck.feature.creator.domain.usecase.GetCreatorModelsUseCase
import com.riox432.civitdeck.feature.creator.presentation.CreatorProfileViewModel
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ActivateExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.DeleteExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.DeleteServerImagesUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ExecuteGenerationUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetDependentChoicesUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetExternalServerCapabilitiesUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetExternalServerImagesUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetGenerationOptionsUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetGenerationStatusUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ObserveActiveExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ObserveExternalServerConfigsUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.SaveExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.TestExternalServerConnectionUseCase
import com.riox432.civitdeck.feature.gallery.domain.usecase.GetImagesUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.DeleteSavedPromptUseCase
import com.riox432.civitdeck.feature.prompts.presentation.SavedPromptsViewModel
import com.riox432.civitdeck.feature.prompts.domain.usecase.ObserveSavedPromptsUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ObserveTemplatesUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.SearchSavedPromptsUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ToggleTemplateUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.AddSearchHistoryUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.DeleteSavedSearchFilterUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.DeleteSearchHistoryItemUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetDiscoveryModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetHiddenModelIdsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetRecommendationsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.TrackRecommendationClickUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.HideModelUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.MultiSourceSearchUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.ObserveSavedSearchFiltersUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.ObserveSearchHistoryUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.SaveSearchFilterUseCase
import com.riox432.civitdeck.feature.gallery.presentation.ImageGalleryViewModel
import com.riox432.civitdeck.feature.search.presentation.BrowsingHistoryViewModel
import com.riox432.civitdeck.feature.search.presentation.SwipeDiscoveryViewModel
import com.riox432.civitdeck.presentation.settings.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.presentation.settings.AuthSettingsViewModel
import com.riox432.civitdeck.presentation.settings.ContentFilterSettingsViewModel
import com.riox432.civitdeck.presentation.settings.DisplaySettingsViewModel
import com.riox432.civitdeck.presentation.settings.StorageSettingsViewModel
import com.riox432.civitdeck.presentation.analytics.AnalyticsViewModel
import com.riox432.civitdeck.presentation.backup.BackupViewModel
import com.riox432.civitdeck.presentation.dataset.BatchTagEditorViewModel
import com.riox432.civitdeck.presentation.dataset.DatasetDetailViewModel
import com.riox432.civitdeck.presentation.dataset.DatasetListViewModel
import com.riox432.civitdeck.presentation.similar.SimilarModelsViewModel
import com.riox432.civitdeck.presentation.feed.FeedViewModel
import com.riox432.civitdeck.presentation.notificationcenter.NotificationCenterViewModel
import com.riox432.civitdeck.presentation.plugin.PluginManagementViewModel
import com.riox432.civitdeck.presentation.share.ShareViewModel
import com.riox432.civitdeck.presentation.tutorial.GestureTutorialViewModel
import com.riox432.civitdeck.presentation.update.UpdateViewModel
import com.riox432.civitdeck.usecase.ActivateThemePluginUseCase
import com.riox432.civitdeck.usecase.ExportWithPluginUseCase
import com.riox432.civitdeck.usecase.GetActiveThemeUseCase
import com.riox432.civitdeck.usecase.GetAvailableExportFormatsUseCase
import com.riox432.civitdeck.usecase.ImportThemeUseCase
import com.riox432.civitdeck.usecase.ObserveThemePluginsUseCase
import org.koin.mp.KoinPlatform.getKoin

@Suppress("TooManyFunctions")
object KoinHelper {

    // region Search & Discovery
    fun getModelsUseCase(): GetModelsUseCase = getKoin().get()
    fun getMultiSourceSearchUseCase(): MultiSourceSearchUseCase = getKoin().get()
    fun getDiscoveryModelsUseCase(): GetDiscoveryModelsUseCase = getKoin().get()
    fun getRecommendationsUseCase(): GetRecommendationsUseCase = getKoin().get()
    fun getTrackRecommendationClickUseCase(): TrackRecommendationClickUseCase = getKoin().get()
    fun getEnrichModelImagesUseCase(): EnrichModelImagesUseCase = getKoin().get()
    fun getSimilarModelsUseCase(): GetSimilarModelsUseCase = getKoin().get()
    // endregion

    // region Search History & Filters
    fun getObserveSearchHistoryUseCase(): ObserveSearchHistoryUseCase = getKoin().get()
    fun getAddSearchHistoryUseCase(): AddSearchHistoryUseCase = getKoin().get()
    fun getClearSearchHistoryUseCase(): ClearSearchHistoryUseCase = getKoin().get()
    fun getDeleteSearchHistoryItemUseCase(): DeleteSearchHistoryItemUseCase = getKoin().get()
    fun getObserveSavedSearchFiltersUseCase(): ObserveSavedSearchFiltersUseCase = getKoin().get()
    fun getSaveSearchFilterUseCase(): SaveSearchFilterUseCase = getKoin().get()
    fun getDeleteSavedSearchFilterUseCase(): DeleteSavedSearchFilterUseCase = getKoin().get()
    fun getExcludedTagsUseCase(): GetExcludedTagsUseCase = getKoin().get()
    fun getAddExcludedTagUseCase(): AddExcludedTagUseCase = getKoin().get()
    fun getRemoveExcludedTagUseCase(): RemoveExcludedTagUseCase = getKoin().get()
    // endregion

    // region Model Detail & Creator
    fun getModelDetailUseCase(): GetModelDetailUseCase = getKoin().get()
    fun getCreatorModelsUseCase(): GetCreatorModelsUseCase = getKoin().get()
    fun getImagesUseCase(): GetImagesUseCase = getKoin().get()
    fun getModelLicenseUseCase(): GetModelLicenseUseCase = getKoin().get()
    // endregion

    // region Favorites
    fun getToggleFavoriteUseCase(): ToggleFavoriteUseCase = getKoin().get()
    fun getObserveFavoritesUseCase(): ObserveFavoritesUseCase = getKoin().get()
    fun getObserveIsFavoriteUseCase(): ObserveIsFavoriteUseCase = getKoin().get()
    // endregion

    // region Browsing History
    fun getTrackModelViewUseCase(): TrackModelViewUseCase = getKoin().get()
    fun getObserveRecentlyViewedUseCase(): ObserveRecentlyViewedUseCase = getKoin().get()
    fun getViewedModelIdsUseCase(): GetViewedModelIdsUseCase = getKoin().get()
    fun getClearBrowsingHistoryUseCase(): ClearBrowsingHistoryUseCase = getKoin().get()
    fun getDeleteBrowsingHistoryItemUseCase(): DeleteBrowsingHistoryItemUseCase = getKoin().get()
    fun getCleanupBrowsingHistoryUseCase(): CleanupBrowsingHistoryUseCase = getKoin().get()
    // endregion

    // region Hidden Models
    fun getHiddenModelIdsUseCase(): GetHiddenModelIdsUseCase = getKoin().get()
    fun getHideModelUseCase(): HideModelUseCase = getKoin().get()
    fun getUnhideModelUseCase(): UnhideModelUseCase = getKoin().get()
    fun getHiddenModelsUseCase(): GetHiddenModelsUseCase = getKoin().get()
    // endregion

    // region Collections
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
    // endregion

    // region Local Model Files
    fun getObserveModelDirectoriesUseCase(): ObserveModelDirectoriesUseCase = getKoin().get()
    fun getAddModelDirectoryUseCase(): AddModelDirectoryUseCase = getKoin().get()
    fun getRemoveModelDirectoryUseCase(): RemoveModelDirectoryUseCase = getKoin().get()
    fun getObserveLocalModelFilesUseCase(): ObserveLocalModelFilesUseCase = getKoin().get()
    fun getScanModelDirectoriesUseCase(): ScanModelDirectoriesUseCase = getKoin().get()
    fun getVerifyModelHashUseCase(): VerifyModelHashUseCase = getKoin().get()
    fun getObserveOwnedModelHashesUseCase(): ObserveOwnedModelHashesUseCase = getKoin().get()
    // endregion

    // region Prompts & Templates
    fun getAutoSavePromptUseCase(): AutoSavePromptUseCase = getKoin().get()
    fun getToggleTemplateUseCase(): ToggleTemplateUseCase = getKoin().get()
    fun getSearchSavedPromptsUseCase(): SearchSavedPromptsUseCase = getKoin().get()
    fun getObserveTemplatesUseCase(): ObserveTemplatesUseCase = getKoin().get()
    fun getSavePromptUseCase(): SavePromptUseCase = getKoin().get()
    fun getObserveSavedPromptsUseCase(): ObserveSavedPromptsUseCase = getKoin().get()
    fun getDeleteSavedPromptUseCase(): DeleteSavedPromptUseCase = getKoin().get()
    // endregion

    // region Theme & Display Settings
    fun getObserveAccentColorUseCase(): ObserveAccentColorUseCase = getKoin().get()
    fun getSetAccentColorUseCase(): SetAccentColorUseCase = getKoin().get()
    fun getObserveAmoledDarkModeUseCase(): ObserveAmoledDarkModeUseCase = getKoin().get()
    fun getSetAmoledDarkModeUseCase(): SetAmoledDarkModeUseCase = getKoin().get()
    fun getObserveThemeModeUseCase(): ObserveThemeModeUseCase = getKoin().get()
    fun getSetThemeModeUseCase(): SetThemeModeUseCase = getKoin().get()
    fun getObserveDefaultSortOrderUseCase(): ObserveDefaultSortOrderUseCase = getKoin().get()
    fun getSetDefaultSortOrderUseCase(): SetDefaultSortOrderUseCase = getKoin().get()
    fun getObserveDefaultTimePeriodUseCase(): ObserveDefaultTimePeriodUseCase = getKoin().get()
    fun getSetDefaultTimePeriodUseCase(): SetDefaultTimePeriodUseCase = getKoin().get()
    fun getObserveGridColumnsUseCase(): ObserveGridColumnsUseCase = getKoin().get()
    fun getSetGridColumnsUseCase(): SetGridColumnsUseCase = getKoin().get()
    fun getObserveQualityThresholdUseCase(): ObserveQualityThresholdUseCase = getKoin().get()
    fun getSetQualityThresholdUseCase(): SetQualityThresholdUseCase = getKoin().get()
    fun getObservePowerUserModeUseCase(): ObservePowerUserModeUseCase = getKoin().get()
    fun getSetPowerUserModeUseCase(): SetPowerUserModeUseCase = getKoin().get()
    fun getObserveCustomNavShortcutsUseCase(): ObserveCustomNavShortcutsUseCase = getKoin().get()
    fun getSetCustomNavShortcutsUseCase(): SetCustomNavShortcutsUseCase = getKoin().get()
    fun getObserveSeenTutorialVersionUseCase(): ObserveSeenTutorialVersionUseCase = getKoin().get()
    fun getSetSeenTutorialVersionUseCase(): SetSeenTutorialVersionUseCase = getKoin().get()
    // endregion

    // region NSFW & Content Filtering
    fun getObserveNsfwFilterUseCase(): ObserveNsfwFilterUseCase = getKoin().get()
    fun getSetNsfwFilterUseCase(): SetNsfwFilterUseCase = getKoin().get()
    fun getObserveNsfwBlurSettingsUseCase(): ObserveNsfwBlurSettingsUseCase = getKoin().get()
    fun getSetNsfwBlurSettingsUseCase(): SetNsfwBlurSettingsUseCase = getKoin().get()
    // endregion

    // region Authentication & API Key
    fun getObserveApiKeyUseCase(): ObserveApiKeyUseCase = getKoin().get()
    fun getSetApiKeyUseCase(): SetApiKeyUseCase = getKoin().get()
    fun getValidateApiKeyUseCase(): ValidateApiKeyUseCase = getKoin().get()
    fun getApiKeyProvider(): ApiKeyProvider = getKoin().get()
    // endregion

    // region Notifications & Model Updates
    fun getCheckModelUpdatesUseCase(): CheckModelUpdatesUseCase = getKoin().get()
    fun getObserveNotificationsEnabledUseCase(): ObserveNotificationsEnabledUseCase = getKoin().get()
    fun getSetNotificationsEnabledUseCase(): SetNotificationsEnabledUseCase = getKoin().get()
    fun getObservePollingIntervalUseCase(): ObservePollingIntervalUseCase = getKoin().get()
    fun getSetPollingIntervalUseCase(): SetPollingIntervalUseCase = getKoin().get()
    fun getModelUpdateNotificationsUseCase(): GetModelUpdateNotificationsUseCase = getKoin().get()
    fun getUnreadNotificationCountUseCase(): GetUnreadNotificationCountUseCase = getKoin().get()
    fun getMarkNotificationReadUseCase(): MarkNotificationReadUseCase = getKoin().get()
    fun getMarkAllNotificationsReadUseCase(): MarkAllNotificationsReadUseCase = getKoin().get()
    fun getCheckAndStoreModelUpdatesUseCase(): CheckAndStoreModelUpdatesUseCase = getKoin().get()
    // endregion

    // region Offline Cache & Network
    fun getObserveNetworkStatusUseCase(): ObserveNetworkStatusUseCase = getKoin().get()
    fun getCacheInfoUseCase(): GetCacheInfoUseCase = getKoin().get()
    fun getEvictCacheUseCase(): EvictCacheUseCase = getKoin().get()
    fun getClearCacheUseCase(): ClearCacheUseCase = getKoin().get()
    fun getObserveOfflineCacheEnabledUseCase(): ObserveOfflineCacheEnabledUseCase = getKoin().get()
    fun getSetOfflineCacheEnabledUseCase(): SetOfflineCacheEnabledUseCase = getKoin().get()
    fun getObserveCacheSizeLimitUseCase(): ObserveCacheSizeLimitUseCase = getKoin().get()
    fun getSetCacheSizeLimitUseCase(): SetCacheSizeLimitUseCase = getKoin().get()
    // endregion

    // region ComfyUI
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
    fun getInterruptComfyUIGenerationUseCase(): InterruptComfyUIGenerationUseCase = getKoin().get()
    fun getComfyUIConnectionRepository(): ComfyUIConnectionRepository = getKoin().get()
    fun getFindMatchingLocalModelUseCase(): FindMatchingLocalModelUseCase = getKoin().get()
    fun getPopulateGenerationFromModelUseCase(): PopulateGenerationFromModelUseCase = getKoin().get()
    fun getSaveGeneratedImageUseCase(): SaveGeneratedImageUseCase = getKoin().get()
    fun getFetchComfyUIHistoryUseCase(): FetchComfyUIHistoryUseCase = getKoin().get()
    fun getFetchComfyUIHistoryItemUseCase(): FetchComfyUIHistoryItemUseCase = getKoin().get()
    // endregion

    // region ComfyUI Workflow Templates
    fun getGetWorkflowTemplatesUseCase(): GetWorkflowTemplatesUseCase = getKoin().get()
    fun getSaveWorkflowTemplateUseCase(): SaveWorkflowTemplateUseCase = getKoin().get()
    fun getDeleteWorkflowTemplateUseCase(): DeleteWorkflowTemplateUseCase = getKoin().get()
    fun getExportWorkflowTemplateUseCase(): ExportWorkflowTemplateUseCase = getKoin().get()
    fun getImportWorkflowTemplateUseCase(): ImportWorkflowTemplateUseCase = getKoin().get()
    fun getApplyWorkflowTemplateUseCase(): ApplyWorkflowTemplateUseCase = getKoin().get()
    // endregion

    // region ComfyHub
    fun getSearchComfyHubWorkflowsUseCase(): SearchComfyHubWorkflowsUseCase = getKoin().get()
    fun getGetComfyHubWorkflowDetailUseCase(): GetComfyHubWorkflowDetailUseCase = getKoin().get()
    fun getImportComfyHubWorkflowUseCase(): ImportComfyHubWorkflowUseCase = getKoin().get()
    // endregion

    // region Civitai Link
    fun getObserveCivitaiLinkKeyUseCase(): ObserveCivitaiLinkKeyUseCase = getKoin().get()
    fun getSetCivitaiLinkKeyUseCase(): SetCivitaiLinkKeyUseCase = getKoin().get()
    fun getObserveCivitaiLinkStatusUseCase(): ObserveCivitaiLinkStatusUseCase = getKoin().get()
    fun getObserveCivitaiLinkActivitiesUseCase(): ObserveCivitaiLinkActivitiesUseCase = getKoin().get()
    fun getConnectCivitaiLinkUseCase(): ConnectCivitaiLinkUseCase = getKoin().get()
    fun getDisconnectCivitaiLinkUseCase(): DisconnectCivitaiLinkUseCase = getKoin().get()
    fun getSendResourceToPCUseCase(): SendResourceToPCUseCase = getKoin().get()
    fun getCancelLinkActivityUseCase(): CancelLinkActivityUseCase = getKoin().get()
    // endregion

    // region SD WebUI
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
    // endregion

    // region External Server
    fun getObserveExternalServerConfigsUseCase(): ObserveExternalServerConfigsUseCase = getKoin().get()
    fun getObserveActiveExternalServerConfigUseCase(): ObserveActiveExternalServerConfigUseCase = getKoin().get()
    fun getSaveExternalServerConfigUseCase(): SaveExternalServerConfigUseCase = getKoin().get()
    fun getDeleteExternalServerConfigUseCase(): DeleteExternalServerConfigUseCase = getKoin().get()
    fun getActivateExternalServerConfigUseCase(): ActivateExternalServerConfigUseCase = getKoin().get()
    fun getTestExternalServerConnectionUseCase(): TestExternalServerConnectionUseCase = getKoin().get()
    fun getGetExternalServerCapabilitiesUseCase(): GetExternalServerCapabilitiesUseCase = getKoin().get()
    fun getGetExternalServerImagesUseCase(): GetExternalServerImagesUseCase = getKoin().get()
    fun getGetGenerationOptionsUseCase(): GetGenerationOptionsUseCase = getKoin().get()
    fun getGetDependentChoicesUseCase(): GetDependentChoicesUseCase = getKoin().get()
    fun getExecuteGenerationUseCase(): ExecuteGenerationUseCase = getKoin().get()
    fun getGetGenerationStatusUseCase(): GetGenerationStatusUseCase = getKoin().get()
    fun getDeleteServerImagesUseCase(): DeleteServerImagesUseCase = getKoin().get()
    // endregion

    // region Dataset
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
    fun getDetectDuplicatesUseCase(): DetectDuplicatesUseCase = getKoin().get()
    fun getFilterByResolutionUseCase(): FilterByResolutionUseCase = getKoin().get()
    fun getMarkImageExcludedUseCase(): MarkImageExcludedUseCase = getKoin().get()
    fun getStorePHashUseCase(): StorePHashUseCase = getKoin().get()
    fun getStoreImageDimensionsUseCase(): StoreImageDimensionsUseCase = getKoin().get()
    fun getExportDatasetUseCase(): ExportDatasetUseCase = getKoin().get()
    fun getGetAvailableExportFormatsUseCase(): GetAvailableExportFormatsUseCase = getKoin().get()
    fun getExportWithPluginUseCase(): ExportWithPluginUseCase = getKoin().get()
    // endregion

    // region Model Notes & Personal Tags
    fun getObserveModelNoteUseCase(): ObserveModelNoteUseCase = getKoin().get()
    fun getSaveModelNoteUseCase(): SaveModelNoteUseCase = getKoin().get()
    fun getDeleteModelNoteUseCase(): DeleteModelNoteUseCase = getKoin().get()
    fun getObservePersonalTagsUseCase(): ObservePersonalTagsUseCase = getKoin().get()
    fun getAddPersonalTagUseCase(): AddPersonalTagUseCase = getKoin().get()
    fun getRemovePersonalTagUseCase(): RemovePersonalTagUseCase = getKoin().get()
    fun getGetAllPersonalTagsUseCase(): GetAllPersonalTagsUseCase = getKoin().get()
    fun getSearchModelsByTagUseCase(): SearchModelsByTagUseCase = getKoin().get()
    // endregion

    // region Analytics
    fun getBrowsingStatsUseCase(): GetBrowsingStatsUseCase = getKoin().get()
    // endregion

    // region Creator Follow & Feed
    fun getFollowCreatorUseCase(): FollowCreatorUseCase = getKoin().get()
    fun getUnfollowCreatorUseCase(): UnfollowCreatorUseCase = getKoin().get()
    fun getIsFollowingCreatorUseCase(): IsFollowingCreatorUseCase = getKoin().get()
    fun getCreatorFeedUseCase(): GetCreatorFeedUseCase = getKoin().get()
    fun getUnreadFeedCountUseCase(): GetUnreadFeedCountUseCase = getKoin().get()
    fun getMarkFeedReadUseCase(): MarkFeedReadUseCase = getKoin().get()
    fun getFollowedCreatorsUseCase(): GetFollowedCreatorsUseCase = getKoin().get()
    // endregion

    // region Reviews
    fun getModelReviewsUseCase(): GetModelReviewsUseCase = getKoin().get()
    fun getRatingTotalsUseCase(): GetRatingTotalsUseCase = getKoin().get()
    fun getSubmitReviewUseCase(): SubmitReviewUseCase = getKoin().get()
    // endregion

    // region Downloads
    fun getModelDownloadRepository(): ModelDownloadRepository = getKoin().get()
    fun getEnqueueDownloadUseCase(): EnqueueDownloadUseCase = getKoin().get()
    fun getObserveDownloadsUseCase(): ObserveDownloadsUseCase = getKoin().get()
    fun getObserveModelDownloadsUseCase(): ObserveModelDownloadsUseCase = getKoin().get()
    fun getCancelDownloadUseCase(): CancelDownloadUseCase = getKoin().get()
    fun getDeleteDownloadUseCase(): DeleteDownloadUseCase = getKoin().get()
    fun getClearCompletedDownloadsUseCase(): ClearCompletedDownloadsUseCase = getKoin().get()
    fun getPauseDownloadUseCase(): PauseDownloadUseCase = getKoin().get()
    fun getResumeDownloadUseCase(): ResumeDownloadUseCase = getKoin().get()
    fun getVerifyDownloadHashUseCase(): VerifyDownloadHashUseCase = getKoin().get()
    // endregion

    // region Backup & Restore
    fun getCreateBackupUseCase(): CreateBackupUseCase = getKoin().get()
    fun getRestoreBackupUseCase(): RestoreBackupUseCase = getKoin().get()
    fun getParseBackupUseCase(): ParseBackupUseCase = getKoin().get()
    // endregion

    // region Plugin Management
    fun getInstallPluginUseCase(): InstallPluginUseCase = getKoin().get()
    fun getUninstallPluginUseCase(): UninstallPluginUseCase = getKoin().get()
    fun getActivatePluginUseCase(): ActivatePluginUseCase = getKoin().get()
    fun getDeactivatePluginUseCase(): DeactivatePluginUseCase = getKoin().get()
    fun getObserveInstalledPluginsUseCase(): ObserveInstalledPluginsUseCase = getKoin().get()
    fun getGetPluginConfigUseCase(): GetPluginConfigUseCase = getKoin().get()
    fun getUpdatePluginConfigUseCase(): UpdatePluginConfigUseCase = getKoin().get()
    // endregion

    // region Theme Plugins
    fun getImportThemeUseCase(): ImportThemeUseCase = getKoin().get()
    fun getGetActiveThemeUseCase(): GetActiveThemeUseCase = getKoin().get()
    fun getObserveThemePluginsUseCase(): ObserveThemePluginsUseCase = getKoin().get()
    fun getActivateThemePluginUseCase(): ActivateThemePluginUseCase = getKoin().get()
    // endregion

    // region Share Hashtags
    fun getObserveShareHashtagsUseCase(): ObserveShareHashtagsUseCase = getKoin().get()
    fun getAddShareHashtagUseCase(): AddShareHashtagUseCase = getKoin().get()
    fun getRemoveShareHashtagUseCase(): RemoveShareHashtagUseCase = getKoin().get()
    fun getToggleShareHashtagUseCase(): ToggleShareHashtagUseCase = getKoin().get()
    // endregion

    // region Settings ViewModels
    fun createContentFilterSettingsViewModel(): ContentFilterSettingsViewModel = getKoin().get()
    fun createDisplaySettingsViewModel(): DisplaySettingsViewModel = getKoin().get()
    fun createAppBehaviorSettingsViewModel(): AppBehaviorSettingsViewModel = getKoin().get()
    fun createAuthSettingsViewModel(): AuthSettingsViewModel = getKoin().get()
    fun createStorageSettingsViewModel(): StorageSettingsViewModel = getKoin().get()
    // endregion

    // region Feature ViewModels (Phase 1)
    fun createCollectionsViewModel(): CollectionsViewModel = getKoin().get()
    fun createCollectionDetailViewModel(collectionId: Long): CollectionDetailViewModel =
        getKoin().get { org.koin.core.parameter.parametersOf(collectionId) }
    fun createCreatorProfileViewModel(username: String): CreatorProfileViewModel =
        getKoin().get { org.koin.core.parameter.parametersOf(username) }
    fun createSavedPromptsViewModel(): SavedPromptsViewModel = getKoin().get()

    // region Phase 2 ViewModels
    fun createImageGalleryViewModel(modelVersionId: Long): ImageGalleryViewModel =
        getKoin().get { org.koin.core.parameter.parametersOf(modelVersionId) }
    fun createSwipeDiscoveryViewModel(): SwipeDiscoveryViewModel = getKoin().get()
    fun createBrowsingHistoryViewModel(): BrowsingHistoryViewModel = getKoin().get()

    // region Phase 3 ViewModels
    fun createAnalyticsViewModel(): AnalyticsViewModel = getKoin().get()
    fun createBackupViewModel(): BackupViewModel = getKoin().get()
    fun createFeedViewModel(): FeedViewModel = getKoin().get()
    fun createNotificationCenterViewModel(): NotificationCenterViewModel = getKoin().get()
    fun createPluginManagementViewModel(): PluginManagementViewModel = getKoin().get()
    fun createShareViewModel(): ShareViewModel = getKoin().get()
    fun createGestureTutorialViewModel(): GestureTutorialViewModel = getKoin().get()
    fun createUpdateViewModel(): UpdateViewModel = getKoin().get()
    fun createDatasetListViewModel(): DatasetListViewModel = getKoin().get()
    fun createSimilarModelsViewModel(modelId: Long): SimilarModelsViewModel =
        getKoin().get { org.koin.core.parameter.parametersOf(modelId) }
    fun createDatasetDetailViewModel(datasetId: Long): DatasetDetailViewModel =
        getKoin().get { org.koin.core.parameter.parametersOf(datasetId) }
    fun createBatchTagEditorViewModel(datasetId: Long): BatchTagEditorViewModel =
        getKoin().get { org.koin.core.parameter.parametersOf(datasetId) }
    // endregion
}
