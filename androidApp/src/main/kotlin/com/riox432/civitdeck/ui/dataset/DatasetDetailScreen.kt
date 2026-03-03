@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.dataset

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

private const val GRID_COLUMNS = 2
private const val IMAGE_ASPECT_RATIO = 1f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatasetDetailScreen(
    datasetName: String,
    viewModel: DatasetDetailViewModel,
    onBack: () -> Unit,
) {
    val images by viewModel.images.collectAsStateWithLifecycle()
    val selectedImageIds by viewModel.selectedImageIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            DatasetDetailTopBar(
                datasetName = datasetName,
                isSelectionMode = isSelectionMode,
                selectedCount = selectedImageIds.size,
                onBack = onBack,
                onClearSelection = viewModel::clearSelection,
                onSelectAll = viewModel::selectAll,
            )
        },
        bottomBar = {
            if (isSelectionMode && selectedImageIds.isNotEmpty()) {
                DatasetSelectionBottomBar(
                    selectedCount = selectedImageIds.size,
                    onRemove = viewModel::removeSelected,
                )
            }
        },
    ) { padding ->
        DatasetDetailContent(
            images = images,
            isSelectionMode = isSelectionMode,
            selectedImageIds = selectedImageIds,
            onToggleSelection = viewModel::toggleSelection,
            onEnterSelectionMode = viewModel::enterSelectionMode,
            modifier = Modifier.padding(padding),
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
            }
        },
    )
}

@Composable
private fun DatasetSelectionBottomBar(
    selectedCount: Int,
    onRemove: () -> Unit,
) {
    BottomAppBar {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Button(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
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
    onToggleSelection: (Long) -> Unit,
    onEnterSelectionMode: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (images.isEmpty()) {
        EmptyStateMessage(
            icon = Icons.Default.Dataset,
            title = "No images yet",
            subtitle = "Add images to this dataset from the ComfyUI output gallery",
            modifier = modifier.fillMaxSize(),
        )
    } else {
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
            items(items = images, key = { it.id }) { image ->
                DatasetImageItem(
                    image = image,
                    isSelected = image.id in selectedImageIds,
                    isSelectionMode = isSelectionMode,
                    onClick = {
                        if (isSelectionMode) onToggleSelection(image.id)
                    },
                    onLongClick = {
                        if (!isSelectionMode) onEnterSelectionMode(image.id)
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DatasetImageItem(
    image: DatasetImage,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
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
        }
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
