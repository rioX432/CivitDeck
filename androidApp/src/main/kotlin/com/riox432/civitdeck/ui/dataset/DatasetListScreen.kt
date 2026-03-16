package com.riox432.civitdeck.ui.dataset

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatasetListScreen(
    viewModel: DatasetListViewModel,
    onDatasetClick: (Long, String) -> Unit,
    onBack: () -> Unit,
) {
    val datasets by viewModel.datasets.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Datasets") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_create_dataset))
            }
        },
    ) { padding ->
        DatasetListContent(
            datasets = datasets,
            onDatasetClick = onDatasetClick,
            onRenameDataset = viewModel::renameDataset,
            onDeleteDataset = viewModel::deleteDataset,
            modifier = Modifier.padding(padding),
        )
    }

    if (showCreateDialog) {
        DatasetNameDialog(
            title = "New Dataset",
            confirmLabel = "Create",
            initialName = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createDataset(name)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun DatasetListContent(
    datasets: List<DatasetCollection>,
    onDatasetClick: (Long, String) -> Unit,
    onRenameDataset: (Long, String) -> Unit,
    onDeleteDataset: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (datasets.isEmpty()) {
        EmptyStateMessage(
            icon = Icons.Default.Dataset,
            title = "No datasets yet",
            subtitle = "Create a dataset to organize training images",
            modifier = modifier.fillMaxSize(),
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
        ) {
            items(items = datasets, key = { it.id }) { dataset ->
                DatasetCard(
                    dataset = dataset,
                    onClick = { onDatasetClick(dataset.id, dataset.name) },
                    onRename = { newName -> onRenameDataset(dataset.id, newName) },
                    onDelete = { onDeleteDataset(dataset.id) },
                )
            }
        }
    }
}

@Composable
private fun DatasetCard(
    dataset: DatasetCollection,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var showRenameDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open dataset", onClick = onClick),
        shape = RoundedCornerShape(CornerRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        DatasetCardContent(
            dataset = dataset,
            onShowRename = { showRenameDialog = true },
            onDelete = onDelete,
        )
    }

    if (showRenameDialog) {
        DatasetNameDialog(
            title = "Rename Dataset",
            confirmLabel = "Rename",
            initialName = dataset.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { name ->
                onRename(name)
                showRenameDialog = false
            },
        )
    }
}

@Composable
private fun DatasetCardContent(
    dataset: DatasetCollection,
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
        BadgedBox(
            badge = {
                if (dataset.imageCount > 0) {
                    Badge { Text(dataset.imageCount.toString()) }
                }
            },
        ) {
            Icon(
                imageVector = Icons.Default.Dataset,
                contentDescription = stringResource(R.string.cd_dataset),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dataset.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${dataset.imageCount} images",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DatasetOverflowMenu(
            showMenu = showMenu,
            onToggleMenu = { showMenu = it },
            onRename = onShowRename,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun DatasetOverflowMenu(
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
internal fun DatasetNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Dataset name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
