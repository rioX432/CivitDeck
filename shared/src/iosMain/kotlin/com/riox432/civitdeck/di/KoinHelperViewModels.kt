// iOS Koin factory bridge: many cohesive single-line ViewModel accessors for Swift interop.
@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.repository.ModelDownloadRepository
import com.riox432.civitdeck.domain.usecase.CancelDownloadUseCase
import com.riox432.civitdeck.domain.usecase.ClearCompletedDownloadsUseCase
import com.riox432.civitdeck.domain.usecase.DeleteDownloadUseCase
import com.riox432.civitdeck.domain.usecase.EnqueueDownloadUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDownloadsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelDownloadsUseCase
import com.riox432.civitdeck.domain.usecase.PauseDownloadUseCase
import com.riox432.civitdeck.domain.usecase.ResumeDownloadUseCase
import com.riox432.civitdeck.domain.usecase.VerifyDownloadHashUseCase
import com.riox432.civitdeck.feature.collections.presentation.BatchTagEditorViewModel
import com.riox432.civitdeck.feature.collections.presentation.CollectionDetailViewModel
import com.riox432.civitdeck.feature.collections.presentation.CollectionsViewModel
import com.riox432.civitdeck.feature.collections.presentation.DatasetDetailViewModel
import com.riox432.civitdeck.feature.collections.presentation.DatasetListViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.CivitaiLinkSendViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.CivitaiLinkSettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyHubBrowserViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyHubDetailViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIQueueViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ConnectionOnboardingViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.MaskEditorViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.WorkflowTemplateViewModel
import com.riox432.civitdeck.feature.creator.presentation.CreatorProfileViewModel
import com.riox432.civitdeck.feature.creator.presentation.FeedViewModel
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerGalleryViewModel
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerSettingsViewModel
import com.riox432.civitdeck.feature.gallery.presentation.AnalyticsViewModel
import com.riox432.civitdeck.feature.gallery.presentation.DownloadQueueViewModel
import com.riox432.civitdeck.feature.gallery.presentation.GestureTutorialViewModel
import com.riox432.civitdeck.feature.gallery.presentation.ImageGalleryViewModel
import com.riox432.civitdeck.feature.gallery.presentation.ModelFileBrowserViewModel
import com.riox432.civitdeck.feature.gallery.presentation.NotificationCenterViewModel
import com.riox432.civitdeck.feature.gallery.presentation.ShareViewModel
import com.riox432.civitdeck.feature.prompts.presentation.SavedPromptsViewModel
import com.riox432.civitdeck.feature.search.presentation.BrowsingHistoryViewModel
import com.riox432.civitdeck.feature.search.presentation.ModelSearchViewModel
import com.riox432.civitdeck.feature.search.presentation.SwipeDiscoveryViewModel
import com.riox432.civitdeck.feature.settings.presentation.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.AuthSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.BackupViewModel
import com.riox432.civitdeck.feature.settings.presentation.ContentFilterSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.StorageSettingsViewModel
import com.riox432.civitdeck.presentation.plugin.PluginManagementViewModel
import org.koin.core.parameter.parametersOf

// Settings ViewModels
fun KoinHelper.createContentFilterSettingsViewModel(): ContentFilterSettingsViewModel = resolve()
fun KoinHelper.createDisplaySettingsViewModel(): DisplaySettingsViewModel = resolve()
fun KoinHelper.createAppBehaviorSettingsViewModel(): AppBehaviorSettingsViewModel = resolve()
fun KoinHelper.createAuthSettingsViewModel(): AuthSettingsViewModel = resolve()
fun KoinHelper.createStorageSettingsViewModel(): StorageSettingsViewModel = resolve()

// Feature Module ViewModels
fun KoinHelper.createCreatorProfileViewModel(username: String): CreatorProfileViewModel =
    resolve { parametersOf(username) }
fun KoinHelper.createSavedPromptsViewModel(): SavedPromptsViewModel = resolve()
fun KoinHelper.createCollectionsViewModel(): CollectionsViewModel = resolve()
fun KoinHelper.createCollectionDetailViewModel(collectionId: Long): CollectionDetailViewModel =
    resolve { parametersOf(collectionId) }
fun KoinHelper.createImageGalleryViewModel(modelVersionId: Long): ImageGalleryViewModel =
    resolve { parametersOf(modelVersionId) }
fun KoinHelper.createModelSearchViewModel(): ModelSearchViewModel = resolve()
fun KoinHelper.createSwipeDiscoveryViewModel(): SwipeDiscoveryViewModel = resolve()
fun KoinHelper.createBrowsingHistoryViewModel(): BrowsingHistoryViewModel = resolve()
fun KoinHelper.createModelDetailViewModel(modelId: Long): ModelDetailViewModel =
    resolve { parametersOf(modelId) }

// Phase 3 ViewModels
fun KoinHelper.createAnalyticsViewModel(): AnalyticsViewModel = resolve()
fun KoinHelper.createBackupViewModel(): BackupViewModel = resolve()
fun KoinHelper.createFeedViewModel(): FeedViewModel = resolve()
fun KoinHelper.createNotificationCenterViewModel(): NotificationCenterViewModel = resolve()
fun KoinHelper.createPluginManagementViewModel(): PluginManagementViewModel = resolve()
fun KoinHelper.createShareViewModel(): ShareViewModel = resolve()
fun KoinHelper.createGestureTutorialViewModel(): GestureTutorialViewModel = resolve()
fun KoinHelper.createDatasetListViewModel(): DatasetListViewModel = resolve()
fun KoinHelper.createDatasetDetailViewModel(datasetId: Long): DatasetDetailViewModel =
    resolve { parametersOf(datasetId) }
fun KoinHelper.createBatchTagEditorViewModel(datasetId: Long): BatchTagEditorViewModel =
    resolve { parametersOf(datasetId) }
fun KoinHelper.createDownloadQueueViewModel(): DownloadQueueViewModel = resolve()

// Phase 4 ViewModels
fun KoinHelper.createComfyUIGenerationViewModel(): ComfyUIGenerationViewModel = resolve()
fun KoinHelper.createMaskEditorViewModel(): MaskEditorViewModel = resolve()
fun KoinHelper.createComfyUIQueueViewModel(): ComfyUIQueueViewModel = resolve()
fun KoinHelper.createComfyUIHistoryViewModel(): ComfyUIHistoryViewModel = resolve()
fun KoinHelper.createComfyUISettingsViewModel(): ComfyUISettingsViewModel = resolve()
fun KoinHelper.createConnectionOnboardingViewModel(): ConnectionOnboardingViewModel = resolve()
fun KoinHelper.createSDWebUIGenerationViewModel(): SDWebUIGenerationViewModel = resolve()
fun KoinHelper.createSDWebUISettingsViewModel(): SDWebUISettingsViewModel = resolve()
fun KoinHelper.createCivitaiLinkSettingsViewModel(): CivitaiLinkSettingsViewModel = resolve()
fun KoinHelper.createCivitaiLinkSendViewModel(): CivitaiLinkSendViewModel = resolve()
fun KoinHelper.createWorkflowTemplateViewModel(): WorkflowTemplateViewModel = resolve()
fun KoinHelper.createComfyHubBrowserViewModel(): ComfyHubBrowserViewModel = resolve()
fun KoinHelper.createComfyHubDetailViewModel(workflowId: String): ComfyHubDetailViewModel =
    resolve { parametersOf(workflowId) }
fun KoinHelper.createExternalServerSettingsViewModel(): ExternalServerSettingsViewModel = resolve()
fun KoinHelper.createExternalServerGalleryViewModel(): ExternalServerGalleryViewModel = resolve()
fun KoinHelper.createModelFileBrowserViewModel(): ModelFileBrowserViewModel = resolve()

// Downloads
fun KoinHelper.getModelDownloadRepository(): ModelDownloadRepository = resolve()
fun KoinHelper.getEnqueueDownloadUseCase(): EnqueueDownloadUseCase = resolve()
fun KoinHelper.getObserveDownloadsUseCase(): ObserveDownloadsUseCase = resolve()
fun KoinHelper.getObserveModelDownloadsUseCase(): ObserveModelDownloadsUseCase = resolve()
fun KoinHelper.getCancelDownloadUseCase(): CancelDownloadUseCase = resolve()
fun KoinHelper.getDeleteDownloadUseCase(): DeleteDownloadUseCase = resolve()
fun KoinHelper.getClearCompletedDownloadsUseCase(): ClearCompletedDownloadsUseCase = resolve()
fun KoinHelper.getPauseDownloadUseCase(): PauseDownloadUseCase = resolve()
fun KoinHelper.getResumeDownloadUseCase(): ResumeDownloadUseCase = resolve()
fun KoinHelper.getVerifyDownloadHashUseCase(): VerifyDownloadHashUseCase = resolve()
