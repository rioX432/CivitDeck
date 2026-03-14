package com.riox432.civitdeck.ui.dataset

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToDatasetSheet(
    datasets: List<DatasetCollection>,
    onSelectDataset: (Long) -> Unit,
    onCreateAndSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var showCreateDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(bottom = Spacing.lg)) {
            Text(
                text = "Add to Dataset",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
            LazyColumn {
                items(items = datasets, key = { it.id }) { dataset ->
                    DatasetPickerRow(
                        dataset = dataset,
                        onClick = { onSelectDataset(dataset.id) },
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
            TextButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.padding(horizontal = Spacing.md),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create new dataset")
                Text("Create New Dataset", modifier = Modifier.padding(start = Spacing.sm))
            }
        }
    }

    if (showCreateDialog) {
        DatasetNameDialog(
            title = "New Dataset",
            confirmLabel = "Create",
            initialName = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                onCreateAndSelect(name)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun DatasetPickerRow(
    dataset: DatasetCollection,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, onClickLabel = "Select dataset")
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Dataset,
            contentDescription = "Dataset",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = Spacing.md),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dataset.name,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${dataset.imageCount} images",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
