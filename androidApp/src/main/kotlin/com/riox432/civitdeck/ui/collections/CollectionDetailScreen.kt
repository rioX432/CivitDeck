package com.riox432.civitdeck.ui.collections

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.riox432.civitdeck.domain.model.CollectionSortOrder
import com.riox432.civitdeck.domain.model.FavoriteModelSummary
import com.riox432.civitdeck.domain.model.ModelCollection
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.ui.components.ImageErrorPlaceholder
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.shimmer
import com.riox432.civitdeck.util.FormatUtils

@Suppress("LongParameterList")
@Composable
fun CollectionDetailScreen(
    collectionName: String,
    models: List<FavoriteModelSummary>,
    sortOrder: CollectionSortOrder,
    typeFilter: ModelType?,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    collections: List<ModelCollection>,
    collectionId: Long,
    onBack: () -> Unit,
    onModelClick: (Long) -> Unit,
    onSortChange: (CollectionSortOrder) -> Unit,
    onTypeFilterChange: (ModelType?) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onEnterSelectionMode: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRemoveSelected: () -> Unit,
    onMoveSelectedTo: (Long) -> Unit,
) {
    Scaffold(
        topBar = {
            DetailTopBar(
                collectionName = collectionName,
                isSelectionMode = isSelectionMode,
                selectedCount = selectedIds.size,
                onBack = onBack,
                onSelectAll = onSelectAll,
                onClearSelection = onClearSelection,
            )
        },
        bottomBar = {
            if (isSelectionMode && selectedIds.isNotEmpty()) {
                SelectionBottomBar(
                    collections = collections,
                    currentCollectionId = collectionId,
                    onRemove = onRemoveSelected,
                    onMoveTo = onMoveSelectedTo,
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SortFilterBar(
                sortOrder = sortOrder,
                typeFilter = typeFilter,
                onSortChange = onSortChange,
                onTypeFilterChange = onTypeFilterChange,
            )
            if (models.isEmpty()) {
                EmptyCollectionDetail()
            } else {
                ModelsGrid(
                    models = models,
                    isSelectionMode = isSelectionMode,
                    selectedIds = selectedIds,
                    onModelClick = onModelClick,
                    onToggleSelection = onToggleSelection,
                    onEnterSelectionMode = onEnterSelectionMode,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    collectionName: String,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onBack: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = if (isSelectionMode) "$selectedCount selected" else collectionName,
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
private fun SortFilterBar(
    sortOrder: CollectionSortOrder,
    typeFilter: ModelType?,
    onSortChange: (CollectionSortOrder) -> Unit,
    onTypeFilterChange: (ModelType?) -> Unit,
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box {
            FilterChip(
                selected = sortOrder != CollectionSortOrder.DateAdded,
                onClick = { showSortMenu = true },
                label = { Text(sortOrder.name) },
            )
            SortDropdown(showSortMenu, onSortChange) { showSortMenu = false }
        }
        Box {
            FilterChip(
                selected = typeFilter != null,
                onClick = { showTypeMenu = true },
                label = { Text(typeFilter?.name ?: "All Types") },
            )
            TypeDropdown(showTypeMenu, onTypeFilterChange) { showTypeMenu = false }
        }
    }
}

@Composable
private fun SortDropdown(
    expanded: Boolean,
    onSortChange: (CollectionSortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        CollectionSortOrder.entries.forEach { order ->
            DropdownMenuItem(
                text = { Text(order.name) },
                onClick = {
                    onSortChange(order)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun TypeDropdown(
    expanded: Boolean,
    onTypeFilterChange: (ModelType?) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("All Types") },
            onClick = {
                onTypeFilterChange(null)
                onDismiss()
            },
        )
        ModelType.entries.forEach { type ->
            DropdownMenuItem(
                text = { Text(type.name) },
                onClick = {
                    onTypeFilterChange(type)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun SelectionBottomBar(
    collections: List<ModelCollection>,
    currentCollectionId: Long,
    onRemove: () -> Unit,
    onMoveTo: (Long) -> Unit,
) {
    var showMoveMenu by remember { mutableStateOf(false) }

    BottomAppBar {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Box {
                IconButton(onClick = { showMoveMenu = true }) {
                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move")
                }
                DropdownMenu(
                    expanded = showMoveMenu,
                    onDismissRequest = { showMoveMenu = false },
                ) {
                    collections
                        .filter { it.id != currentCollectionId }
                        .forEach { target ->
                            DropdownMenuItem(
                                text = { Text(target.name) },
                                onClick = {
                                    showMoveMenu = false
                                    onMoveTo(target.id)
                                },
                            )
                        }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
    }
}

@Composable
private fun EmptyCollectionDetail() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No models in this collection",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Add models from the detail screen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModelsGrid(
    models: List<FavoriteModelSummary>,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onModelClick: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onEnterSelectionMode: (Long) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.sm,
            bottom = Spacing.lg,
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(items = models, key = { it.id }) { model ->
            ModelCardItem(
                model = model,
                isSelected = model.id in selectedIds,
                isSelectionMode = isSelectionMode,
                onClick = {
                    if (isSelectionMode) onToggleSelection(model.id) else onModelClick(model.id)
                },
                onLongClick = {
                    if (!isSelectionMode) onEnterSelectionMode(model.id)
                },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModelCardItem(
    model: FavoriteModelSummary,
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
        ModelCard(model = model)
        if (isSelectionMode) {
            SelectionOverlay(isSelected = isSelected)
        }
    }
}

@Composable
private fun SelectionOverlay(isSelected: Boolean) {
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

@Composable
private fun ModelCard(
    model: FavoriteModelSummary,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column {
            ModelThumbnail(model = model)
            ModelCardInfo(model = model)
        }
    }
}

@Composable
private fun ModelThumbnail(model: FavoriteModelSummary) {
    if (model.thumbnailUrl != null) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(
                androidx.compose.ui.platform.LocalContext.current,
            )
                .data(model.thumbnailUrl)
                .crossfade(Duration.normal)
                .build(),
            contentDescription = model.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .shimmer(),
                )
            },
            error = {
                ImageErrorPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
            },
        )
    }
}

@Composable
private fun ModelCardInfo(model: FavoriteModelSummary) {
    Column(
        modifier = Modifier.padding(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = model.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = model.type.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(CornerRadius.chip),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        StatsRow(model = model)
    }
}

@Composable
private fun StatsRow(model: FavoriteModelSummary) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatItem(FormatUtils.formatCount(model.downloadCount), Icons.Outlined.Download)
        StatItem(FormatUtils.formatCount(model.favoriteCount), Icons.Outlined.FavoriteBorder)
        StatItem(FormatUtils.formatRating(model.rating), Icons.Outlined.Star)
    }
}

@Composable
private fun StatItem(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(com.riox432.civitdeck.ui.theme.IconSize.statIcon),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
