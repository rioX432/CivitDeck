package com.riox432.civitdeck.ui.comfyhub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.ComfyHubWorkflow
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyHubDetailUiState
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyHubDetailViewModel
import com.riox432.civitdeck.ui.components.ErrorStateView
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfyHubDetailScreen(
    viewModel: ComfyHubDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    DetailScreenEffects(state, snackbarHostState, viewModel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(state.workflow?.name ?: stringResource(R.string.comfyhub_detail_fallback_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        DetailContentSwitch(state, viewModel, Modifier.padding(padding))
    }
}

@Composable
private fun DetailScreenEffects(
    state: ComfyHubDetailUiState,
    snackbarHostState: SnackbarHostState,
    viewModel: ComfyHubDetailViewModel,
) {
    val context = LocalContext.current
    LaunchedEffect(state.importSuccess, state.importError) {
        if (state.importSuccess) {
            snackbarHostState.showSnackbar("Workflow imported successfully!")
            viewModel.dismissImportResult()
        }
        state.importError?.let {
            snackbarHostState.showSnackbar("Import failed: $it")
            viewModel.dismissImportResult()
        }
    }
    LaunchedEffect(state.saveTemplateSuccess, state.saveTemplateError) {
        if (state.saveTemplateSuccess) {
            snackbarHostState.showSnackbar(context.getString(R.string.comfyhub_template_saved))
            viewModel.dismissSaveTemplateResult()
        }
        state.saveTemplateError?.let {
            snackbarHostState.showSnackbar(context.getString(R.string.comfyhub_save_template_failed, it))
            viewModel.dismissSaveTemplateResult()
        }
    }
}

@Composable
private fun DetailContentSwitch(
    state: ComfyHubDetailUiState,
    viewModel: ComfyHubDetailViewModel,
    modifier: Modifier,
) {
    when {
        state.isLoading -> LoadingStateOverlay()
        state.error != null -> ErrorStateView(
            message = requireNotNull(state.error) { "error must not be null" },
            onRetry = viewModel::retry,
        )
        state.workflow != null -> DetailContent(
            workflow = requireNotNull(state.workflow) { "workflow must not be null" },
            nodeNames = state.nodeNames,
            isImporting = state.isImporting,
            hasAppMode = state.hasAppMode,
            isSavingTemplate = state.isSavingTemplate,
            onImport = viewModel::onImport,
            onSaveAsTemplate = viewModel::onSaveAsTemplate,
            modifier = modifier,
        )
    }
}

@Composable
private fun DetailContent(
    workflow: ComfyHubWorkflow,
    nodeNames: List<String>,
    isImporting: Boolean,
    hasAppMode: Boolean,
    isSavingTemplate: Boolean,
    onImport: () -> Unit,
    onSaveAsTemplate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        WorkflowHeader(workflow, hasAppMode)
        HorizontalDivider()
        DescriptionSection(workflow.description)
        TagsSection(workflow.tags)
        HorizontalDivider()
        NodeGraphSection(nodeNames)
        HorizontalDivider()
        ImportButton(isImporting, onImport)
        SaveAsTemplateButton(isSavingTemplate, onSaveAsTemplate)
    }
}

@Composable
private fun WorkflowHeader(workflow: ComfyHubWorkflow, hasAppMode: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                workflow.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            if (hasAppMode) {
                AppModeBadge()
            }
        }
        Text(
            text = "by ${workflow.author}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatItem(Icons.Outlined.Download, formatCount(workflow.downloads))
            StatItem(Icons.Outlined.Star, String.format(java.util.Locale.US, "%.1f", workflow.rating))
            Text(
                "${workflow.nodeCount} nodes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                workflow.category,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DescriptionSection(description: String) {
    Column {
        Text(stringResource(R.string.comfyhub_description_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(tags: List<String>) {
    if (tags.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        tags.forEach { tag ->
            AssistChip(onClick = {}, label = { Text(tag) })
        }
    }
}

@Composable
private fun NodeGraphSection(nodeNames: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            "Node Graph (${nodeNames.size} node types)",
            style = MaterialTheme.typography.titleMedium,
        )
        nodeNames.forEach { nodeName ->
            Text(
                text = nodeName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
    }
}

@Composable
private fun ImportButton(isImporting: Boolean, onImport: () -> Unit) {
    Button(
        onClick = onImport,
        enabled = !isImporting,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isImporting) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
            )
            Text(stringResource(R.string.label_importing), modifier = Modifier.padding(start = Spacing.sm))
        } else {
            Icon(Icons.Outlined.Download, contentDescription = "Import workflow")
            Text(stringResource(R.string.comfyhub_import_to_comfyui), modifier = Modifier.padding(start = Spacing.sm))
        }
    }
}

@Composable
private fun AppModeBadge() {
    Text(
        text = stringResource(R.string.template_app_mode_badge),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(Spacing.xs),
            )
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    )
}

@Composable
private fun SaveAsTemplateButton(isSaving: Boolean, onSave: () -> Unit) {
    OutlinedButton(
        onClick = onSave,
        enabled = !isSaving,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
            )
            Text(
                stringResource(R.string.comfyhub_saving_template),
                modifier = Modifier.padding(start = Spacing.sm),
            )
        } else {
            Icon(Icons.Outlined.Star, contentDescription = null)
            Text(
                stringResource(R.string.comfyhub_save_as_template),
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1000 -> "${count / 1000}k"
    else -> count.toString()
}
