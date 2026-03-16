package com.riox432.civitdeck.ui.collections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.ModelCollection
import com.riox432.civitdeck.feature.prompts.presentation.SavedPromptsViewModel
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.prompts.SavedPromptsScreen
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

private enum class CollectionsScreenTab { Collections, Prompts, Datasets }

@Composable
fun CollectionsScreen(
    collections: List<ModelCollection>,
    onCollectionClick: (Long, String) -> Unit,
    onCreateCollection: (String) -> Unit,
    onRenameCollection: (Long, String) -> Unit,
    onDeleteCollection: (Long) -> Unit,
    promptsViewModel: SavedPromptsViewModel,
    onNavigateToDatasets: () -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableStateOf(CollectionsScreenTab.Collections) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            when (selectedTab) {
                CollectionsScreenTab.Collections -> {
                    FloatingActionButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_create_collection))
                    }
                }
                CollectionsScreenTab.Datasets -> {
                    FloatingActionButton(onClick = onNavigateToDatasets) {
                        Icon(Icons.Default.Dataset, contentDescription = stringResource(R.string.cd_manage_datasets))
                    }
                }
                else -> {}
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            CollectionsTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
            when (selectedTab) {
                CollectionsScreenTab.Collections -> CollectionsContent(
                    collections = collections,
                    onCollectionClick = onCollectionClick,
                    onRenameCollection = onRenameCollection,
                    onDeleteCollection = onDeleteCollection,
                )
                CollectionsScreenTab.Prompts -> SavedPromptsScreen(viewModel = promptsViewModel)
                CollectionsScreenTab.Datasets -> DatasetsEntryContent()
            }
        }
        if (showCreateDialog) {
            CreateCollectionDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { name ->
                    onCreateCollection(name)
                    showCreateDialog = false
                },
            )
        }
    }
}

@Composable
private fun CollectionsTabRow(
    selectedTab: CollectionsScreenTab,
    onTabSelected: (CollectionsScreenTab) -> Unit,
) {
    TabRow(selectedTabIndex = selectedTab.ordinal) {
        CollectionsScreenTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = { Text(tab.name) },
            )
        }
    }
}

@Composable
private fun DatasetsEntryContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        EmptyStateMessage(
            icon = Icons.Default.Dataset,
            title = "Training Datasets",
            subtitle = "Curate image sets for AI model training",
        )
    }
}

@Composable
private fun CollectionsContent(
    collections: List<ModelCollection>,
    onCollectionClick: (Long, String) -> Unit,
    onRenameCollection: (Long, String) -> Unit,
    onDeleteCollection: (Long) -> Unit,
) {
    if (collections.isEmpty()) {
        EmptyStateMessage(
            icon = Icons.Default.Folder,
            title = "No collections yet",
            subtitle = "Create a collection to organize your models",
        )
    } else {
        CollectionsList(
            collections = collections,
            onCollectionClick = onCollectionClick,
            onRenameCollection = onRenameCollection,
            onDeleteCollection = onDeleteCollection,
        )
    }
}

@Composable
private fun CollectionsList(
    collections: List<ModelCollection>,
    onCollectionClick: (Long, String) -> Unit,
    onRenameCollection: (Long, String) -> Unit,
    onDeleteCollection: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
    ) {
        items(items = collections, key = { it.id }) { collection ->
            CollectionCard(
                collection = collection,
                onClick = { onCollectionClick(collection.id, collection.name) },
                onRename = { newName -> onRenameCollection(collection.id, newName) },
                onDelete = { onDeleteCollection(collection.id) },
            )
        }
    }
}

@Composable
private fun CollectionCard(
    collection: ModelCollection,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var showRenameDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.cd_open_collection), onClick = onClick),
        shape = RoundedCornerShape(CornerRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        CollectionCardContent(
            collection = collection,
            onShowRename = { showRenameDialog = true },
            onDelete = onDelete,
        )
    }

    if (showRenameDialog) {
        RenameCollectionDialog(
            currentName = collection.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { name ->
                onRename(name)
                showRenameDialog = false
            },
        )
    }
}

@Composable
private fun CollectionCardContent(
    collection: ModelCollection,
    onShowRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CollectionThumbnail(thumbnailUrl = collection.thumbnailUrl)
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = collection.name,
                style = MaterialTheme.typography.titleMedium,
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
            CollectionOverflowMenu(
                showMenu = showMenu,
                onToggleMenu = { showMenu = it },
                onRename = onShowRename,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun CollectionOverflowMenu(
    showMenu: Boolean,
    onToggleMenu: (Boolean) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Box {
        Text(
            text = "...",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Open menu") { onToggleMenu(true) },
        )
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { onToggleMenu(false) },
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_rename)) },
                onClick = {
                    onToggleMenu(false)
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete)) },
                onClick = {
                    onToggleMenu(false)
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun CollectionThumbnail(thumbnailUrl: String?) {
    val thumbnailSize = 56.dp
    if (thumbnailUrl != null) {
        CivitAsyncImage(
            imageUrl = thumbnailUrl,
            contentDescription = stringResource(R.string.cd_collection_thumbnail),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(thumbnailSize)
                .clip(RoundedCornerShape(CornerRadius.image)),
        )
    } else {
        Box(
            modifier = Modifier
                .size(thumbnailSize)
                .clip(RoundedCornerShape(CornerRadius.image))
                .then(
                    Modifier.aspectRatio(1f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = stringResource(R.string.cd_collection_folder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun CreateCollectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Collection") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Collection name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
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

@Composable
private fun RenameCollectionDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Collection") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Collection name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
