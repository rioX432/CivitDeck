package com.riox432.civitdeck.ui.collections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.riox432.civitdeck.domain.model.ModelCollection
import com.riox432.civitdeck.feature.collections.presentation.CollectionsViewModel
import com.riox432.civitdeck.ui.desktopFocusRing
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.Elevation

@Composable
fun DesktopCollectionsScreen(
    viewModel: CollectionsViewModel,
    onCollectionClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val collections by viewModel.collections.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        CollectionsTopBar(onCreateClick = { showCreateDialog = true })
        CollectionsList(
            collections = collections,
            onCollectionClick = onCollectionClick,
            onDeleteClick = viewModel::deleteCollection,
        )
    }

    if (showCreateDialog) {
        CreateCollectionDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createCollection(name)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun CollectionsTopBar(onCreateClick: () -> Unit) {
    Surface(tonalElevation = Elevation.xs) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Collections",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = Spacing.sm),
            )
            IconButton(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = "Create collection")
            }
        }
    }
}

@Composable
private fun CollectionsList(
    collections: List<ModelCollection>,
    onCollectionClick: (Long, String) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    if (collections.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.FolderCopy,
                    contentDescription = "No collections",
                    modifier = Modifier.size(EMPTY_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = "No collections yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(items = collections, key = { it.id }) { collection ->
            CollectionCard(
                collection = collection,
                onClick = { onCollectionClick(collection.id, collection.name) },
                onDeleteClick = { onDeleteClick(collection.id) },
            )
        }
    }
}

@Composable
private fun CollectionCard(
    collection: ModelCollection,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .desktopFocusRing()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CornerRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.xs),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (collection.thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(PlatformContext.INSTANCE)
                        .data(collection.thumbnailUrl)
                        .size(Size(COLLECTION_THUMBNAIL_PX, COLLECTION_THUMBNAIL_PX))
                        .build(),
                    contentDescription = collection.name,
                    modifier = Modifier
                        .size(THUMBNAIL_SIZE)
                        .clip(RoundedCornerShape(CornerRadius.chip)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(Spacing.md))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${collection.modelCount} models",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!collection.isDefault) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateCollectionDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Collection") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private val THUMBNAIL_SIZE = 56.dp
private val EMPTY_ICON_SIZE = 48.dp
private const val COLLECTION_THUMBNAIL_PX = 112
