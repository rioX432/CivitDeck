@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.di

import com.riox432.civitdeck.feature.comfyui.presentation.CivitaiLinkSendViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.CivitaiLinkSettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyHubBrowserViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyHubDetailViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIQueueViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.MaskEditorViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.WorkflowTemplateViewModel
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerGalleryViewModel
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerSettingsViewModel
import com.riox432.civitdeck.feature.gallery.presentation.DownloadQueueViewModel
import com.riox432.civitdeck.feature.gallery.presentation.ModelFileBrowserViewModel
import com.riox432.civitdeck.feature.settings.presentation.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.AuthSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.ContentFilterSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.StorageSettingsViewModel
import org.koin.mp.KoinPlatform.getKoin

// Settings ViewModels
fun KoinHelper.createContentFilterSettingsViewModel(): ContentFilterSettingsViewModel = getKoin().get()
fun KoinHelper.createDisplaySettingsViewModel(): DisplaySettingsViewModel = getKoin().get()
fun KoinHelper.createAppBehaviorSettingsViewModel(): AppBehaviorSettingsViewModel = getKoin().get()
fun KoinHelper.createAuthSettingsViewModel(): AuthSettingsViewModel = getKoin().get()
fun KoinHelper.createStorageSettingsViewModel(): StorageSettingsViewModel = getKoin().get()

// Feature Module ViewModels
fun KoinHelper.createCreatorProfileViewModel(username: String):
    com.riox432.civitdeck.feature.creator.presentation.CreatorProfileViewModel =
    getKoin().get { org.koin.core.parameter.parametersOf(username) }
fun KoinHelper.createSavedPromptsViewModel():
    com.riox432.civitdeck.feature.prompts.presentation.SavedPromptsViewModel = getKoin().get()
fun KoinHelper.createCollectionsViewModel():
    com.riox432.civitdeck.feature.collections.presentation.CollectionsViewModel = getKoin().get()
fun KoinHelper.createCollectionDetailViewModel(collectionId: Long):
    com.riox432.civitdeck.feature.collections.presentation.CollectionDetailViewModel =
    getKoin().get { org.koin.core.parameter.parametersOf(collectionId) }
fun KoinHelper.createImageGalleryViewModel(modelVersionId: Long):
    com.riox432.civitdeck.feature.gallery.presentation.ImageGalleryViewModel =
    getKoin().get { org.koin.core.parameter.parametersOf(modelVersionId) }
fun KoinHelper.createModelSearchViewModel():
    com.riox432.civitdeck.feature.search.presentation.ModelSearchViewModel = getKoin().get()
fun KoinHelper.createSwipeDiscoveryViewModel():
    com.riox432.civitdeck.feature.search.presentation.SwipeDiscoveryViewModel = getKoin().get()
fun KoinHelper.createBrowsingHistoryViewModel():
    com.riox432.civitdeck.feature.search.presentation.BrowsingHistoryViewModel = getKoin().get()
fun KoinHelper.createModelDetailViewModel(
    modelId: Long
): com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel =
    getKoin().get { org.koin.core.parameter.parametersOf(modelId) }

// Phase 3 ViewModels
fun KoinHelper.createAnalyticsViewModel():
    com.riox432.civitdeck.feature.gallery.presentation.AnalyticsViewModel = getKoin().get()
fun KoinHelper.createBackupViewModel():
    com.riox432.civitdeck.feature.settings.presentation.BackupViewModel = getKoin().get()
fun KoinHelper.createFeedViewModel(): com.riox432.civitdeck.feature.creator.presentation.FeedViewModel = getKoin().get()
fun KoinHelper.createNotificationCenterViewModel():
    com.riox432.civitdeck.feature.gallery.presentation.NotificationCenterViewModel = getKoin().get()
fun KoinHelper.createPluginManagementViewModel():
    com.riox432.civitdeck.presentation.plugin.PluginManagementViewModel = getKoin().get()
fun KoinHelper.createSimilarModelsViewModel(modelId: Long):
    com.riox432.civitdeck.feature.search.presentation.SimilarModelsViewModel =
    getKoin().get { org.koin.core.parameter.parametersOf(modelId) }
fun KoinHelper.createTextSearchViewModel():
    com.riox432.civitdeck.feature.search.presentation.TextSearchViewModel = getKoin().get()
fun KoinHelper.createShareViewModel():
    com.riox432.civitdeck.feature.gallery.presentation.ShareViewModel = getKoin().get()
fun KoinHelper.createGestureTutorialViewModel():
    com.riox432.civitdeck.feature.gallery.presentation.GestureTutorialViewModel = getKoin().get()
fun KoinHelper.createDatasetListViewModel():
    com.riox432.civitdeck.feature.collections.presentation.DatasetListViewModel = getKoin().get()
fun KoinHelper.createDatasetDetailViewModel(
    datasetId: Long
): com.riox432.civitdeck.feature.collections.presentation.DatasetDetailViewModel =
    getKoin().get { org.koin.core.parameter.parametersOf(datasetId) }
fun KoinHelper.createBatchTagEditorViewModel(
    datasetId: Long
): com.riox432.civitdeck.feature.collections.presentation.BatchTagEditorViewModel =
    getKoin().get { org.koin.core.parameter.parametersOf(datasetId) }
fun KoinHelper.createDownloadQueueViewModel(): DownloadQueueViewModel = getKoin().get()

// Phase 4 ViewModels
fun KoinHelper.createComfyUIGenerationViewModel(): ComfyUIGenerationViewModel = getKoin().get()
fun KoinHelper.createMaskEditorViewModel(): MaskEditorViewModel = getKoin().get()
fun KoinHelper.createComfyUIQueueViewModel(): ComfyUIQueueViewModel = getKoin().get()
fun KoinHelper.createComfyUIHistoryViewModel(): ComfyUIHistoryViewModel = getKoin().get()
fun KoinHelper.createComfyUISettingsViewModel(): ComfyUISettingsViewModel = getKoin().get()
fun KoinHelper.createSDWebUIGenerationViewModel(): SDWebUIGenerationViewModel = getKoin().get()
fun KoinHelper.createSDWebUISettingsViewModel(): SDWebUISettingsViewModel = getKoin().get()
fun KoinHelper.createCivitaiLinkSettingsViewModel(): CivitaiLinkSettingsViewModel = getKoin().get()
fun KoinHelper.createCivitaiLinkSendViewModel(): CivitaiLinkSendViewModel = getKoin().get()
fun KoinHelper.createWorkflowTemplateViewModel(): WorkflowTemplateViewModel = getKoin().get()
fun KoinHelper.createComfyHubBrowserViewModel(): ComfyHubBrowserViewModel = getKoin().get()
fun KoinHelper.createComfyHubDetailViewModel(workflowId: String): ComfyHubDetailViewModel =
    getKoin().get { org.koin.core.parameter.parametersOf(workflowId) }
fun KoinHelper.createExternalServerSettingsViewModel(): ExternalServerSettingsViewModel = getKoin().get()
fun KoinHelper.createExternalServerGalleryViewModel(): ExternalServerGalleryViewModel = getKoin().get()
fun KoinHelper.createModelFileBrowserViewModel(): ModelFileBrowserViewModel = getKoin().get()

// Downloads
fun KoinHelper.getModelDownloadRepository():
    com.riox432.civitdeck.domain.repository.ModelDownloadRepository = getKoin().get()
fun KoinHelper.getEnqueueDownloadUseCase():
    com.riox432.civitdeck.domain.usecase.EnqueueDownloadUseCase = getKoin().get()
fun KoinHelper.getObserveDownloadsUseCase():
    com.riox432.civitdeck.domain.usecase.ObserveDownloadsUseCase = getKoin().get()
fun KoinHelper.getObserveModelDownloadsUseCase():
    com.riox432.civitdeck.domain.usecase.ObserveModelDownloadsUseCase = getKoin().get()
fun KoinHelper.getCancelDownloadUseCase(): com.riox432.civitdeck.domain.usecase.CancelDownloadUseCase = getKoin().get()
fun KoinHelper.getDeleteDownloadUseCase(): com.riox432.civitdeck.domain.usecase.DeleteDownloadUseCase = getKoin().get()
fun KoinHelper.getClearCompletedDownloadsUseCase():
    com.riox432.civitdeck.domain.usecase.ClearCompletedDownloadsUseCase = getKoin().get()
fun KoinHelper.getPauseDownloadUseCase(): com.riox432.civitdeck.domain.usecase.PauseDownloadUseCase = getKoin().get()
fun KoinHelper.getResumeDownloadUseCase(): com.riox432.civitdeck.domain.usecase.ResumeDownloadUseCase = getKoin().get()
fun KoinHelper.getVerifyDownloadHashUseCase():
    com.riox432.civitdeck.domain.usecase.VerifyDownloadHashUseCase = getKoin().get()
