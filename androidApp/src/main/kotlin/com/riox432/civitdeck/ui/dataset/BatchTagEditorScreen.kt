@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.dataset

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

private const val BATCH_GRID_COLUMNS = 3
private const val IMAGE_ASPECT = 1f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchTagEditorScreen(
    viewModel: BatchTagEditorViewModel,
    onBack: () -> Unit,
) {
    val images by viewModel.images.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedImageIds.collectAsStateWithLifecycle()
    val tagInput by viewModel.tagInput.collectAsStateWithLifecycle()
    val suggestions by viewModel.tagSuggestions.collectAsStateWithLifecycle()
    val isAddMode by viewModel.isAddMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            BatchTagEditorTopBar(
                selectedCount = selectedIds.size,
                isAddMode = isAddMode,
                onBack = onBack,
                onSelectAll = viewModel::selectAll,
                onToggleMode = viewModel::toggleMode,
            )
        },
    ) { padding ->
        BatchTagEditorBody(
            images = images,
            selectedIds = selectedIds,
            tagInput = tagInput,
            suggestions = suggestions,
            isAddMode = isAddMode,
            onToggleSelection = viewModel::toggleSelection,
            onTagInputChange = viewModel::setTagInput,
            onApplyTag = { tag ->
                viewModel.applyTags(listOf(tag))
                viewModel.setTagInput("")
            },
            modifier = Modifier.padding(padding),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchTagEditorTopBar(
    selectedCount: Int,
    isAddMode: Boolean,
    onBack: () -> Unit,
    onSelectAll: () -> Unit,
    onToggleMode: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text("$selectedCount selected")
                FilterChip(
                    selected = isAddMode,
                    onClick = onToggleMode,
                    label = { Text(if (isAddMode) "Add Tags" else "Remove Tags") },
                    leadingIcon = if (isAddMode) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else {
                        null
                    },
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select all")
            }
        },
    )
}

@Composable
private fun BatchTagEditorBody(
    images: List<DatasetImage>,
    selectedIds: Set<Long>,
    tagInput: String,
    suggestions: List<String>,
    isAddMode: Boolean,
    onToggleSelection: (Long) -> Unit,
    onTagInputChange: (String) -> Unit,
    onApplyTag: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TagInputSection(
            tagInput = tagInput,
            suggestions = suggestions,
            selectedCount = selectedIds.size,
            isAddMode = isAddMode,
            onTagInputChange = onTagInputChange,
            onApplyTag = onApplyTag,
        )
        BatchImageGrid(
            images = images,
            selectedIds = selectedIds,
            onToggleSelection = onToggleSelection,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagInputSection(
    tagInput: String,
    suggestions: List<String>,
    selectedCount: Int,
    isAddMode: Boolean,
    onTagInputChange: (String) -> Unit,
    onApplyTag: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .padding(top = Spacing.sm),
    ) {
        TagInputRow(
            tagInput = tagInput,
            selectedCount = selectedCount,
            isAddMode = isAddMode,
            onTagInputChange = onTagInputChange,
            onApplyTag = onApplyTag,
        )
        if (suggestions.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                suggestions.forEach { tag ->
                    AssistChip(
                        onClick = { onApplyTag(tag) },
                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TagInputRow(
    tagInput: String,
    selectedCount: Int,
    isAddMode: Boolean,
    onTagInputChange: (String) -> Unit,
    onApplyTag: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = tagInput,
            onValueChange = onTagInputChange,
            modifier = Modifier.weight(1f),
            label = { Text(if (isAddMode) "Tag to add" else "Tag to remove") },
            singleLine = true,
        )
        val canApply = selectedCount > 0 && tagInput.isNotBlank()
        TextButton(
            onClick = { onApplyTag(tagInput.trim()) },
            enabled = canApply,
        ) {
            Text("Apply")
        }
    }
}

@Composable
private fun BatchImageGrid(
    images: List<DatasetImage>,
    selectedIds: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (images.isEmpty()) {
        EmptyStateMessage(
            icon = Icons.Default.PhotoLibrary,
            title = "No images",
            subtitle = "Add images to this dataset to start tagging",
            modifier = modifier.fillMaxWidth(),
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(BATCH_GRID_COLUMNS),
            contentPadding = PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = Spacing.sm,
                bottom = Spacing.lg,
            ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = modifier.fillMaxWidth(),
        ) {
            items(items = images, key = { it.id }) { image ->
                BatchImageItem(
                    image = image,
                    isSelected = image.id in selectedIds,
                    onToggle = { onToggleSelection(image.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BatchImageItem(
    image: DatasetImage,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.combinedClickable(onClick = onToggle),
    ) {
        CivitAsyncImage(
            imageUrl = image.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(IMAGE_ASPECT)
                .clip(RoundedCornerShape(CornerRadius.image)),
        )
        BatchSelectionOverlay(isSelected = isSelected)
    }
}

@Composable
private fun BatchSelectionOverlay(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .padding(Spacing.xs)
            .size(22.dp)
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
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
