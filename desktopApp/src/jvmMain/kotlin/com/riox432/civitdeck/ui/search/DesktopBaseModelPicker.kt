package com.riox432.civitdeck.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.BaseModelCatalog
import com.riox432.civitdeck.domain.model.BaseModelStatus
import com.riox432.civitdeck.ui.theme.Spacing

internal data class BaseModelPickerSections(
    val selected: List<BaseModel>,
    val current: List<BaseModel>,
    val older: List<BaseModel>,
) {
    val isEmpty: Boolean get() = selected.isEmpty() && current.isEmpty() && older.isEmpty()
}

/** Filters every section by a case-insensitive substring of the label; a blank query keeps all. */
internal fun baseModelPickerSections(
    catalog: BaseModelCatalog?,
    selection: Set<BaseModel>,
    query: String,
): BaseModelPickerSections {
    val needle = query.trim()
    val matches = { model: BaseModel -> model.displayName.contains(needle, ignoreCase = true) }
    return BaseModelPickerSections(
        selected = orderedBaseModelSelection(selection, catalog).filter(matches),
        current = catalog?.active.orEmpty().filter(matches),
        older = catalog?.retired.orEmpty().filter(matches),
    )
}

/**
 * Catalog order (current, then older), then values outside the catalog sorted by label, because
 * a set restored from storage has no meaningful order.
 */
internal fun orderedBaseModelSelection(
    selection: Set<BaseModel>,
    catalog: BaseModelCatalog?,
): List<BaseModel> {
    val known = catalog?.let { (it.active + it.retired).filter { model -> model in selection } }.orEmpty()
    val knownSet = known.toSet()
    return known + selection.filterNot { it in knownSet }.sortedBy { it.apiValue }
}

/** Label with the catalog marker; no marker while the catalog has not loaded yet. */
internal fun baseModelChipLabel(baseModel: BaseModel, catalog: BaseModelCatalog?): String =
    when (catalog?.statusOf(baseModel)) {
        BaseModelStatus.Retired -> "${baseModel.displayName} (retired)"
        BaseModelStatus.Unknown -> "${baseModel.displayName} (unknown)"
        BaseModelStatus.Active, null -> baseModel.displayName
    }

/**
 * The selection stays local until "Done", so the search runs once per picker session instead of
 * once per click. Closing the dialog any other way discards it.
 */
@Composable
fun DesktopBaseModelPicker(
    catalog: BaseModelCatalog?,
    initialSelection: Set<BaseModel>,
    onApply: (Set<BaseModel>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selection by remember { mutableStateOf(initialSelection) }
    val toggle = { model: BaseModel ->
        selection = if (model in selection) selection - model else selection + model
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose base models") },
        text = {
            BaseModelPickerBody(catalog = catalog, selection = selection, onToggle = toggle)
        },
        confirmButton = {
            TextButton(onClick = { onApply(selection) }) { Text("Done") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { selection = emptySet() }, enabled = selection.isNotEmpty()) {
                    Text("Clear")
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun BaseModelPickerBody(
    catalog: BaseModelCatalog?,
    selection: Set<BaseModel>,
    onToggle: (BaseModel) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var olderExpanded by remember { mutableStateOf(false) }
    val searching = query.isNotBlank()
    val sections = baseModelPickerSections(catalog, selection, query)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.width(PICKER_WIDTH)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search base models") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        )
        Spacer(Modifier.height(Spacing.sm))
        // Fixed height keeps the dialog from resizing on every keystroke.
        LazyColumn(modifier = Modifier.fillMaxWidth().height(PICKER_LIST_HEIGHT)) {
            when {
                catalog == null -> item { PickerMessage("Loading base models…") }
                sections.isEmpty -> item {
                    PickerMessage(
                        if (searching) "No base models match \"${query.trim()}\"" else "No base models available",
                    )
                }
                else -> {
                    selectedSection(sections.selected, catalog, onToggle)
                    optionSection("Current", "current", sections.current, selection, onToggle)
                    olderSection(
                        older = sections.older,
                        selection = selection,
                        expanded = olderExpanded || searching,
                        onToggleExpansion = if (searching) null else ({ olderExpanded = !olderExpanded }),
                        onToggle = onToggle,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.selectedSection(
    selected: List<BaseModel>,
    catalog: BaseModelCatalog,
    onToggle: (BaseModel) -> Unit,
) {
    if (selected.isEmpty()) return
    item(key = "selected-header") { PickerSectionHeader("Selected") }
    // Chips instead of rows: checking an option adds a chip here, and a wrapping chip row
    // shifts the list below far less than a new full-width row would.
    item(key = "selected-chips") {
        FlowRow(
            modifier = Modifier.padding(horizontal = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            selected.forEach { model ->
                RemovableBaseModelChip(
                    label = baseModelChipLabel(model, catalog),
                    onRemove = { onToggle(model) },
                )
            }
        }
    }
}

private fun LazyListScope.optionSection(
    title: String,
    keyPrefix: String,
    options: List<BaseModel>,
    selection: Set<BaseModel>,
    onToggle: (BaseModel) -> Unit,
) {
    if (options.isEmpty()) return
    item(key = "$keyPrefix-header") { PickerSectionHeader(title) }
    items(options, key = { "$keyPrefix:${it.apiValue}" }) { model ->
        BaseModelOptionRow(model, checked = model in selection, onToggle = { onToggle(model) })
    }
}

/** [onToggleExpansion] is null while a search forces the section open. */
private fun LazyListScope.olderSection(
    older: List<BaseModel>,
    selection: Set<BaseModel>,
    expanded: Boolean,
    onToggleExpansion: (() -> Unit)?,
    onToggle: (BaseModel) -> Unit,
) {
    if (older.isEmpty()) return
    item(key = "older-header") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onToggleExpansion != null, onClick = { onToggleExpansion?.invoke() }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PickerSectionHeader("Older (${older.size})", modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse older" else "Expand older",
            )
        }
    }
    if (!expanded) return
    items(older, key = { "older:${it.apiValue}" }) { model ->
        BaseModelOptionRow(model, checked = model in selection, onToggle = { onToggle(model) })
    }
}

@Composable
internal fun RemovableBaseModelChip(label: String, onRemove: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        trailingIcon = {
            Icon(Icons.Default.Close, contentDescription = "Remove")
        },
    )
}

@Composable
private fun BaseModelOptionRow(
    baseModel: BaseModel,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(Spacing.sm))
        Text(baseModel.displayName, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PickerSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm),
    )
}

@Composable
private fun PickerMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(Spacing.sm),
    )
}

private val PICKER_WIDTH = 420.dp
private val PICKER_LIST_HEIGHT = 360.dp
