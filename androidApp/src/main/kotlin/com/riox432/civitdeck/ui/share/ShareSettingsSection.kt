package com.riox432.civitdeck.ui.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.ShareHashtag
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun ShareSettingsSection(
    hashtags: List<ShareHashtag>,
    onToggle: (String, Boolean) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var newTagInput by remember { mutableStateOf("") }

    Text(
        text = "Manage default hashtags for sharing generated images.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    )
    ShareHashtagChips(hashtags = hashtags, onToggle = onToggle, onRemove = onRemove)
    ShareHashtagAddRow(
        input = newTagInput,
        onInputChange = { newTagInput = it },
        onAdd = {
            if (newTagInput.isNotBlank()) {
                onAdd(newTagInput)
                newTagInput = ""
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShareHashtagChips(
    hashtags: List<ShareHashtag>,
    onToggle: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
    ) {
        hashtags.forEach { hashtag ->
            key(hashtag.tag) {
                FilterChip(
                    selected = hashtag.isEnabled,
                    onClick = { onToggle(hashtag.tag, !hashtag.isEnabled) },
                    label = { Text(hashtag.tag, style = MaterialTheme.typography.labelSmall) },
                    trailingIcon = {
                        IconButton(
                            onClick = { onRemove(hashtag.tag) },
                            modifier = Modifier.size(16.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ShareHashtagAddRow(
    input: String,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            label = { Text("Add hashtag") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() }),
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = "Add hashtag")
        }
    }
}
