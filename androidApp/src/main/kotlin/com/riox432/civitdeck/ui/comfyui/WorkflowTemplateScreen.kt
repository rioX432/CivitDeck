@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.domain.model.WorkflowTemplateCategory
import com.riox432.civitdeck.domain.model.WorkflowTemplateType
import com.riox432.civitdeck.ui.theme.Spacing

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowTemplateScreen(
    viewModel: WorkflowTemplateViewModel,
    onBack: () -> Unit,
    onCreateTemplate: () -> Unit,
    onEditTemplate: (WorkflowTemplate) -> Unit,
    onSelectTemplate: ((WorkflowTemplate) -> Unit)? = null,
    onShareQR: ((String) -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    TemplateScreenEffects(state, snackbarHostState, viewModel)
    TemplateScaffold(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onImportClick = { showImportDialog = true },
        onCreateTemplate = onCreateTemplate,
        onEditTemplate = onEditTemplate,
        onSelectTemplate = onSelectTemplate,
        onExport = viewModel::onExportTemplate,
        onDelete = viewModel::onDeleteTemplate,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onTypeSelected = viewModel::onTypeSelected,
        onShareQR = onShareQR,
        isPicker = onSelectTemplate != null,
    )
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
private fun TemplateScreenEffects(
    state: WorkflowTemplateUiState,
    snackbarHostState: SnackbarHostState,
    viewModel: WorkflowTemplateViewModel,
) {
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
}

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateScaffold(
    state: WorkflowTemplateUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onImportClick: () -> Unit,
    onCreateTemplate: () -> Unit,
    onEditTemplate: (WorkflowTemplate) -> Unit,
    onSelectTemplate: ((WorkflowTemplate) -> Unit)?,
    onExport: (WorkflowTemplate) -> Unit,
    onDelete: (Long) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (WorkflowTemplateCategory?) -> Unit,
    onTypeSelected: (WorkflowTemplateType?) -> Unit,
    onShareQR: ((String) -> Unit)?,
    isPicker: Boolean,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isPicker) "Pick Template" else "Workflow Templates") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onImportClick) {
                        Icon(Icons.Default.Upload, "Import template")
                    }
                },
            )
        },
        floatingActionButton = {
            if (!isPicker) {
                FloatingActionButton(onClick = onCreateTemplate) {
                    Icon(Icons.Default.Add, "Create template")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        TemplateContent(
            modifier = Modifier.padding(padding),
            state = state,
            onSelectTemplate = onSelectTemplate,
            onEditTemplate = onEditTemplate,
            onExport = onExport,
            onDelete = onDelete,
            onSearchQueryChanged = onSearchQueryChanged,
            onCategorySelected = onCategorySelected,
            onTypeSelected = onTypeSelected,
            onShareQR = onShareQR,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun TemplateContent(
    modifier: Modifier,
    state: WorkflowTemplateUiState,
    onSelectTemplate: ((WorkflowTemplate) -> Unit)?,
    onEditTemplate: (WorkflowTemplate) -> Unit,
    onExport: (WorkflowTemplate) -> Unit,
    onDelete: (Long) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (WorkflowTemplateCategory?) -> Unit,
    onTypeSelected: (WorkflowTemplateType?) -> Unit,
    onShareQR: ((String) -> Unit)?,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            TemplateSearchBar(query = state.searchQuery, onQueryChanged = onSearchQueryChanged)
        }
        item {
            CategoryFilterRow(
                selectedCategory = state.selectedCategory,
                onCategorySelected = onCategorySelected,
            )
        }
        item {
            TypeFilterRow(selectedType = state.selectedType, onTypeSelected = onTypeSelected)
        }
        templateListItems(state, onSelectTemplate, onEditTemplate, onExport, onDelete, onShareQR)
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.templateListItems(
    state: WorkflowTemplateUiState,
    onSelectTemplate: ((WorkflowTemplate) -> Unit)?,
    onEditTemplate: (WorkflowTemplate) -> Unit,
    onExport: (WorkflowTemplate) -> Unit,
    onDelete: (Long) -> Unit,
    onShareQR: ((String) -> Unit)?,
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
                onShareQR = onShareQR,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChanged,
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text("Search templates...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { onQueryChanged("") }) { Icon(Icons.Default.Close, "Clear") } }
                } else {
                    null
                },
            )
        },
        expanded = false,
        onExpandedChange = {},
        modifier = Modifier.fillMaxWidth(),
    ) {}
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

@Suppress("LongParameterList")
@Composable
private fun TemplateCard(
    template: WorkflowTemplate,
    onSelect: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onExport: () -> Unit,
    onShareQR: ((String) -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect ?: {},
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TemplateCardInfo(template = template, modifier = Modifier.weight(1f))
                TemplateCardActions(
                    template = template,
                    onExport = onExport,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onShareQR = onShareQR,
                )
            }
        }
    }
}

@Composable
private fun TemplateCardInfo(template: WorkflowTemplate, modifier: Modifier) {
    Column(modifier = modifier) {
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
}

@Composable
private fun TemplateCardActions(
    template: WorkflowTemplate,
    onExport: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onShareQR: ((String) -> Unit)?,
) {
    Row {
        onShareQR?.let { share ->
            IconButton(onClick = { share(template.name) }) {
                Icon(Icons.Default.QrCode, "Share QR code")
            }
        }
        IconButton(onClick = onExport) {
            Icon(Icons.Default.ContentCopy, "Export template")
        }
        onEdit?.let {
            IconButton(onClick = it) { Icon(Icons.Default.Edit, "Edit template") }
        }
        onDelete?.let {
            IconButton(onClick = it) {
                Icon(
                    Icons.Default.Delete,
                    "Delete template",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun typeLabel(type: WorkflowTemplateType) = when (type) {
    WorkflowTemplateType.TXT2IMG -> "txt2img"
    WorkflowTemplateType.IMG2IMG -> "img2img"
    WorkflowTemplateType.INPAINTING -> "Inpainting"
    WorkflowTemplateType.UPSCALE -> "Upscale"
    WorkflowTemplateType.LORA -> "LoRA"
}

private fun categoryLabel(category: WorkflowTemplateCategory) = when (category) {
    WorkflowTemplateCategory.GENERAL -> "General"
    WorkflowTemplateCategory.ANIME -> "Anime"
    WorkflowTemplateCategory.PHOTOREALISTIC -> "Photo"
    WorkflowTemplateCategory.ARTISTIC -> "Artistic"
    WorkflowTemplateCategory.UTILITY -> "Utility"
}
