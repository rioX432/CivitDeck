@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.dataset

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.ExportProgress
import com.riox432.civitdeck.domain.model.ImageSource
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.components.FilterChipRow
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

private const val GRID_COLUMNS = 2
private const val IMAGE_ASPECT_RATIO = 1f

private data class DatasetGridCallbacks(
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
        onOpenExport = viewModel::openExportSheet,
        onDismissExport = viewModel::dismissExportSheet,
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
    onOpenExport: () -> Unit = {},
    onDismissExport: () -> Unit = {},
    onStartExport: () -> Unit = {},
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatasetDetailTopBar(
    datasetName: String,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onBack: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onReviewDuplicates: () -> Unit,
    onResolutionFilter: () -> Unit,
    onExport: () -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = if (isSelectionMode) "$selectedCount selected" else datasetName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = if (isSelectionMode) onClearSelection else onBack,
            ) {
                Icon(
                    imageVector = if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isSelectionMode) "Cancel selection" else "Back",
                )
            }
        },
        actions = {
            if (isSelectionMode) {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                }
            } else {
                IconButton(onClick = onExport) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export dataset")
                }
                IconButton(onClick = onReviewDuplicates) {
                    Icon(Icons.Default.FindReplace, contentDescription = "Review duplicates")
                }
                IconButton(onClick = onResolutionFilter) {
                    Icon(Icons.Default.PhotoSizeSelectLarge, contentDescription = "Resolution filter")
                }
            }
        },
    )
}

@Composable
private fun DatasetSelectionBottomBar(
    selectedCount: Int,
    onRemove: () -> Unit,
    onEditTags: () -> Unit,
) {
    BottomAppBar {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        ) {
            Button(onClick = onEditTags) {
                Icon(
                    imageVector = Icons.Default.Style,
                    contentDescription = "Edit tags",
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Edit Tags",
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
            Button(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Remove $selectedCount ${if (selectedCount == 1) "image" else "images"}",
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
        }
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
            title = "No images yet",
            subtitle = "Add images to this dataset from the ComfyUI output gallery",
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
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            if (!isSelectionMode) {
                androidx.compose.foundation.layout.Column {
                    FilterChipRow(
                        options = sourceOptions,
                        selected = selectedSource,
                        onSelect = callbacks.onSourceFilterChange,
                        label = { it?.name?.lowercase()?.replaceFirstChar { c -> c.uppercase() } ?: "All" },
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

@Composable
private fun QualitySummaryChip(
    imageCount: Int,
    duplicateCount: Int,
    lowResCount: Int,
    modifier: Modifier = Modifier,
) {
    val label = buildString {
        append("$imageCount images")
        if (duplicateCount > 0) append(" • $duplicateCount duplicates")
        if (lowResCount > 0) append(" • $lowResCount below threshold")
    }
    SuggestionChip(
        onClick = {},
        label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DatasetImageItem(
    image: DatasetImage,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    callbacks: DatasetGridCallbacks,
    modifier: Modifier = Modifier,
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.combinedClickable(
            onClick = {
                if (isSelectionMode) callbacks.onToggleSelection(image.id) else callbacks.onShowDetail(image)
            },
            onLongClick = { if (!isSelectionMode) showContextMenu = true },
        ),
    ) {
        CivitAsyncImage(
            imageUrl = image.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(IMAGE_ASPECT_RATIO)
                .clip(RoundedCornerShape(CornerRadius.image)),
        )
        if (isSelectionMode) {
            DatasetImageSelectionOverlay(isSelected = isSelected)
        } else {
            SourceBadgeMini(
                sourceType = image.sourceType,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.xs),
            )
            if (image.excluded) {
                FlaggedBadge(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.xs),
                )
            }
        }
        ImageContextMenu(
            expanded = showContextMenu,
            onDismiss = { showContextMenu = false },
            onEditCaption = {
                showContextMenu = false
                callbacks.onEditCaption(image)
            },
            onBatchEditTags = {
                showContextMenu = false
                callbacks.onNavigateToBatchTagEditor()
            },
            onSelect = {
                showContextMenu = false
                callbacks.onEnterSelectionMode(image.id)
            },
        )
    }
}

@Composable
private fun FlaggedBadge(modifier: Modifier = Modifier) {
    Text(
        text = "Flagged",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onError,
        modifier = modifier
            .clip(RoundedCornerShape(CornerRadius.chip))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
            .padding(horizontal = Spacing.xs, vertical = 2.dp),
    )
}

@Composable
private fun SourceBadgeMini(sourceType: ImageSource, modifier: Modifier = Modifier) {
    val (label, color) = when (sourceType) {
        ImageSource.CIVITAI -> "CI" to MaterialTheme.colorScheme.primaryContainer
        ImageSource.LOCAL -> "LO" to MaterialTheme.colorScheme.secondaryContainer
        ImageSource.GENERATED -> "GN" to MaterialTheme.colorScheme.tertiaryContainer
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .clip(RoundedCornerShape(CornerRadius.chip))
            .background(color.copy(alpha = 0.85f))
            .padding(horizontal = Spacing.xs, vertical = 2.dp),
    )
}

@Composable
private fun ImageContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEditCaption: () -> Unit,
    onBatchEditTags: () -> Unit,
    onSelect: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        DropdownMenuItem(
            text = { Text("Edit caption") },
            onClick = onEditCaption,
        )
        DropdownMenuItem(
            text = { Text("Batch edit tags") },
            onClick = onBatchEditTags,
        )
        DropdownMenuItem(
            text = { Text("Select") },
            onClick = onSelect,
        )
    }
}

@Composable
private fun DatasetImageSelectionOverlay(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .padding(Spacing.sm)
            .size(24.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
