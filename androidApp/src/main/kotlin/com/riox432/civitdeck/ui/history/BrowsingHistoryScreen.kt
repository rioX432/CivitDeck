package com.riox432.civitdeck.ui.history

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.RecentlyViewedModel
import com.riox432.civitdeck.feature.search.presentation.BrowsingHistoryUiState
import com.riox432.civitdeck.feature.search.presentation.BrowsingHistoryViewModel
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.theme.IconSize
import com.riox432.civitdeck.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowsingHistoryScreen(
    viewModel: BrowsingHistoryViewModel,
    onBack: () -> Unit,
    onModelClick: (Long) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            HistoryTopAppBar(
                isEmpty = state.isEmpty,
                onBack = onBack,
                onClear = { showClearDialog = true },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        HistoryContent(
            state = state,
            onModelClick = onModelClick,
            onDelete = { historyId ->
                pendingDeleteId = historyId
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result != SnackbarResult.ActionPerformed && pendingDeleteId == historyId) {
                        viewModel.deleteItem(historyId)
                    }
                    pendingDeleteId = null
                }
            },
            pendingDeleteId = pendingDeleteId,
            modifier = Modifier.padding(padding),
        )
    }

    if (showClearDialog) {
        ClearHistoryDialog(
            onConfirm = {
                viewModel.clearAll()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTopAppBar(
    isEmpty: Boolean,
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    TopAppBar(
        title = { Text("Browsing History") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_navigate_back)
                )
            }
        },
        actions = {
            if (!isEmpty) {
                TextButton(onClick = onClear) { Text("Clear All") }
            }
        },
    )
}

@Composable
private fun HistoryContent(
    state: BrowsingHistoryUiState,
    onModelClick: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    pendingDeleteId: Long? = null,
    modifier: Modifier = Modifier,
) {
    if (state.isEmpty) {
        EmptyStateMessage(
            icon = Icons.Default.History,
            title = "No browsing history",
            modifier = modifier.fillMaxSize(),
        )
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            state.groups.forEach { group ->
                stickyHeader(key = group.label) {
                    GroupHeader(group.label)
                }
                val visibleItems = group.items.filter { it.historyId != pendingDeleteId }
                items(items = visibleItems, key = { it.historyId }) { item ->
                    HistoryItem(
                        item = item,
                        onClick = { onModelClick(item.modelId) },
                        onDelete = { onDelete(item.historyId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    )
}

@Composable
private fun ClearHistoryDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear All History") },
        text = { Text("Are you sure? This cannot be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Clear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryItem(
    item: RecentlyViewedModel,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeDismissBackground(dismissState.targetValue) },
        enableDismissFromStartToEnd = false,
    ) {
        HistoryItemContent(item = item, onClick = onClick)
    }
}

@Composable
private fun SwipeDismissBackground(targetValue: SwipeToDismissBoxValue) {
    val color by animateColorAsState(
        targetValue = if (targetValue == SwipeToDismissBoxValue.EndToStart) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "swipeBg",
    )
    Box(
        modifier = Modifier.fillMaxSize().background(color).padding(horizontal = Spacing.lg),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun HistoryItemContent(item: RecentlyViewedModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivitAsyncImage(
            imageUrl = item.thumbnailUrl,
            contentDescription = item.modelName,
            modifier = Modifier.size(
                IconSize.xlarge
            ).clip(MaterialTheme.shapes.small), // TODO: Unify with shared design token
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.modelName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    item.modelType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                item.creatorName?.let { creator ->
                    Text(
                        creator,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Text(
            formatRelativeTime(item.viewedAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> "${days / 7}w"
    }
}
