package com.riox432.civitdeck.ui.dataset

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.ExportProgress
import com.riox432.civitdeck.domain.model.ImageSource
import com.riox432.civitdeck.plugin.PluginExportFormat
import com.riox432.civitdeck.presentation.dataset.DatasetDetailViewModel
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.components.FilterChipRow
import com.riox432.civitdeck.ui.theme.Spacing

private const val GRID_COLUMNS = 2

internal data class DatasetGridCallbacks(
    val onToggleSelection: (Long) -> Unit,
    val onEnterSelectionMode: (Long) -> Unit,
    val onShowDetail: (DatasetImage) -> Unit,
    val onEditCaption: (DatasetImage) -> Unit,
    val onNavigateToBatchTagEditor: () -> Unit,
    val onSourceFilterChange: (ImageSource?) -> Unit,
)

private data class ResolutionFilterState(
    val show: Boolean,
    val minWidth: Int,
    val minHeight: Int,
    val onApply: (Int, Int) -> Unit,
    val onDismiss: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatasetDetailScreen(
    datasetName: String,
    viewModel: DatasetDetailViewModel,
    onBack: () -> Unit,
    onNavigateToBatchTagEditor: (datasetId: Long) -> Unit,
    onNavigateToDuplicateReview: (Long) -> Unit,
) {
    val filteredImages by viewModel.filteredImages.collectAsStateWithLifecycle()
    val selectedImageIds by viewModel.selectedImageIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedSource by viewModel.selectedSource.collectAsStateWithLifecycle()
    val detailImage by viewModel.detailImage.collectAsStateWithLifecycle()
    val duplicateCount by viewModel.duplicateCount.collectAsStateWithLifecycle()
    val lowResImages by viewModel.lowResImages.collectAsStateWithLifecycle()
    val resFilter = collectResolutionFilterState(viewModel)
    val showExportSheet by viewModel.showExportSheet.collectAsStateWithLifecycle()
    val exportProgress by viewModel.exportProgress.collectAsStateWithLifecycle()
    val allImages by viewModel.images.collectAsStateWithLifecycle()
    val availableExportFormats by viewModel.availableExportFormats.collectAsStateWithLifecycle()
    val selectedExportFormatId by viewModel.selectedExportFormatId.collectAsStateWithLifecycle()
    var captionSheetImageId by remember { mutableStateOf<Long?>(null) }
    var captionSheetInitial by remember { mutableStateOf("") }
    val callbacks = buildCallbacks(viewModel, onNavigateToBatchTagEditor) { id, text ->
        captionSheetImageId = id
        captionSheetInitial = text
    }
    DatasetDetailScaffold(
        datasetName = datasetName,
        viewModel = viewModel,
        filteredImages = filteredImages,
        selectedImageIds = selectedImageIds,
        isSelectionMode = isSelectionMode,
        selectedSource = selectedSource,
        callbacks = callbacks,
        duplicateCount = duplicateCount,
        lowResImages = lowResImages,
        resFilter = resFilter,
        captionSheetImageId = captionSheetImageId,
        captionSheetInitial = captionSheetInitial,
        detailImage = detailImage,
        onBack = onBack,
        onNavigateToBatchTagEditor = onNavigateToBatchTagEditor,
        onNavigateToDuplicateReview = onNavigateToDuplicateReview,
        onDismissCaption = { captionSheetImageId = null },
        showExportSheet = showExportSheet,
        exportProgress = exportProgress,
        allImages = allImages,
        availableExportFormats = availableExportFormats,
        selectedExportFormatId = selectedExportFormatId,
        onOpenExport = viewModel::openExportSheet,
        onDismissExport = viewModel::dismissExportSheet,
        onExportFormatSelected = viewModel::selectExportFormat,
        onStartExport = viewModel::startExport,
        onDismissExportResult = viewModel::dismissExportResult,
    )
}

@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatasetDetailScaffold(
    datasetName: String,
    viewModel: DatasetDetailViewModel,
    filteredImages: List<DatasetImage>,
    selectedImageIds: Set<Long>,
    isSelectionMode: Boolean,
    selectedSource: ImageSource?,
    callbacks: DatasetGridCallbacks,
    duplicateCount: Int,
    lowResImages: List<DatasetImage>,
    resFilter: ResolutionFilterState,
    captionSheetImageId: Long?,
    captionSheetInitial: String,
    detailImage: DatasetImage?,
    onBack: () -> Unit,
    onNavigateToBatchTagEditor: (Long) -> Unit,
    onNavigateToDuplicateReview: (Long) -> Unit,
    onDismissCaption: () -> Unit,
    showExportSheet: Boolean = false,
    exportProgress: ExportProgress? = null,
    allImages: List<DatasetImage> = emptyList(),
    availableExportFormats: List<PluginExportFormat> = emptyList(),
    selectedExportFormatId: String? = null,
    onOpenExport: () -> Unit = {},
    onDismissExport: () -> Unit = {},
    onExportFormatSelected: (String) -> Unit = {},
    onStartExport: (String) -> Unit = {},
    onDismissExportResult: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            DatasetDetailTopBar(
                datasetName = datasetName,
                isSelectionMode = isSelectionMode,
                selectedCount = selectedImageIds.size,
                onBack = onBack,
                onClearSelection = viewModel::clearSelection,
                onSelectAll = viewModel::selectAll,
                onReviewDuplicates = { onNavigateToDuplicateReview(viewModel.datasetId) },
                onResolutionFilter = viewModel::openResolutionFilter,
                onExport = onOpenExport,
            )
        },
        bottomBar = {
            if (isSelectionMode && selectedImageIds.isNotEmpty()) {
                DatasetSelectionBottomBar(
                    selectedCount = selectedImageIds.size,
                    onRemove = viewModel::removeSelected,
                    onEditTags = { onNavigateToBatchTagEditor(viewModel.datasetId) },
                )
            }
        },
    ) { padding ->
        DatasetDetailContent(
            images = filteredImages,
            isSelectionMode = isSelectionMode,
            selectedImageIds = selectedImageIds,
            selectedSource = selectedSource,
            callbacks = callbacks,
            duplicateCount = duplicateCount,
            lowResCount = lowResImages.size,
            modifier = Modifier.padding(padding),
        )
    }
    DatasetDetailSheets(
        captionSheetImageId = captionSheetImageId,
        captionSheetInitial = captionSheetInitial,
        detailImage = detailImage,
        resolutionFilterState = resFilter,
        onSaveCaption = { id, text -> viewModel.editCaption(id, text) },
        onDismissCaption = onDismissCaption,
        onTrainableToggle = { id, trainable -> viewModel.updateTrainable(id, trainable) },
        onDismissDetail = viewModel::dismissDetail,
    )
    if (showExportSheet) {
        val trainableCount = allImages.count { it.trainable && !it.excluded }
        val nonTrainableCount = allImages.size - trainableCount
        ExportDatasetSheet(
            imageCount = trainableCount,
            nonTrainableCount = nonTrainableCount,
            availableFormats = availableExportFormats,
            selectedFormatId = selectedExportFormatId,
            onFormatSelected = onExportFormatSelected,
            onExport = onStartExport,
            onDismiss = onDismissExport,
        )
    }
    exportProgress?.let { progress ->
        ExportProgressOverlay(
            progress = progress,
            onDismiss = onDismissExportResult,
        )
    }
}

@Composable
private fun collectResolutionFilterState(viewModel: DatasetDetailViewModel): ResolutionFilterState {
    val show by viewModel.showResolutionFilter.collectAsStateWithLifecycle()
    val minWidth by viewModel.minWidth.collectAsStateWithLifecycle()
    val minHeight by viewModel.minHeight.collectAsStateWithLifecycle()
    return ResolutionFilterState(
        show = show,
        minWidth = minWidth,
        minHeight = minHeight,
        onApply = viewModel::setResolutionFilter,
        onDismiss = viewModel::dismissResolutionFilter,
    )
}

@Composable
private fun buildCallbacks(
    viewModel: DatasetDetailViewModel,
    onNavigateToBatchTagEditor: (Long) -> Unit,
    onStartEditCaption: (Long, String) -> Unit,
): DatasetGridCallbacks = DatasetGridCallbacks(
    onToggleSelection = viewModel::toggleSelection,
    onEnterSelectionMode = viewModel::enterSelectionMode,
    onShowDetail = viewModel::showDetail,
    onEditCaption = { image -> onStartEditCaption(image.id, image.caption?.text.orEmpty()) },
    onNavigateToBatchTagEditor = { onNavigateToBatchTagEditor(viewModel.datasetId) },
    onSourceFilterChange = viewModel::setSourceFilter,
)

@Composable
private fun DatasetDetailSheets(
    captionSheetImageId: Long?,
    captionSheetInitial: String,
    detailImage: DatasetImage?,
    resolutionFilterState: ResolutionFilterState,
    onSaveCaption: (Long, String) -> Unit,
    onDismissCaption: () -> Unit,
    onTrainableToggle: (Long, Boolean) -> Unit,
    onDismissDetail: () -> Unit,
) {
    captionSheetImageId?.let { imageId ->
        CaptionEditorSheet(
            imageId = imageId,
            initialCaption = captionSheetInitial,
            onSave = onSaveCaption,
            onDismiss = onDismissCaption,
        )
    }
    detailImage?.let { image ->
        ImageDetailSheet(
            image = image,
            onTrainableToggle = { trainable -> onTrainableToggle(image.id, trainable) },
            onDismiss = onDismissDetail,
        )
    }
    if (resolutionFilterState.show) {
        ResolutionFilterSheet(
            initialMinWidth = resolutionFilterState.minWidth,
            initialMinHeight = resolutionFilterState.minHeight,
            onApply = { w, h ->
                resolutionFilterState.onApply(w, h)
                resolutionFilterState.onDismiss()
            },
            onDismiss = resolutionFilterState.onDismiss,
        )
    }
}

@Composable
private fun DatasetDetailContent(
    images: List<DatasetImage>,
    isSelectionMode: Boolean,
    selectedImageIds: Set<Long>,
    selectedSource: ImageSource?,
    callbacks: DatasetGridCallbacks,
    duplicateCount: Int,
    lowResCount: Int,
    modifier: Modifier = Modifier,
) {
    if (images.isEmpty() && selectedSource == null) {
        EmptyStateMessage(
            icon = Icons.Default.Dataset,
            title = stringResource(R.string.dataset_no_images_title),
            subtitle = stringResource(R.string.dataset_no_images_subtitle),
            modifier = modifier.fillMaxSize(),
        )
    } else {
        DatasetImageGrid(
            images = images,
            isSelectionMode = isSelectionMode,
            selectedImageIds = selectedImageIds,
            selectedSource = selectedSource,
            callbacks = callbacks,
            duplicateCount = duplicateCount,
            lowResCount = lowResCount,
            modifier = modifier,
        )
    }
}

@Composable
private fun DatasetImageGrid(
    images: List<DatasetImage>,
    isSelectionMode: Boolean,
    selectedImageIds: Set<Long>,
    selectedSource: ImageSource?,
    callbacks: DatasetGridCallbacks,
    duplicateCount: Int,
    lowResCount: Int,
    modifier: Modifier = Modifier,
) {
    val sourceOptions: List<ImageSource?> = listOf(null) + ImageSource.entries
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        contentPadding = PaddingValues(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.sm,
            bottom = Spacing.lg,
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            if (!isSelectionMode) {
                Column {
                    val allLabel = stringResource(R.string.dataset_source_all)
                    FilterChipRow(
                        options = sourceOptions,
                        selected = selectedSource,
                        onSelect = callbacks.onSourceFilterChange,
                        label = {
                            it?.name?.lowercase()
                                ?.replaceFirstChar { c -> c.uppercase() }
                                ?: allLabel
                        },
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.sm),
                    )
                    if (duplicateCount > 0 || lowResCount > 0) {
                        QualitySummaryChip(
                            imageCount = images.size,
                            duplicateCount = duplicateCount,
                            lowResCount = lowResCount,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        )
                    }
                }
            }
        }
        items(items = images, key = { it.id }) { image ->
            DatasetImageItem(
                image = image,
                isSelected = image.id in selectedImageIds,
                isSelectionMode = isSelectionMode,
                callbacks = callbacks,
                modifier = Modifier.animateItem(),
            )
        }
    }
}
