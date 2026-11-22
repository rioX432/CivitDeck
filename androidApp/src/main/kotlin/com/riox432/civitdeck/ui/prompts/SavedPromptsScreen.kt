package com.riox432.civitdeck.ui.prompts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.export.PromptTemplateEngine
import com.riox432.civitdeck.domain.export.WorkflowExportService
import com.riox432.civitdeck.domain.model.HapticFeedbackType
import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.feature.prompts.presentation.PromptTab
import com.riox432.civitdeck.feature.prompts.presentation.SavedPromptsViewModel
import com.riox432.civitdeck.feature.prompts.presentation.SavedReferenceDetailState
import com.riox432.civitdeck.feature.prompts.presentation.SavedReferenceViewModel
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.components.rememberHapticFeedback
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun SavedPromptsScreen(
    viewModel: SavedPromptsViewModel,
    scrollToTopTrigger: Int = 0,
    referenceViewModel: SavedReferenceViewModel? = null,
    onTryReference: ((Long) -> Unit)? = null,
) {
    val prompts by viewModel.prompts.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var openReferenceId by rememberSaveable { mutableStateOf<Long?>(null) }

    var lastHandledTrigger by rememberSaveable { mutableIntStateOf(scrollToTopTrigger) }
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger != lastHandledTrigger) {
            lastHandledTrigger = scrollToTopTrigger
            listState.animateScrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = searchQuery,
            onQueryChange = viewModel::onSearchQueryChanged,
        )
        PromptTabs(
            selectedTab = selectedTab,
            onTabSelected = viewModel::onTabSelected,
        )
        val displayedPrompts = when (selectedTab) {
            PromptTab.All -> prompts
            PromptTab.History -> prompts.filter { it.autoSaved }
            PromptTab.Templates -> templates
        }
        if (displayedPrompts.isEmpty()) {
            EmptyStateMessage(
                icon = Icons.Outlined.BookmarkBorder,
                title = "No saved prompts yet",
                subtitle = "Prompts are auto-saved when you view images.\nYou can also save prompts manually.",
            )
        } else {
            PromptList(
                prompts = displayedPrompts,
                onDelete = viewModel::delete,
                onToggleTemplate = viewModel::toggleTemplate,
                onOpenSource = referenceViewModel?.let { { id: Long -> openReferenceId = id } },
                listState = listState,
            )
        }
    }
    val referenceId = openReferenceId
    if (referenceViewModel != null && referenceId != null) {
        SavedReferenceDetail(referenceViewModel, referenceId, onTryReference) { openReferenceId = null }
    }
}

@Composable
private fun SavedReferenceDetail(
    viewModel: SavedReferenceViewModel,
    referenceId: Long,
    onTryReference: ((Long) -> Unit)?,
    onClose: () -> Unit,
) {
    SavedReferenceSheet(
        viewModel = viewModel,
        referenceId = referenceId,
        onDismiss = onClose,
        footer = {
            if (onTryReference != null) {
                TryReferenceButton(viewModel, referenceId) {
                    onClose()
                    onTryReference(referenceId)
                }
            }
        },
    )
}

// Only a snapshot that decoded for this row can be tried; a missing or unreadable one has nothing
// to link, and the sheet already says why.
@Composable
private fun TryReferenceButton(viewModel: SavedReferenceViewModel, referenceId: Long, onTry: () -> Unit) {
    val detail by viewModel.detailState.collectAsStateWithLifecycle()
    val loaded = detail as? SavedReferenceDetailState.Loaded
    Button(
        onClick = onTry,
        enabled = loaded?.prompt?.id == referenceId && loaded.reference != null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.saved_reference_try))
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        placeholder = { Text(stringResource(R.string.prompts_search_placeholder)) },
        singleLine = true,
    )
}

@Composable
private fun PromptTabs(
    selectedTab: PromptTab,
    onTabSelected: (PromptTab) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        edgePadding = Spacing.lg,
    ) {
        PromptTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = { Text(tab.name) },
            )
        }
    }
}

@Composable
private fun PromptList(
    prompts: List<SavedPrompt>,
    onDelete: (Long) -> Unit,
    onToggleTemplate: (Long, Boolean, String?) -> Unit,
    onOpenSource: ((Long) -> Unit)?,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val context = LocalContext.current
    var templateDialogPromptId by remember { mutableStateOf<Long?>(null) }
    var templateNameInput by remember { mutableStateOf("") }
    var applyTemplatePrompt by remember { mutableStateOf<SavedPrompt?>(null) }

    PromptLazyColumn(
        prompts = prompts,
        modifier = modifier,
        listState = listState,
        context = context,
        onDelete = onDelete,
        onToggleTemplate = { id, isTemplate ->
            if (isTemplate) {
                onToggleTemplate(id, false, null)
            } else {
                templateNameInput = ""
                templateDialogPromptId = id
            }
        },
        onRequestApply = { applyTemplatePrompt = it },
        onOpenSource = onOpenSource,
    )

    templateDialogPromptId?.let { promptId ->
        TemplateNameDialog(
            name = templateNameInput,
            onNameChange = { templateNameInput = it },
            onConfirm = {
                val name = templateNameInput.trim().ifEmpty { null }
                onToggleTemplate(promptId, true, name)
                templateDialogPromptId = null
            },
            onDismiss = { templateDialogPromptId = null },
        )
    }

    applyTemplatePrompt?.let { prompt ->
        ApplyTemplateDialog(
            prompt = prompt,
            onGenerate = { result ->
                copyToClipboard(context, result)
                applyTemplatePrompt = null
            },
            onDismiss = { applyTemplatePrompt = null },
        )
    }
}

@Composable
private fun PromptLazyColumn(
    prompts: List<SavedPrompt>,
    context: Context,
    onDelete: (Long) -> Unit,
    onToggleTemplate: (Long, Boolean) -> Unit,
    onRequestApply: (SavedPrompt) -> Unit,
    onOpenSource: ((Long) -> Unit)?,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val haptic = rememberHapticFeedback()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (prompts.isEmpty()) {
            item {
                EmptyStateMessage(
                    icon = Icons.Outlined.SearchOff,
                    title = "No results",
                    subtitle = "Try adjusting your search filters",
                )
            }
        }
        items(prompts, key = { it.id }) { prompt ->
            PromptCard(
                prompt = prompt,
                onCopy = {
                    haptic(HapticFeedbackType.Success)
                    copyToClipboard(context, prompt.prompt)
                },
                onDelete = { onDelete(prompt.id) },
                onToggleTemplate = { onToggleTemplate(prompt.id, prompt.isTemplate) },
                onExport = { exportPrompt(context, prompt) },
                onApply = if (prompt.isTemplate) {
                    {
                        val variables = PromptTemplateEngine.extractVariables(prompt.prompt)
                        if (variables.isEmpty()) {
                            haptic(HapticFeedbackType.Success)
                            copyToClipboard(context, prompt.prompt)
                        } else {
                            onRequestApply(prompt)
                        }
                    }
                } else {
                    null
                },
                onOpenSource = onOpenSource?.let { open -> { open(prompt.id) } },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun TemplateNameDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.prompts_save_as_template)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.prompts_template_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun PromptCard(
    prompt: SavedPrompt,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onToggleTemplate: () -> Unit,
    onExport: () -> Unit,
    onApply: (() -> Unit)? = null,
    onOpenSource: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val row = promptRowState(prompt)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.card),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            PromptHeader(
                prompt = prompt,
                isReference = row.isReference,
                onToggleTemplate = onToggleTemplate,
                onOpenSource = onOpenSource.takeIf { row.hasSource },
            )
            Text(
                text = prompt.prompt,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            prompt.negativePrompt?.let {
                Spacer(modifier = Modifier.height(Spacing.xs))
                NegativePromptText(text = it)
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            PromptParams(prompt)
            Spacer(modifier = Modifier.height(Spacing.sm))
            PromptActions(
                promptActionsEnabled = row.promptActionsEnabled,
                onCopy = onCopy,
                onExport = onExport,
                onDelete = onDelete,
                onApply = onApply,
            )
        }
    }
}

@Composable
private fun PromptHeader(
    prompt: SavedPrompt,
    isReference: Boolean,
    onToggleTemplate: () -> Unit,
    onOpenSource: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (isReference) {
                Text(
                    text = stringResource(R.string.saved_reference_row_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
            }
            prompt.templateName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
            }
            prompt.modelName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
            }
        }
        if (onOpenSource != null) {
            TextButton(onClick = onOpenSource) {
                Text(stringResource(R.string.saved_reference_open_source))
            }
        }
        IconButton(onClick = onToggleTemplate) {
            Icon(
                imageVector = if (prompt.isTemplate) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (prompt.isTemplate) "Remove template" else "Save as template",
                tint = if (prompt.isTemplate) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun NegativePromptText(text: String) {
    Text(
        text = "Negative: $text",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun PromptActions(
    promptActionsEnabled: Boolean,
    onCopy: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onApply: (() -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onApply != null) {
            OutlinedButton(onClick = onApply, enabled = promptActionsEnabled) {
                Text(stringResource(R.string.action_apply))
            }
        }
        OutlinedButton(onClick = onCopy, enabled = promptActionsEnabled) {
            Text(stringResource(R.string.action_copy))
        }
        OutlinedButton(onClick = onExport, enabled = promptActionsEnabled) {
            Text(stringResource(R.string.action_export))
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.cd_delete),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun PromptParams(prompt: SavedPrompt) {
    val params = buildList {
        prompt.sampler?.let { add("Sampler: $it") }
        prompt.steps?.let { add("Steps: $it") }
        prompt.cfgScale?.let { add("CFG: $it") }
        prompt.seed?.let { add("Seed: $it") }
        prompt.size?.let { add("Size: $it") }
    }
    if (params.isNotEmpty()) {
        Text(
            text = params.joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ApplyTemplateDialog(
    prompt: SavedPrompt,
    onGenerate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val variables = remember(prompt.id) {
        PromptTemplateEngine.extractVariables(prompt.prompt)
    }
    val values = remember(prompt.id) {
        mutableStateOf(variables.associateWith { "" })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(prompt.templateName ?: "Apply Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                variables.forEach { variable ->
                    OutlinedTextField(
                        value = values.value[variable] ?: "",
                        onValueChange = { newValue ->
                            values.value = values.value.toMutableMap().apply {
                                put(variable, newValue)
                            }
                        },
                        label = { Text(variable) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val result = PromptTemplateEngine.substitute(prompt.prompt, values.value)
                onGenerate(result)
            }) { Text(stringResource(R.string.action_generate)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun exportPrompt(context: Context, prompt: SavedPrompt) {
    val meta = prompt.toMeta()
    val text = WorkflowExportService.generateA1111Params(meta)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Export Prompt"))
}

private fun SavedPrompt.toMeta() = ImageGenerationMeta(
    prompt = prompt,
    negativePrompt = negativePrompt,
    sampler = sampler,
    cfgScale = cfgScale,
    steps = steps,
    seed = seed,
    model = modelName,
    size = size,
)

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Prompt", text))
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

internal data class PromptRowState(
    val isReference: Boolean,
    val promptActionsEnabled: Boolean,
    val hasSource: Boolean,
)

/**
 * A row saved from a source without a prompt keeps an empty editable prompt, so prompt actions
 * would copy or export nothing; its source snapshot stays inspectable. Rows without a source keep
 * their actions even when blank, since the built-in templates are seeded with an empty prompt.
 */
internal fun promptRowState(prompt: SavedPrompt): PromptRowState {
    val hasSource = prompt.sourceReference != null
    val isReference = hasSource && prompt.prompt.isBlank()
    return PromptRowState(
        isReference = isReference,
        promptActionsEnabled = !isReference,
        hasSource = hasSource,
    )
}
