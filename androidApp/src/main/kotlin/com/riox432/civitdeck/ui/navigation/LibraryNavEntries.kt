package com.riox432.civitdeck.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import com.riox432.civitdeck.feature.collections.presentation.CollectionDetailViewModel
import com.riox432.civitdeck.feature.collections.presentation.CollectionsViewModel
import com.riox432.civitdeck.feature.creator.presentation.CreatorProfileViewModel
import com.riox432.civitdeck.feature.prompts.presentation.SavedPromptsViewModel
import com.riox432.civitdeck.ui.analytics.AnalyticsScreen
import com.riox432.civitdeck.ui.analytics.AnalyticsViewModel
import com.riox432.civitdeck.ui.collections.CollectionDetailScreen
import com.riox432.civitdeck.ui.collections.CollectionsScreen
import com.riox432.civitdeck.ui.compare.ModelCompareScreen
import com.riox432.civitdeck.ui.creator.CreatorProfileScreen
import com.riox432.civitdeck.ui.dataset.BatchTagEditorScreen
import com.riox432.civitdeck.ui.dataset.BatchTagEditorViewModel
import com.riox432.civitdeck.ui.dataset.DatasetDetailScreen
import com.riox432.civitdeck.ui.dataset.DatasetDetailViewModel
import com.riox432.civitdeck.ui.dataset.DatasetListScreen
import com.riox432.civitdeck.ui.dataset.DatasetListViewModel
import com.riox432.civitdeck.ui.dataset.DuplicateReviewScreen
import com.riox432.civitdeck.ui.dataset.DuplicateReviewViewModel
import com.riox432.civitdeck.ui.detail.ModelDetailScreen
import com.riox432.civitdeck.ui.detail.ModelDetailViewModel
import com.riox432.civitdeck.ui.downloadqueue.DownloadQueueScreen
import com.riox432.civitdeck.ui.downloadqueue.DownloadQueueViewModel
import com.riox432.civitdeck.ui.feed.FeedScreen
import com.riox432.civitdeck.ui.feed.FeedViewModel
import com.riox432.civitdeck.ui.gallery.ImageGalleryScreen
import com.riox432.civitdeck.ui.gallery.ImageGalleryViewModel
import com.riox432.civitdeck.ui.history.BrowsingHistoryScreen
import com.riox432.civitdeck.ui.history.BrowsingHistoryViewModel
import com.riox432.civitdeck.ui.notificationcenter.NotificationCenterScreen
import com.riox432.civitdeck.ui.notificationcenter.NotificationCenterViewModel
import com.riox432.civitdeck.ui.share.ShareViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

internal fun EntryProviderScope<Any>.collectionsEntry(backStack: MutableList<Any>) {
    entry<CollectionsRoute> {
        val viewModel: CollectionsViewModel = koinViewModel()
        val promptsViewModel: SavedPromptsViewModel = koinViewModel()
        val collections by viewModel.collections.collectAsStateWithLifecycle()
        CollectionsScreen(
            collections = collections,
            onCollectionClick = { id, name ->
                backStack.add(CollectionDetailRoute(id, name))
            },
            onCreateCollection = viewModel::createCollection,
            onRenameCollection = viewModel::renameCollection,
            onDeleteCollection = viewModel::deleteCollection,
            promptsViewModel = promptsViewModel,
            onNavigateToDatasets = { backStack.add(DatasetListRoute) },
        )
    }
}

internal fun EntryProviderScope<Any>.collectionDetailEntry(
    backStack: MutableList<Any>,
    compareModelId: Long?,
    onCancelCompare: () -> Unit,
) {
    entry<CollectionDetailRoute> { key ->
        val viewModel: CollectionDetailViewModel = koinViewModel(
            key = "collection_${key.collectionId}",
        ) { parametersOf(key.collectionId) }
        val models by viewModel.displayModels.collectAsStateWithLifecycle()
        val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
        val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
        val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
        val selectedIds by viewModel.selectedModelIds.collectAsStateWithLifecycle()
        val collections by viewModel.collections.collectAsStateWithLifecycle()
        CollectionDetailScreen(
            collectionName = key.collectionName,
            models = models,
            sortOrder = sortOrder,
            typeFilter = typeFilter,
            isSelectionMode = isSelectionMode,
            selectedIds = selectedIds,
            collections = collections,
            collectionId = key.collectionId,
            onBack = { backStack.removeLastOrNull() },
            onModelClick = { modelId ->
                val cmpId = compareModelId
                if (cmpId != null) {
                    backStack.add(CompareRoute(cmpId, modelId))
                    onCancelCompare()
                } else {
                    backStack.add(DetailRoute(modelId))
                }
            },
            onSortChange = { viewModel.sortOrder.value = it },
            onTypeFilterChange = { viewModel.typeFilter.value = it },
            onToggleSelection = viewModel::toggleSelection,
            onEnterSelectionMode = viewModel::enterSelectionMode,
            onSelectAll = viewModel::selectAll,
            onClearSelection = viewModel::clearSelection,
            onRemoveSelected = viewModel::removeSelected,
            onMoveSelectedTo = viewModel::moveSelectedTo,
        )
    }
}

internal fun EntryProviderScope<Any>.datasetListEntry(backStack: MutableList<Any>) {
    entry<DatasetListRoute> {
        val viewModel: DatasetListViewModel = koinViewModel()
        DatasetListScreen(
            viewModel = viewModel,
            onDatasetClick = { id, name -> backStack.add(DatasetDetailRoute(id, name)) },
            onBack = { backStack.removeLastOrNull() },
        )
    }
}

internal fun EntryProviderScope<Any>.datasetDetailEntry(backStack: MutableList<Any>) {
    entry<DatasetDetailRoute> { key ->
        val viewModel: DatasetDetailViewModel = koinViewModel(
            key = "dataset_${key.datasetId}",
        ) { parametersOf(key.datasetId) }
        DatasetDetailScreen(
            datasetName = key.datasetName,
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onNavigateToBatchTagEditor = { datasetId ->
                backStack.add(BatchTagEditorRoute(datasetId))
            },
            onNavigateToDuplicateReview = { datasetId ->
                backStack.add(DuplicateReviewRoute(datasetId))
            },
        )
    }
}

internal fun EntryProviderScope<Any>.batchTagEditorEntry(backStack: MutableList<Any>) {
    entry<BatchTagEditorRoute> { key ->
        val viewModel: BatchTagEditorViewModel = koinViewModel(
            parameters = { parametersOf(key.datasetId) },
        )
        BatchTagEditorScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
}

internal fun EntryProviderScope<Any>.duplicateReviewEntry(backStack: MutableList<Any>) {
    entry<DuplicateReviewRoute> { key ->
        val viewModel: DuplicateReviewViewModel = koinViewModel(
            parameters = { parametersOf(key.datasetId) },
        )
        DuplicateReviewScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
}

internal fun EntryProviderScope<Any>.detailEntry(backStack: MutableList<Any>) {
    entry<DetailRoute> { key ->
        val viewModel: ModelDetailViewModel = koinViewModel(
            key = key.modelId.toString(),
        ) { parametersOf(key.modelId) }
        val shareVm: ShareViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val shareHashtags by shareVm.hashtags.collectAsStateWithLifecycle()
        ModelDetailScreen(
            viewModel = viewModel,
            modelId = key.modelId,
            initialThumbnailUrl = key.thumbnailUrl,
            sharedElementSuffix = key.sharedElementSuffix,
            shareHashtags = shareHashtags,
            onToggleShareHashtag = shareVm::onToggle,
            onAddShareHashtag = shareVm::onAdd,
            onRemoveShareHashtag = shareVm::onRemove,
            onBack = { backStack.removeLastOrNull() },
            onViewImages = { modelVersionId ->
                backStack.add(ImageGalleryRoute(modelVersionId))
            },
            onCreatorClick = { username ->
                backStack.add(CreatorRoute(username))
            },
            onTryInComfyUI = if (uiState.powerUserMode) {
                { sha256, modelName, meta ->
                    backStack.add(
                        ComfyUIBridgeRoute(
                            modelId = key.modelId,
                            versionId = uiState.model?.modelVersions
                                ?.getOrNull(uiState.selectedVersionIndex)?.id ?: 0L,
                            sha256Hash = sha256,
                            modelName = modelName,
                            prompt = meta?.prompt,
                            negativePrompt = meta?.negativePrompt,
                            steps = meta?.steps,
                            cfgScale = meta?.cfgScale,
                            seed = meta?.seed,
                            sampler = meta?.sampler,
                        )
                    )
                }
            } else {
                null
            },
            onFindSimilar = { modelId -> backStack.add(SimilarModelsRoute(modelId)) },
        )
    }
}

internal fun EntryProviderScope<Any>.creatorEntry(backStack: MutableList<Any>) {
    entry<CreatorRoute> { key ->
        val viewModel: CreatorProfileViewModel = koinViewModel(
            key = "creator_${key.username}",
        ) { parametersOf(key.username) }
        CreatorProfileScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onModelClick = { modelId, thumbnailUrl ->
                backStack.add(DetailRoute(modelId, thumbnailUrl))
            },
        )
    }
}

internal fun EntryProviderScope<Any>.galleryEntry(backStack: MutableList<Any>) {
    entry<ImageGalleryRoute> { key ->
        val viewModel: ImageGalleryViewModel = koinViewModel(
            key = "gallery_${key.modelVersionId}",
        ) { parametersOf(key.modelVersionId) }
        val shareVm: ShareViewModel = koinViewModel()
        val shareHashtags by shareVm.hashtags.collectAsStateWithLifecycle()
        ImageGalleryScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            shareHashtags = shareHashtags,
            onToggleShareHashtag = shareVm::onToggle,
            onAddShareHashtag = shareVm::onAdd,
            onRemoveShareHashtag = shareVm::onRemove,
        )
    }
}

internal fun EntryProviderScope<Any>.compareEntry(backStack: MutableList<Any>) {
    entry<CompareRoute> { key ->
        val leftVm: ModelDetailViewModel = koinViewModel(
            key = "compare_left_${key.leftModelId}",
        ) { parametersOf(key.leftModelId) }
        val rightVm: ModelDetailViewModel = koinViewModel(
            key = "compare_right_${key.rightModelId}",
        ) { parametersOf(key.rightModelId) }
        ModelCompareScreen(
            leftViewModel = leftVm,
            rightViewModel = rightVm,
            onBack = { backStack.removeLastOrNull() },
        )
    }
}

internal fun EntryProviderScope<Any>.feedEntry(backStack: MutableList<Any>) {
    entry<FeedRoute> {
        val viewModel: FeedViewModel = koinViewModel()
        FeedScreen(
            viewModel = viewModel,
            onBack = if (backStack.size > 1) {
                { backStack.removeLastOrNull() }
            } else {
                null
            },
            onModelClick = { modelId -> backStack.add(DetailRoute(modelId)) },
            onCreatorClick = { username -> backStack.add(CreatorRoute(username)) },
        )
    }
}

internal fun EntryProviderScope<Any>.analyticsEntry(backStack: MutableList<Any>) {
    entry<AnalyticsRoute> {
        val viewModel: AnalyticsViewModel = koinViewModel()
        AnalyticsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
}

internal fun EntryProviderScope<Any>.notificationCenterEntry(backStack: MutableList<Any>) {
    entry<NotificationCenterRoute> {
        val viewModel: NotificationCenterViewModel = koinViewModel()
        NotificationCenterScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onNavigateToModel = { modelId -> backStack.add(DetailRoute(modelId)) },
        )
    }
}

internal fun EntryProviderScope<Any>.browsingHistoryEntry(backStack: MutableList<Any>) {
    entry<BrowsingHistoryRoute> {
        val viewModel: BrowsingHistoryViewModel = koinViewModel()
        BrowsingHistoryScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onModelClick = { modelId ->
                backStack.add(DetailRoute(modelId))
            },
        )
    }
}

internal fun EntryProviderScope<Any>.downloadQueueEntry(backStack: MutableList<Any>) {
    entry<DownloadQueueRoute> {
        val viewModel: DownloadQueueViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        DownloadQueueScreen(
            uiState = uiState,
            onBack = { backStack.removeLastOrNull() },
            onPause = viewModel::pauseDownload,
            onResume = viewModel::resumeDownload,
            onCancel = viewModel::cancelDownload,
            onRetry = viewModel::retryDownload,
            onDelete = viewModel::deleteDownload,
            onClearCompleted = viewModel::clearCompleted,
        )
    }
}
