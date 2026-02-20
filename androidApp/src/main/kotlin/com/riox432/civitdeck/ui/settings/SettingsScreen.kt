package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.BuildConfig
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToLicenses: () -> Unit = {},
    scrollToTopTrigger: Int = 0,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var lastHandledTrigger by rememberSaveable { mutableIntStateOf(scrollToTopTrigger) }
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger != lastHandledTrigger) {
            lastHandledTrigger = scrollToTopTrigger
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(state = listState) {
        item { SectionHeader("Account") }
        item {
            AccountSection(
                apiKey = state.apiKey,
                connectedUsername = state.connectedUsername,
                isValidating = state.isValidatingApiKey,
                error = state.apiKeyError,
                onValidateAndSave = viewModel::onValidateAndSaveApiKey,
                onClear = viewModel::onClearApiKey,
            )
        }
        item { SectionHeader("Content Filter") }
        item { NsfwToggleRow(state.nsfwFilterLevel, viewModel::onNsfwFilterChanged) }
        item { SectionHeader("Display") }
        item { SortOrderRow(state.defaultSortOrder, viewModel::onSortOrderChanged) }
        item { TimePeriodRow(state.defaultTimePeriod, viewModel::onTimePeriodChanged) }
        item { GridColumnsRow(state.gridColumns, viewModel::onGridColumnsChanged) }
        item { SectionHeader("Data Management") }
        item { HiddenModelsRow(state.hiddenModels.size, state.hiddenModels, viewModel::onUnhideModel) }
        item { ExcludedTagsRow(state.excludedTags, viewModel::onAddExcludedTag, viewModel::onRemoveExcludedTag) }
        item { ClearActionRow("Clear Search History", viewModel::onClearSearchHistory) }
        item { ClearActionRow("Clear Browsing History", viewModel::onClearBrowsingHistory) }
        item { ClearActionRow("Clear Cache", viewModel::onClearCache) }
        item { SectionHeader("About") }
        item { InfoRow("App Version", BuildConfig.VERSION_NAME) }
        item { NavigationRow("Open Source Licenses", onNavigateToLicenses) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    )
}

@Composable
private fun NsfwToggleRow(level: NsfwFilterLevel, onToggle: (NsfwFilterLevel) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("NSFW Content", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Show NSFW content in search results",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = level != NsfwFilterLevel.Off,
            onCheckedChange = {
                onToggle(if (level == NsfwFilterLevel.Off) NsfwFilterLevel.All else NsfwFilterLevel.Off)
            },
        )
    }
}

@Composable
private fun SortOrderRow(selected: SortOrder, onChanged: (SortOrder) -> Unit) {
    DropdownSettingRow(
        label = "Default Sort",
        value = selected.name,
        options = SortOrder.entries.map { it.name },
        onSelected = { onChanged(SortOrder.valueOf(it)) },
    )
}

@Composable
private fun TimePeriodRow(selected: TimePeriod, onChanged: (TimePeriod) -> Unit) {
    DropdownSettingRow(
        label = "Default Period",
        value = selected.name,
        options = TimePeriod.entries.map { it.name },
        onSelected = { onChanged(TimePeriod.valueOf(it)) },
    )
}

@Composable
private fun DropdownSettingRow(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Column {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun GridColumnsRow(columns: Int, onChanged: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Grid Columns", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf(2, 3).forEach { count ->
                TextButton(onClick = { onChanged(count) }) {
                    Text(
                        text = count.toString(),
                        color = if (columns == count) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun HiddenModelsRow(
    count: Int,
    models: List<com.riox432.civitdeck.data.local.entity.HiddenModelEntity>,
    onUnhide: (Long) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    SettingsClickRow(label = "Hidden Models", detail = "$count models") { showDialog = true }
    if (showDialog) {
        HiddenModelsDialog(models = models, onUnhide = onUnhide, onDismiss = { showDialog = false })
    }
}

@Composable
private fun HiddenModelsDialog(
    models: List<com.riox432.civitdeck.data.local.entity.HiddenModelEntity>,
    onUnhide: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hidden Models") },
        text = {
            if (models.isEmpty()) {
                Text("No hidden models.\nLong-press a model card on the Search screen to hide it.")
            } else {
                Column {
                    models.forEach { model ->
                        HiddenModelItem(model.modelName) { onUnhide(model.modelId) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun HiddenModelItem(name: String, onUnhide: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onUnhide) { Text("Unhide") }
    }
}

@Composable
private fun ExcludedTagsRow(tags: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    SettingsClickRow(label = "Excluded Tags", detail = "${tags.size} tags") { showDialog = true }
    if (showDialog) {
        ExcludedTagsDialog(tags = tags, onAdd = onAdd, onRemove = onRemove, onDismiss = { showDialog = false })
    }
}

@Composable
private fun ExcludedTagsDialog(
    tags: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newTag by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluded Tags") },
        text = {
            Column {
                ExcludedTagInput(newTag, onValueChange = { newTag = it }) {
                    onAdd(newTag)
                    newTag = ""
                }
                if (tags.isEmpty()) {
                    Text(
                        "No excluded tags",
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                } else {
                    tags.forEach { tag ->
                        ExcludedTagItem(tag) { onRemove(tag) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ExcludedTagInput(value: String, onValueChange: (String) -> Unit, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Add tag") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onAdd, enabled = value.isNotBlank()) { Text("Add") }
    }
}

@Composable
private fun ExcludedTagItem(tag: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tag, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}

@Composable
private fun ClearActionRow(label: String, onConfirm: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    SettingsClickRow(label = label) { showDialog = true }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(label) },
            text = { Text("Are you sure? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm()
                    showDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SettingsClickRow(label: String, detail: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AccountSection(
    apiKey: String?,
    connectedUsername: String?,
    isValidating: Boolean,
    error: String?,
    onValidateAndSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (apiKey != null && connectedUsername != null) {
        ConnectedAccountRow(connectedUsername, onClear)
    } else {
        ApiKeyInputRow(isValidating, error, onValidateAndSave)
    }
}

@Composable
private fun ConnectedAccountRow(username: String, onClear: () -> Unit) {
    var showConfirmation by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Connected as",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(username, style = MaterialTheme.typography.bodyLarge)
        }
        TextButton(onClick = { showConfirmation = true }) {
            Text("Disconnect", color = MaterialTheme.colorScheme.error)
        }
    }
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Disconnect") },
            text = { Text("Remove your CivitAI API key?") },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    showConfirmation = false
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ApiKeyInputRow(
    isValidating: Boolean,
    error: String?,
    onValidateAndSave: (String) -> Unit,
) {
    var keyInput by rememberSaveable { mutableStateOf("") }
    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                placeholder = { Text("Paste API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = error != null,
                modifier = Modifier.weight(1f),
            )
            if (isValidating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                TextButton(
                    onClick = {
                        onValidateAndSave(keyInput)
                        keyInput = ""
                    },
                    enabled = keyInput.isNotBlank(),
                ) { Text("Verify") }
            }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Text(
            "Get your key at civitai.com/user/account",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

@Composable
private fun NavigationRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider()
}
