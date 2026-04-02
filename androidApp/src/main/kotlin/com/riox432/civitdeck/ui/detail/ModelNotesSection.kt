package com.riox432.civitdeck.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.ModelNote
import com.riox432.civitdeck.domain.model.PersonalTag
import com.riox432.civitdeck.ui.components.SectionHeader
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun ModelNotesSection(
    note: ModelNote?,
    onSaveNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(note) { mutableStateOf(note?.noteText ?: "") }

    Column(modifier = modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        NoteHeader(
            hasNote = note != null,
            isEditing = isEditing,
            onToggleEdit = { isEditing = !isEditing },
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        if (isEditing) {
            NoteEditor(
                text = editText,
                onTextChange = { editText = it },
                onSave = {
                    onSaveNote(editText)
                    isEditing = false
                },
                onCancel = {
                    editText = note?.noteText ?: ""
                    isEditing = false
                },
            )
        } else {
            NoteDisplay(noteText = note?.noteText, onEdit = { isEditing = true })
        }
    }
}

@Composable
private fun NoteHeader(
    hasNote: Boolean,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionHeader(title = "My Notes", showDivider = true, modifier = Modifier.weight(1f))
        if (!isEditing) {
            IconButton(onClick = onToggleEdit) {
                Icon(
                    imageVector = if (hasNote) Icons.Default.Edit else Icons.Default.Add,
                    contentDescription = if (hasNote) "Edit note" else "Add note",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun NoteDisplay(noteText: String?, onEdit: () -> Unit) {
    if (noteText.isNullOrBlank()) {
        Text(
            text = "Tap + to add a personal note",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable(onClickLabel = "Edit note", onClick = onEdit),
        )
    } else {
        Text(
            text = noteText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun NoteEditor(
    text: String,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("e.g. Works great with X LoRA") },
        minLines = 2,
        maxLines = 5,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onCancel) { Text("Cancel") }
        TextButton(onClick = onSave) { Text("Save") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalTagsSection(
    tags: List<PersonalTag>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddField by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }

    Column(modifier = modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        TagsHeader(onAdd = { showAddField = !showAddField })
        Spacer(modifier = Modifier.height(Spacing.sm))
        TagChips(tags = tags, onRemoveTag = onRemoveTag)
        if (showAddField) {
            TagInput(
                text = newTagText,
                onTextChange = { newTagText = it },
                onSubmit = {
                    onAddTag(newTagText)
                    newTagText = ""
                },
            )
        }
        if (tags.isEmpty() && !showAddField) {
            Text(
                text = "Tap + to add personal tags",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TagsHeader(onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionHeader(title = "My Tags", showDivider = true, modifier = Modifier.weight(1f))
        IconButton(onClick = onAdd) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_add_tag),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChips(tags: List<PersonalTag>, onRemoveTag: (String) -> Unit) {
    if (tags.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        tags.forEach { tag ->
            InputChip(
                selected = false,
                onClick = { onRemoveTag(tag.tag) },
                label = { Text(tag.tag, style = MaterialTheme.typography.labelSmall) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_remove),
                        modifier = Modifier.size(14.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun TagInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Spacer(modifier = Modifier.height(Spacing.sm))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Tag name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onSubmit() }),
        )
        AssistChip(
            onClick = { if (text.isNotBlank()) onSubmit() },
            label = { Text("Add") },
        )
    }
}
