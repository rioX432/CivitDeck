@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.dataset

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import coil3.PlatformContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.ImageSource
import com.riox432.civitdeck.feature.collections.presentation.DatasetDetailViewModel
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.Elevation
import org.koin.compose.viewmodel.koinViewModel

private const val DEFAULT_GRID_COLUMNS = 4
private const val IMAGE_ASPECT_RATIO = 1f
private const val DATASET_IMAGE_SIZE = 300

@Composable
fun DesktopDatasetDetailScreen(
    datasetName: String,
    viewModel: DatasetDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredImages by viewModel.filteredImages.collectAsState()
    val selectedImageIds by viewModel.selectedImageIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val availableExportFormats by viewModel.availableExportFormats.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    var captionEditState by remember { mutableStateOf<Pair<Long, String>?>(null) }

    Surface(modifier = modifier.fillMaxSize()) {
        Column {
            DatasetDetailToolbar(
                datasetName = datasetName,
                isSelectionMode = isSelectionMode,
                selectedCount = selectedImageIds.size,
                onBack = onBack,
                onClearSelection = viewModel::clearSelection,
                onSelectAll = viewModel::selectAll,
                onExport = { showExportDialog = true },
            )
            SourceFilterRow(
                selectedSource = selectedSource,
                onSourceSelected = viewModel::setSourceFilter,
            )
            if (isSelectionMode && selectedImageIds.isNotEmpty()) {
                SelectionActionBar(
                    selectedCount = selectedImageIds.size,
                    onRemove = viewModel::removeSelected,
                )
            }
            DatasetImageGridContent(
                images = filteredImages,
                isSelectionMode = isSelectionMode,
                selectedImageIds = selectedImageIds,
                onImageClick = { image ->
                    if (isSelectionMode) {
                        viewModel.toggleSelection(image.id)
                    } else {
                        captionEditState = image.id to (image.caption?.text.orEmpty())
                    }
                },
                onImageLongClick = { imageId ->
                    if (!isSelectionMode) viewModel.enterSelectionMode(imageId)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showExportDialog) {
        DesktopExportDialog(
            formats = availableExportFormats,
            onExport = { formatId ->
                viewModel.startExport(formatId)
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false },
        )
    }

    captionEditState?.let { (imageId, initialCaption) ->
        CaptionEditDialog(
            initialCaption = initialCaption,
            onSave = { text ->
                viewModel.editCaption(imageId, text)
                captionEditState = null
            },
            onDismiss = { captionEditState = null },
        )
    }
}

@Composable
private fun DatasetDetailToolbar(
    datasetName: String,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onBack: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onExport: () -> Unit,
) {
    Surface(tonalElevation = Elevation.xs) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = if (isSelectionMode) onClearSelection else onBack,
            ) {
                Icon(
                    imageVector = if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isSelectionMode) "Cancel" else "Back",
                )
            }
            Text(
                text = if (isSelectionMode) "$selectedCount selected" else datasetName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = Spacing.sm),
            )
            if (isSelectionMode) {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                }
            } else {
                IconButton(onClick = onExport) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export")
                }
            }
        }
    }
}

@Composable
private fun SourceFilterRow(
    selectedSource: ImageSource?,
    onSourceSelected: (ImageSource?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        FilterChip(
            selected = selectedSource == null,
            onClick = { onSourceSelected(null) },
            label = { Text("All") },
        )
        ImageSource.entries.forEach { source ->
            FilterChip(
                selected = selectedSource == source,
                onClick = { onSourceSelected(source) },
                label = { Text(source.name.lowercase().replaceFirstChar { it.uppercase() }) },
            )
        }
    }
}

@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Button(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Remove selected", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text("Remove $selectedCount ${if (selectedCount == 1) "image" else "images"}")
        }
    }
}

@Composable
private fun DatasetImageGridContent(
    images: List<DatasetImage>,
    isSelectionMode: Boolean,
    selectedImageIds: Set<Long>,
    onImageClick: (DatasetImage) -> Unit,
    onImageLongClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayViewModel: DisplaySettingsViewModel = koinViewModel()
    val displayState by displayViewModel.uiState.collectAsState()
    val gridColumns = if (displayState.gridColumns > 0) displayState.gridColumns else DEFAULT_GRID_COLUMNS

    if (images.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Dataset,
                    contentDescription = "Empty dataset",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "No images yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = modifier.fillMaxSize(),
        ) {
            items(items = images, key = { it.id }) { image ->
                DesktopDatasetImageItem(
                    image = image,
                    isSelected = image.id in selectedImageIds,
                    isSelectionMode = isSelectionMode,
                    onClick = { onImageClick(image) },
                    onLongClick = { onImageLongClick(image.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopDatasetImageItem(
    image: DatasetImage,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(PlatformContext.INSTANCE)
                .data(image.imageUrl)
                .size(Size(DATASET_IMAGE_SIZE, DATASET_IMAGE_SIZE))
                .build(),
            contentDescription = "Dataset image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(IMAGE_ASPECT_RATIO)
                .clip(RoundedCornerShape(CornerRadius.image)),
        )
        if (isSelectionMode) {
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
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        } else {
            SourceBadge(
                sourceType = image.sourceType,
                modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.xs),
            )
            if (image.excluded) {
                Text(
                    text = "Flagged",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.xs)
                        .clip(RoundedCornerShape(CornerRadius.chip))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
                        .padding(horizontal = Spacing.xs, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun SourceBadge(sourceType: ImageSource, modifier: Modifier = Modifier) {
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
private fun DesktopExportDialog(
    formats: List<com.riox432.civitdeck.plugin.PluginExportFormat>,
    onExport: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedFormatId by remember { mutableStateOf(formats.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Dataset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (formats.size <= 1) {
                    Text("Format: ${formats.firstOrNull()?.name ?: "ZIP (kohya-ss compatible)"}")
                } else {
                    Text("Format", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        formats.forEach { format ->
                            FilterChip(
                                selected = format.id == selectedFormatId,
                                onClick = { selectedFormatId = format.id },
                                label = { Text(format.name) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedFormatId?.let { onExport(it) } },
                enabled = selectedFormatId != null,
            ) { Text("Export") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun CaptionEditDialog(
    initialCaption: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var caption by remember { mutableStateOf(initialCaption) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Caption") },
        text = {
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(caption) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
