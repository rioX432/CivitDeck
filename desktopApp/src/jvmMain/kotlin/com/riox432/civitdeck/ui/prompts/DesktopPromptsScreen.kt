package com.riox432.civitdeck.ui.prompts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.feature.prompts.presentation.PromptTab
import com.riox432.civitdeck.feature.prompts.presentation.SavedPromptsViewModel
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun DesktopPromptsScreen(
    viewModel: SavedPromptsViewModel,
    modifier: Modifier = Modifier,
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val prompts by viewModel.prompts.collectAsState()
    val templates by viewModel.templates.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        PromptsTopBar()
        PromptsTabRow(
            selectedTab = selectedTab,
            onTabSelected = viewModel::onTabSelected,
        )
        SearchField(
            query = searchQuery,
            onQueryChange = viewModel::onSearchQueryChanged,
        )
        PromptsList(
            prompts = if (selectedTab == PromptTab.Templates) templates else prompts,
            onDelete = viewModel::delete,
            onToggleTemplate = viewModel::toggleTemplate,
        )
    }
}

@Composable
private fun PromptsTopBar() {
    Surface(tonalElevation = Elevation.xs) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Saved Prompts",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
    }
}

@Composable
private fun PromptsTabRow(
    selectedTab: PromptTab,
    onTabSelected: (PromptTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        PromptTab.entries.forEach { tab ->
            FilterChip(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                label = { Text(tab.name) },
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search prompts") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
    )
    Spacer(modifier = Modifier.height(Spacing.sm))
}

@Composable
private fun PromptsList(
    prompts: List<SavedPrompt>,
    onDelete: (Long) -> Unit,
    onToggleTemplate: (Long, Boolean, String?) -> Unit,
) {
    if (prompts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.AutoMirrored.Filled.TextSnippet,
                    contentDescription = null,
                    modifier = Modifier.size(EMPTY_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = "No saved prompts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(items = prompts, key = { it.id }) { prompt ->
            PromptCard(
                prompt = prompt,
                onDelete = { onDelete(prompt.id) },
                onToggleTemplate = {
                    onToggleTemplate(prompt.id, !prompt.isTemplate, prompt.templateName)
                },
            )
        }
    }
}

@Composable
private fun PromptCard(
    prompt: SavedPrompt,
    onDelete: () -> Unit,
    onToggleTemplate: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.xs),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            if (prompt.templateName != null) {
                Text(
                    text = prompt.templateName!!,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
            }
            Text(
                text = prompt.prompt,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = MAX_PROMPT_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            if (prompt.negativePrompt != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "Negative: ${prompt.negativePrompt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PromptMetaRow(prompt)
            Spacer(modifier = Modifier.height(Spacing.xs))
            PromptActions(
                onCopy = {
                    clipboardManager.setText(AnnotatedString(prompt.prompt))
                },
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun PromptMetaRow(prompt: SavedPrompt) {
    val meta = buildList {
        prompt.modelName?.let { add("Model: $it") }
        prompt.sampler?.let { add("Sampler: $it") }
        prompt.steps?.let { add("Steps: $it") }
        prompt.cfgScale?.let { add("CFG: $it") }
    }
    if (meta.isNotEmpty()) {
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = meta.joinToString(" | "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PromptActions(
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onCopy) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy prompt",
                modifier = Modifier.size(ACTION_ICON_SIZE),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(ACTION_ICON_SIZE),
            )
        }
    }
}

private val EMPTY_ICON_SIZE = 48.dp
private val ACTION_ICON_SIZE = 20.dp
private const val MAX_PROMPT_LINES = 4
