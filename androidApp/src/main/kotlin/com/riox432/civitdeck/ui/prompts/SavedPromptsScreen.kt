package com.riox432.civitdeck.ui.prompts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.export.WorkflowExportService
import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun SavedPromptsScreen(
    viewModel: SavedPromptsViewModel,
    scrollToTopTrigger: Int = 0,
) {
    val prompts by viewModel.prompts.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

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
            EmptyState()
        } else {
            PromptList(
                prompts = displayedPrompts,
                onDelete = viewModel::delete,
                onToggleTemplate = viewModel::toggleTemplate,
                listState = listState,
            )
        }
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
        placeholder = { Text("Search prompts...") },
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
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Outlined.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "No saved prompts yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Prompts are auto-saved when you view images.\nYou can also save prompts manually.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PromptList(
    prompts: List<SavedPrompt>,
    onDelete: (Long) -> Unit,
    onToggleTemplate: (Long, Boolean, String?) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(prompts, key = { it.id }) { prompt ->
            PromptCard(
                prompt = prompt,
                onCopy = { copyToClipboard(context, prompt.prompt) },
                onDelete = { onDelete(prompt.id) },
                onToggleTemplate = {
                    onToggleTemplate(prompt.id, !prompt.isTemplate, prompt.templateName)
                },
                onExport = { exportPrompt(context, prompt) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun PromptCard(
    prompt: SavedPrompt,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onToggleTemplate: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.card),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            PromptHeader(prompt = prompt, onToggleTemplate = onToggleTemplate)
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
            PromptActions(onCopy = onCopy, onExport = onExport, onDelete = onDelete)
        }
    }
}

@Composable
private fun PromptHeader(prompt: SavedPrompt, onToggleTemplate: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            prompt.templateName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
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
private fun PromptActions(onCopy: () -> Unit, onExport: () -> Unit, onDelete: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onCopy) {
            Text("Copy")
        }
        OutlinedButton(onClick = onExport) {
            Text("Export")
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
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
