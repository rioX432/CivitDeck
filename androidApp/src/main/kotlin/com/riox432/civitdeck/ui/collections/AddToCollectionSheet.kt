package com.riox432.civitdeck.ui.collections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import com.riox432.civitdeck.domain.model.ModelCollection
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToCollectionSheet(
    collections: List<ModelCollection>,
    modelCollectionIds: List<Long>,
    onToggleCollection: (Long) -> Unit,
    onCreateCollection: (String) -> Unit,
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
                text = "Add to Collection",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
            LazyColumn {
                items(items = collections, key = { it.id }) { collection ->
                    val isInCollection = collection.id in modelCollectionIds
                    CollectionRow(
                        name = collection.name,
                        isChecked = isInCollection,
                        onClick = { onToggleCollection(collection.id) },
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
            TextButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.padding(horizontal = Spacing.md),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Create New Collection", modifier = Modifier.padding(start = Spacing.sm))
            }
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

@Composable
private fun CollectionRow(
    name: String,
    isChecked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (isChecked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "In collection",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
