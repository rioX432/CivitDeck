package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.HiddenModel
import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun NsfwToggleRow(level: NsfwFilterLevel, onToggle: (NsfwFilterLevel) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_nsfw_content), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.settings_nsfw_content_description),
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
internal fun NsfwBlurSection(
    settings: NsfwBlurSettings,
    onSettingsChanged: (NsfwBlurSettings) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            stringResource(R.string.settings_blur_intensity),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.settings_blur_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BlurSliderRow(stringResource(R.string.settings_blur_soft), settings.softIntensity) {
            onSettingsChanged(settings.copy(softIntensity = it))
        }
        BlurSliderRow(stringResource(R.string.settings_blur_mature), settings.matureIntensity) {
            onSettingsChanged(settings.copy(matureIntensity = it))
        }
        BlurSliderRow(stringResource(R.string.settings_blur_explicit), settings.explicitIntensity) {
            onSettingsChanged(settings.copy(explicitIntensity = it))
        }
    }
}

@Composable
internal fun BlurSliderRow(
    label: String,
    intensity: Int,
    onChanged: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (intensity == 0) stringResource(R.string.settings_blur_hidden) else "$intensity%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = intensity.toFloat(),
            onValueChange = { onChanged(it.toInt()) },
            valueRange = 0f..100f,
            steps = 3,
        )
    }
}

@Composable
internal fun HiddenModelsRow(
    count: Int,
    models: List<HiddenModel>,
    onUnhide: (Long) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    SettingsClickRow(
        label = stringResource(R.string.settings_hidden_models),
        detail = stringResource(R.string.settings_hidden_models_count, count)
    ) {
        showDialog = true
    }
    if (showDialog) {
        HiddenModelsDialog(models = models, onUnhide = onUnhide, onDismiss = { showDialog = false })
    }
}

@Composable
internal fun HiddenModelsDialog(
    models: List<HiddenModel>,
    onUnhide: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_hidden_models)) },
        text = {
            if (models.isEmpty()) {
                Text(stringResource(R.string.settings_no_hidden_models))
            } else {
                Column {
                    models.forEach { model ->
                        HiddenModelItem(model.modelName) { onUnhide(model.modelId) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_close)) } },
    )
}

@Composable
internal fun HiddenModelItem(name: String, onUnhide: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onUnhide) { Text(stringResource(R.string.settings_unhide)) }
    }
}

@Composable
internal fun ExcludedTagsRow(
    tags: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    SettingsClickRow(
        label = stringResource(R.string.settings_excluded_tags),
        detail = stringResource(R.string.settings_excluded_tags_count, tags.size)
    ) {
        showDialog = true
    }
    if (showDialog) {
        ExcludedTagsDialog(tags = tags, onAdd = onAdd, onRemove = onRemove, onDismiss = { showDialog = false })
    }
}

@Composable
internal fun ExcludedTagsDialog(
    tags: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newTag by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_excluded_tags)) },
        text = {
            Column {
                ExcludedTagInput(newTag, onValueChange = { newTag = it }) {
                    onAdd(newTag)
                    newTag = ""
                }
                if (tags.isEmpty()) {
                    Text(
                        stringResource(R.string.settings_no_excluded_tags),
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                } else {
                    tags.forEach { tag ->
                        ExcludedTagItem(tag) { onRemove(tag) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_close)) } },
    )
}

@Composable
internal fun ExcludedTagInput(value: String, onValueChange: (String) -> Unit, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(stringResource(R.string.settings_add_tag)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onAdd, enabled = value.isNotBlank()) { Text(stringResource(R.string.settings_add)) }
    }
}

@Composable
internal fun ExcludedTagItem(tag: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tag, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onRemove) { Text(stringResource(R.string.settings_remove)) }
    }
}
