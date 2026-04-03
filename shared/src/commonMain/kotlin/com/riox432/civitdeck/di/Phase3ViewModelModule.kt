package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.ActivatePluginUseCase
import com.riox432.civitdeck.domain.usecase.AddShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.BatchEditTagsUseCase
import com.riox432.civitdeck.domain.usecase.CheckForUpdateUseCase
import com.riox432.civitdeck.domain.usecase.CreateBackupUseCase
import com.riox432.civitdeck.domain.usecase.CreateDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeactivatePluginUseCase
import com.riox432.civitdeck.domain.usecase.DeleteDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DetectDuplicatesUseCase
import com.riox432.civitdeck.domain.usecase.EditCaptionUseCase
import com.riox432.civitdeck.domain.usecase.GetBrowsingStatsUseCase
import com.riox432.civitdeck.domain.usecase.GetCreatorFeedUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.GetModelUpdateNotificationsUseCase
import com.riox432.civitdeck.domain.usecase.GetPluginConfigUseCase
import com.riox432.civitdeck.domain.usecase.GetSimilarModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetTagSuggestionsUseCase
import com.riox432.civitdeck.domain.usecase.GetUnreadFeedCountUseCase
import com.riox432.civitdeck.domain.usecase.MarkAllNotificationsReadUseCase
import com.riox432.civitdeck.domain.usecase.MarkFeedReadUseCase
import com.riox432.civitdeck.domain.usecase.MarkNotificationReadUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAutoUpdateCheckUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetImagesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveInstalledPluginsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSeenTutorialVersionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveShareHashtagsUseCase
import com.riox432.civitdeck.domain.usecase.ParseBackupUseCase
import com.riox432.civitdeck.domain.usecase.RemoveImageFromDatasetUseCase
import com.riox432.civitdeck.domain.usecase.RemoveShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.RenameDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.RestoreBackupUseCase
import com.riox432.civitdeck.domain.usecase.SetAutoUpdateCheckUseCase
import com.riox432.civitdeck.domain.usecase.SetSeenTutorialVersionUseCase
import com.riox432.civitdeck.domain.usecase.ToggleShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.UninstallPluginUseCase
import com.riox432.civitdeck.domain.usecase.UpdatePluginConfigUseCase
import com.riox432.civitdeck.domain.usecase.UpdateTrainableUseCase
import com.riox432.civitdeck.presentation.analytics.AnalyticsViewModel
import com.riox432.civitdeck.presentation.backup.BackupViewModel
import com.riox432.civitdeck.presentation.dataset.BatchTagEditorViewModel
import com.riox432.civitdeck.presentation.dataset.DatasetDetailViewModel
import com.riox432.civitdeck.presentation.dataset.DatasetListViewModel
import com.riox432.civitdeck.presentation.feed.FeedViewModel
import com.riox432.civitdeck.presentation.notificationcenter.NotificationCenterViewModel
import com.riox432.civitdeck.presentation.plugin.PluginManagementViewModel
import com.riox432.civitdeck.presentation.share.ShareViewModel
import com.riox432.civitdeck.presentation.similar.SimilarModelsViewModel
import com.riox432.civitdeck.presentation.tutorial.GestureTutorialViewModel
import com.riox432.civitdeck.presentation.update.UpdateViewModel
import com.riox432.civitdeck.usecase.ExportWithPluginUseCase
import com.riox432.civitdeck.usecase.GetAvailableExportFormatsUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@Suppress("LongMethod")
val phase3ViewModelModule = module {
    viewModel { AnalyticsViewModel(get<GetBrowsingStatsUseCase>()) }
    viewModel {
        BackupViewModel(
            createBackupUseCase = get<CreateBackupUseCase>(),
            restoreBackupUseCase = get<RestoreBackupUseCase>(),
            parseBackupUseCase = get<ParseBackupUseCase>(),
        )
    }
    viewModel {
        FeedViewModel(
            getCreatorFeedUseCase = get<GetCreatorFeedUseCase>(),
            getUnreadFeedCountUseCase = get<GetUnreadFeedCountUseCase>(),
            markFeedReadUseCase = get<MarkFeedReadUseCase>(),
        )
    }
    viewModel {
        NotificationCenterViewModel(
            getNotificationsUseCase = get<GetModelUpdateNotificationsUseCase>(),
            markReadUseCase = get<MarkNotificationReadUseCase>(),
            markAllReadUseCase = get<MarkAllNotificationsReadUseCase>(),
        )
    }
    viewModel {
        PluginManagementViewModel(
            observeInstalledPluginsUseCase = get<ObserveInstalledPluginsUseCase>(),
            activatePluginUseCase = get<ActivatePluginUseCase>(),
            deactivatePluginUseCase = get<DeactivatePluginUseCase>(),
            uninstallPluginUseCase = get<UninstallPluginUseCase>(),
            getPluginConfigUseCase = get<GetPluginConfigUseCase>(),
            updatePluginConfigUseCase = get<UpdatePluginConfigUseCase>(),
        )
    }
    viewModel { params ->
        SimilarModelsViewModel(
            modelId = params.get(),
            getModelDetail = get<GetModelDetailUseCase>(),
            getSimilarModels = get<GetSimilarModelsUseCase>(),
        )
    }
    viewModel {
        ShareViewModel(
            observeShareHashtags = get<ObserveShareHashtagsUseCase>(),
            addShareHashtag = get<AddShareHashtagUseCase>(),
            removeShareHashtag = get<RemoveShareHashtagUseCase>(),
            toggleShareHashtag = get<ToggleShareHashtagUseCase>(),
        )
    }
    viewModel {
        GestureTutorialViewModel(
            observeSeenTutorialVersion = get<ObserveSeenTutorialVersionUseCase>(),
            setSeenTutorialVersion = get<SetSeenTutorialVersionUseCase>(),
        )
    }
    viewModel {
        UpdateViewModel(
            checkForUpdateUseCase = get<CheckForUpdateUseCase>(),
            observeAutoUpdateCheckUseCase = get<ObserveAutoUpdateCheckUseCase>(),
            setAutoUpdateCheckUseCase = get<SetAutoUpdateCheckUseCase>(),
        )
    }
    viewModel {
        DatasetListViewModel(
            observeDatasetCollectionsUseCase = get<ObserveDatasetCollectionsUseCase>(),
            createDatasetCollectionUseCase = get<CreateDatasetCollectionUseCase>(),
            renameDatasetCollectionUseCase = get<RenameDatasetCollectionUseCase>(),
            deleteDatasetCollectionUseCase = get<DeleteDatasetCollectionUseCase>(),
        )
    }
    viewModel { params ->
        DatasetDetailViewModel(
            datasetId = params.get(),
            observeDatasetImagesUseCase = get<ObserveDatasetImagesUseCase>(),
            removeImageFromDatasetUseCase = get<RemoveImageFromDatasetUseCase>(),
            editCaptionUseCase = get<EditCaptionUseCase>(),
            updateTrainableUseCase = get<UpdateTrainableUseCase>(),
            detectDuplicatesUseCase = get<DetectDuplicatesUseCase>(),
            exportWithPluginUseCase = get<ExportWithPluginUseCase>(),
            getAvailableExportFormatsUseCase = get<GetAvailableExportFormatsUseCase>(),
        )
    }
    viewModel { params ->
        BatchTagEditorViewModel(
            datasetId = params.get(),
            observeDatasetImagesUseCase = get<ObserveDatasetImagesUseCase>(),
            batchEditTagsUseCase = get<BatchEditTagsUseCase>(),
            getTagSuggestionsUseCase = get<GetTagSuggestionsUseCase>(),
        )
    }
}
