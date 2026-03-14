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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun DesktopDatasetListScreen(
    viewModel: DesktopDatasetListViewModel,
    onDatasetClick: (Long, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val datasets by viewModel.datasets.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize()) {
        Column {
            DatasetListToolbar(onBack = onBack, onCreate = { showCreateDialog = true })
            DatasetListBody(
                datasets = datasets,
                onDatasetClick = onDatasetClick,
                onRename = viewModel::renameDataset,
                onDelete = viewModel::deleteDataset,
            )
        }
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
private fun DatasetListToolbar(
    onBack: () -> Unit,
    onCreate: () -> Unit,
) {
    Surface(tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Datasets",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = Spacing.sm),
            )
            Button(onClick = onCreate) {
                Icon(Icons.Default.Add, contentDescription = "Create new dataset")
                Text("New Dataset", modifier = Modifier.padding(start = Spacing.xs))
            }
        }
    }
}

@Composable
private fun DatasetListBody(
    datasets: List<DatasetCollection>,
    onDatasetClick: (Long, String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
) {
    if (datasets.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Dataset,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "No datasets yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Create a dataset to organize training images",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
        ) {
            items(items = datasets, key = { it.id }) { dataset ->
                DesktopDatasetCard(
                    dataset = dataset,
                    onClick = { onDatasetClick(dataset.id, dataset.name) },
                    onRename = { newName -> onRename(dataset.id, newName) },
                    onDelete = { onDelete(dataset.id) },
                )
            }
        }
    }
}

@Composable
private fun DesktopDatasetCard(
    dataset: DatasetCollection,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open dataset", onClick = onClick),
        shape = RoundedCornerShape(CornerRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
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
                    contentDescription = null,
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
            Box {
                Text(
                    text = "...",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable(
                        role = Role.Button,
                        onClickLabel = "Open menu",
                    ) { showMenu = true },
                )
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                    )
                }
            }
        }
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
private fun DatasetNameDialog(
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
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
