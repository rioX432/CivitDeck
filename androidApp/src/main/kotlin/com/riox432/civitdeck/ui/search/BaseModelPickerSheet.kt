package com.riox432.civitdeck.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.BaseModelCatalog
import com.riox432.civitdeck.domain.model.BaseModelStatus
import com.riox432.civitdeck.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Orders a selection like the picker lists it: catalog (API) order first, then values outside
 * the catalog by name, because a set restored from storage or Swift has no stable order.
 */
internal fun orderedBaseModelSelection(
    selection: Set<BaseModel>,
    catalog: BaseModelCatalog?,
): List<BaseModel> {
    val catalogOrder = catalog?.let { it.active + it.retired }.orEmpty()
    val catalogSet = catalogOrder.toSet()
    val inCatalog = catalogOrder.filter { it in selection }
    val outside = selection.filterNot { it in catalogSet }.sortedBy { it.apiValue }
    return inCatalog + outside
}

/** The "retired" / "unknown" marker for a selected value; null for an active one. */
@Composable
internal fun baseModelMarker(baseModel: BaseModel, catalog: BaseModelCatalog?): String? =
    when (catalog?.statusOf(baseModel)) {
        BaseModelStatus.Retired -> stringResource(R.string.base_model_picker_marker_retired)
        BaseModelStatus.Unknown -> stringResource(R.string.base_model_picker_marker_unknown)
        // No catalog yet: no basis for a marker.
        BaseModelStatus.Active, null -> null
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BaseModelPickerSheet(
    catalog: BaseModelCatalog?,
    initialSelection: Set<BaseModel>,
    onApply: (Set<BaseModel>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selection by remember { mutableStateOf(initialSelection) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        BaseModelPickerContent(
            catalog = catalog,
            selection = selection,
            onToggle = { baseModel ->
                selection = if (baseModel in selection) selection - baseModel else selection + baseModel
            },
            onClear = { selection = emptySet() },
            onDone = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) onApply(selection)
                }
            },
        )
    }
}

@Composable
private fun BaseModelPickerContent(
    catalog: BaseModelCatalog?,
    selection: Set<BaseModel>,
    onToggle: (BaseModel) -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var isOlderExpanded by rememberSaveable { mutableStateOf(false) }
    val trimmedQuery = query.trim()
    val isSearching = trimmedQuery.isNotEmpty()
    val sections = remember(catalog, selection, trimmedQuery) {
        baseModelPickerSections(catalog, selection, trimmedQuery)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        PickerHeader(hasSelection = selection.isNotEmpty(), onClear = onClear, onDone = onDone)
        PickerSearchField(query = query, onQueryChange = { query = it })
        LazyColumn(contentPadding = PaddingValues(top = Spacing.sm, bottom = Spacing.lg)) {
            selectedSection(sections.selected, selection, catalog, onToggle)
            if (catalog == null) {
                item(key = "loading") { PickerLoading() }
            } else {
                currentSection(sections.current, selection, onToggle)
                olderSection(
                    older = sections.older,
                    selection = selection,
                    isExpanded = isOlderExpanded || isSearching,
                    isToggleEnabled = !isSearching,
                    onToggleExpanded = { isOlderExpanded = !isOlderExpanded },
                    onToggle = onToggle,
                )
                if (isSearching && sections.current.isEmpty() && sections.older.isEmpty()) {
                    item(key = "no-match") { PickerNoMatch(trimmedQuery) }
                }
            }
        }
    }
}

internal data class BaseModelPickerSections(
    val selected: List<BaseModel>,
    val current: List<BaseModel>,
    val older: List<BaseModel>,
)

/**
 * "Current" and "Older" keep the API order and match [query] as a case-insensitive substring of
 * the label. "Selected" ignores the query so the user always sees everything that will be applied.
 */
internal fun baseModelPickerSections(
    catalog: BaseModelCatalog?,
    selection: Set<BaseModel>,
    query: String,
): BaseModelPickerSections {
    val trimmed = query.trim()
    fun BaseModel.matches() = trimmed.isEmpty() || displayName.contains(trimmed, ignoreCase = true)
    return BaseModelPickerSections(
        selected = orderedBaseModelSelection(selection, catalog),
        current = catalog?.active.orEmpty().filter { it.matches() },
        older = catalog?.retired.orEmpty().filter { it.matches() },
    )
}

private fun LazyListScope.selectedSection(
    selected: List<BaseModel>,
    selection: Set<BaseModel>,
    catalog: BaseModelCatalog?,
    onToggle: (BaseModel) -> Unit,
) {
    if (selected.isEmpty()) return
    item(key = "selected-header") {
        PickerSectionHeader(stringResource(R.string.base_model_picker_section_selected), selected.size)
    }
    items(selected, key = { "selected:${it.apiValue}" }) { baseModel ->
        BaseModelRow(
            label = baseModel.displayName,
            marker = baseModelMarker(baseModel, catalog),
            isSelected = baseModel in selection,
            onToggle = { onToggle(baseModel) },
        )
    }
}

private fun LazyListScope.currentSection(
    current: List<BaseModel>,
    selection: Set<BaseModel>,
    onToggle: (BaseModel) -> Unit,
) {
    if (current.isEmpty()) return
    item(key = "current-header") {
        PickerSectionHeader(stringResource(R.string.base_model_picker_section_current), current.size)
    }
    items(current, key = { "current:${it.apiValue}" }) { baseModel ->
        BaseModelRow(
            label = baseModel.displayName,
            marker = null,
            isSelected = baseModel in selection,
            onToggle = { onToggle(baseModel) },
        )
    }
}

private fun LazyListScope.olderSection(
    older: List<BaseModel>,
    selection: Set<BaseModel>,
    isExpanded: Boolean,
    isToggleEnabled: Boolean,
    onToggleExpanded: () -> Unit,
    onToggle: (BaseModel) -> Unit,
) {
    if (older.isEmpty()) return
    item(key = "older-header") {
        OlderSectionHeader(
            count = older.size,
            isExpanded = isExpanded,
            isToggleEnabled = isToggleEnabled,
            onToggleExpanded = onToggleExpanded,
        )
    }
    if (!isExpanded) return
    items(older, key = { "older:${it.apiValue}" }) { baseModel ->
        BaseModelRow(
            label = baseModel.displayName,
            marker = null,
            isSelected = baseModel in selection,
            onToggle = { onToggle(baseModel) },
        )
    }
}

@Composable
private fun PickerHeader(
    hasSelection: Boolean,
    onClear: () -> Unit,
    onDone: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.base_model_picker_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClear, enabled = hasSelection) {
                Text(stringResource(R.string.action_clear))
            }
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.action_done))
            }
        }
    }
}

@Composable
private fun PickerSearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        placeholder = { Text(stringResource(R.string.base_model_picker_search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_clear))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    )
}

@Composable
private fun PickerSectionHeader(title: String, count: Int) {
    Row(modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs)) {
        FilterSectionHeader(title, stringResource(R.string.base_model_picker_section_count, count))
    }
}

@Composable
private fun OlderSectionHeader(
    count: Int,
    isExpanded: Boolean,
    isToggleEnabled: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val actionLabel = stringResource(
        if (isExpanded) R.string.cd_collapse_section else R.string.cd_expand_section,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                enabled = isToggleEnabled,
                onClickLabel = actionLabel,
                role = Role.Button,
                onClick = onToggleExpanded,
            )
            .padding(top = Spacing.sm, end = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f)) {
            FilterSectionHeader(
                stringResource(R.string.base_model_picker_section_older),
                stringResource(R.string.base_model_picker_section_count, count),
            )
        }
        if (isToggleEnabled) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BaseModelRow(
    label: String,
    marker: String?,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Checkbox(checked = isSelected, onCheckedChange = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (marker != null) {
            Text(
                text = marker,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PickerLoading() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PickerNoMatch(query: String) {
    Text(
        text = stringResource(R.string.base_model_picker_no_match, query),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
    )
}
