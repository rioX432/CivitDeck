package com.riox432.civitdeck.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.riox432.civitdeck.domain.model.SavedSearchFilter
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedFiltersSheet(
    savedFilters: List<SavedSearchFilter>,
    onApply: (SavedSearchFilter) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = Spacing.lg),
        ) {
            item {
                Text(
                    text = "Saved Filters",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
            }
            if (savedFilters.isEmpty()) {
                item {
                    Text(
                        text = "No saved filters yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.lg),
                    )
                }
            } else {
                items(savedFilters, key = { it.id }) { filter ->
                    SavedFilterRow(
                        filter = filter,
                        onApply = {
                            onApply(filter)
                            onDismiss()
                        },
                        onDelete = { onDelete(filter.id) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
                }
            }
        }
    }
}

@Composable
private fun SavedFilterRow(
    filter: SavedSearchFilter,
    onApply: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onApply)
            .padding(start = Spacing.lg, top = Spacing.sm, bottom = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = Spacing.sm)) {
            Text(
                text = filter.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = filterSummary(filter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete filter",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun filterSummary(filter: SavedSearchFilter): String {
    val parts = buildList {
        filter.selectedType?.let { add(it.name) }
        add(filter.selectedSort.name)
        if (filter.selectedBaseModels.isNotEmpty()) {
            add(filter.selectedBaseModels.joinToString(", ") { it.displayName })
        }
        if (filter.query.isNotBlank()) add("\"${filter.query}\"")
    }
    return if (parts.isEmpty()) "All models" else parts.joinToString(" · ")
}
