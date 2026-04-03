@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.comfyui.template

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.domain.model.WorkflowTemplateCategory
import com.riox432.civitdeck.domain.model.WorkflowTemplateType
import com.riox432.civitdeck.ui.theme.Elevation
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun DesktopWorkflowTemplateScreen(
    viewModel: DesktopWorkflowTemplateViewModel,
    onBack: () -> Unit,
    onCreateTemplate: () -> Unit,
    onEditTemplate: (WorkflowTemplate) -> Unit,
    onSelectTemplate: ((WorkflowTemplate) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }
    LaunchedEffect(state.importError) {
        state.importError?.let {
            snackbarHostState.showSnackbar("Import failed: $it")
            viewModel.onDismissImportError()
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                if (onSelectTemplate == null) {
                    FloatingActionButton(onClick = onCreateTemplate) {
                        Icon(Icons.Default.Add, "Create template")
                    }
                }
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                TemplateToolbar(
                    onBack = onBack,
                    onImportClick = { showImportDialog = true },
                    isPicker = onSelectTemplate != null,
                )
                TemplateSearchAndFilters(
                    searchQuery = state.searchQuery,
                    selectedCategory = state.selectedCategory,
                    selectedType = state.selectedType,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onCategorySelected = viewModel::onCategorySelected,
                    onTypeSelected = viewModel::onTypeSelected,
                )
                TemplateList(
                    state = state,
                    onSelectTemplate = onSelectTemplate,
                    onEditTemplate = onEditTemplate,
                    onExport = viewModel::onExportTemplate,
                    onDelete = viewModel::onDeleteTemplate,
                )
            }
        }
    }

    if (showImportDialog) {
        ImportDialog(
            text = importText,
            onTextChange = { importText = it },
            onConfirm = {
                viewModel.onImportTemplate(importText)
                importText = ""
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false },
        )
    }

    state.exportedJson?.let { json ->
        ExportDialog(json = json, onDismiss = viewModel::onDismissExport)
    }
}

@Composable
private fun TemplateToolbar(
    onBack: () -> Unit,
    onImportClick: () -> Unit,
    isPicker: Boolean,
) {
    Surface(tonalElevation = Elevation.xs) {
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
                text = if (isPicker) "Pick Template" else "Workflow Templates",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = Spacing.sm),
            )
            IconButton(onClick = onImportClick) {
                Icon(Icons.Default.Upload, "Import template")
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun TemplateSearchAndFilters(
    searchQuery: String,
    selectedCategory: WorkflowTemplateCategory?,
    selectedType: WorkflowTemplateType?,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (WorkflowTemplateCategory?) -> Unit,
    onTypeSelected: (WorkflowTemplateType?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Search templates...") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, "Clear")
                    }
                }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        CategoryFilterRow(selectedCategory, onCategorySelected)
        TypeFilterRow(selectedType, onTypeSelected)
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: WorkflowTemplateCategory?,
    onCategorySelected: (WorkflowTemplateCategory?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text("All") },
        )
        WorkflowTemplateCategory.entries.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = {
                    onCategorySelected(
                        if (selectedCategory == category) null else category,
                    )
                },
                label = { Text(categoryLabel(category)) },
            )
        }
    }
}

@Composable
private fun TypeFilterRow(
    selectedType: WorkflowTemplateType?,
    onTypeSelected: (WorkflowTemplateType?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        FilterChip(
            selected = selectedType == null,
            onClick = { onTypeSelected(null) },
            label = { Text("All Types") },
        )
        WorkflowTemplateType.entries.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = {
                    onTypeSelected(if (selectedType == type) null else type)
                },
                label = { Text(typeLabel(type)) },
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun TemplateList(
    state: DesktopWorkflowTemplateUiState,
    onSelectTemplate: ((WorkflowTemplate) -> Unit)?,
    onEditTemplate: (WorkflowTemplate) -> Unit,
    onExport: (WorkflowTemplate) -> Unit,
    onDelete: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        when {
            state.isLoading -> item {
                Text("Loading...", style = MaterialTheme.typography.bodyMedium)
            }
            state.filteredTemplates.isEmpty() -> item {
                Text(
                    if (state.templates.isEmpty()) {
                        "No templates yet. Create one with the + button."
                    } else {
                        "No templates match your filters."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> items(state.filteredTemplates, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onSelect = onSelectTemplate?.let { cb -> { cb(template) } },
                    onEdit = if (!template.isBuiltIn) { { onEditTemplate(template) } } else null,
                    onDelete = if (!template.isBuiltIn) { { onDelete(template.id) } } else null,
                    onExport = { onExport(template) },
                )
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun TemplateCard(
    template: WorkflowTemplate,
    onSelect: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onExport: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect ?: {},
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(template.name, style = MaterialTheme.typography.titleSmall)
                    if (template.description.isNotBlank()) {
                        Text(
                            template.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                    Text(
                        buildString {
                            append(typeLabel(template.type))
                            if (template.isBuiltIn) append(" \u2022 Built-in")
                            append(" \u2022 ${categoryLabel(template.category)}")
                            if (template.version > 1) append(" \u2022 v${template.version}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (template.variables.isNotEmpty()) {
                        Text(
                            "${template.variables.size} parameters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row {
                    IconButton(onClick = onExport) {
                        Icon(Icons.Default.ContentCopy, "Export template")
                    }
                    onEdit?.let {
                        IconButton(onClick = it) { Icon(Icons.Default.Edit, "Edit") }
                    }
                    onDelete?.let {
                        IconButton(onClick = it) {
                            Icon(
                                Icons.Default.Delete, "Delete",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Template JSON") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text("Paste template JSON") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                maxLines = 10,
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Import") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ExportDialog(json: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Template JSON") },
        text = {
            OutlinedTextField(
                value = json,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                maxLines = 12,
            )
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Done") } },
    )
}

internal fun typeLabel(type: WorkflowTemplateType) = when (type) {
    WorkflowTemplateType.TXT2IMG -> "txt2img"
    WorkflowTemplateType.IMG2IMG -> "img2img"
    WorkflowTemplateType.INPAINTING -> "Inpainting"
    WorkflowTemplateType.UPSCALE -> "Upscale"
    WorkflowTemplateType.LORA -> "LoRA"
}

internal fun categoryLabel(category: WorkflowTemplateCategory) = when (category) {
    WorkflowTemplateCategory.GENERAL -> "General"
    WorkflowTemplateCategory.ANIME -> "Anime"
    WorkflowTemplateCategory.PHOTOREALISTIC -> "Photo"
    WorkflowTemplateCategory.ARTISTIC -> "Artistic"
    WorkflowTemplateCategory.UTILITY -> "Utility"
}
