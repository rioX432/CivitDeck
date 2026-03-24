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
internal fun NsfwBlurSection(
    settings: NsfwBlurSettings,
    onSettingsChanged: (NsfwBlurSettings) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            "Blur Intensity",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Controls blur strength for NSFW images in the Image Gallery. " +
                "Tap any blurred image to reveal it temporarily.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BlurSliderRow("Soft", settings.softIntensity) {
            onSettingsChanged(settings.copy(softIntensity = it))
        }
        BlurSliderRow("Mature", settings.matureIntensity) {
            onSettingsChanged(settings.copy(matureIntensity = it))
        }
        BlurSliderRow("Explicit", settings.explicitIntensity) {
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
                text = if (intensity == 0) "Hidden" else "$intensity%",
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
    SettingsClickRow(label = "Hidden Models", detail = "$count models") { showDialog = true }
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
internal fun HiddenModelItem(name: String, onUnhide: () -> Unit) {
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
internal fun ExcludedTagsRow(
    tags: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    SettingsClickRow(label = "Excluded Tags", detail = "${tags.size} tags") { showDialog = true }
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
internal fun ExcludedTagInput(value: String, onValueChange: (String) -> Unit, onAdd: () -> Unit) {
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
internal fun ExcludedTagItem(tag: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tag, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}
