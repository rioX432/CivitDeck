@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.comfyui.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.riox432.civitdeck.domain.model.TemplateVariable
import com.riox432.civitdeck.domain.model.TemplateVariableType
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.domain.model.WorkflowTemplateCategory
import com.riox432.civitdeck.domain.model.WorkflowTemplateType
import com.riox432.civitdeck.ui.theme.Elevation
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun DesktopWorkflowTemplateEditorScreen(
    initialTemplate: WorkflowTemplate,
    viewModel: DesktopWorkflowTemplateViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(initialTemplate.name) }
    var description by remember { mutableStateOf(initialTemplate.description) }
    var type by remember { mutableStateOf(initialTemplate.type) }
    var category by remember { mutableStateOf(initialTemplate.category) }
    var variables by remember { mutableStateOf(initialTemplate.variables) }

    Surface(modifier = modifier.fillMaxSize()) {
        Column {
            EditorToolbar(
                isNew = initialTemplate.id == 0L,
                onBack = onBack,
                canSave = name.isNotBlank(),
                onSave = {
                    viewModel.onSaveTemplate(
                        initialTemplate.copy(
                            name = name.trim(),
                            description = description.trim(),
                            type = type,
                            category = category,
                            variables = variables,
                        ),
                    )
                    onBack()
                },
            )
            EditorContent(
                name = name,
                description = description,
                type = type,
                category = category,
                variables = variables,
                onNameChange = { name = it },
                onDescriptionChange = { description = it },
                onTypeChange = { newType ->
                    type = newType
                    variables = DesktopWorkflowTemplateViewModel.defaultVariablesFor(newType)
                },
                onCategoryChange = { category = it },
                onVariablesChange = { variables = it },
            )
        }
    }
}

@Composable
private fun EditorToolbar(
    isNew: Boolean,
    onBack: () -> Unit,
    canSave: Boolean,
    onSave: () -> Unit,
) {
    Surface(tonalElevation = Elevation.xs) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (isNew) "Create Template" else "Edit Template",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = Spacing.sm),
            )
            TextButton(onClick = onSave, enabled = canSave) { Text("Save") }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun EditorContent(
    name: String,
    description: String,
    type: WorkflowTemplateType,
    category: WorkflowTemplateCategory,
    variables: List<TemplateVariable>,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTypeChange: (WorkflowTemplateType) -> Unit,
    onCategoryChange: (WorkflowTemplateCategory) -> Unit,
    onVariablesChange: (List<TemplateVariable>) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Template Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                TypeSelector(type = type, onTypeChange = onTypeChange)
                CategorySelector(category = category, onCategoryChange = onCategoryChange)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Variables",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        onVariablesChange(variables + newVariable(variables.size))
                    },
                ) {
                    Icon(Icons.Default.Add, "Add variable")
                }
            }
        }
        itemsIndexed(variables, key = { _, v -> v.name }) { index, variable ->
            VariableEditor(
                variable = variable,
                onUpdate = { updated ->
                    onVariablesChange(
                        variables.toMutableList().also { it[index] = updated },
                    )
                },
                onDelete = {
                    onVariablesChange(
                        variables.toMutableList().also { it.removeAt(index) },
                    )
                },
            )
        }
    }
}

private fun newVariable(size: Int) = TemplateVariable(
    name = "var_${size + 1}",
    type = TemplateVariableType.TEXT,
    defaultValue = "",
)

@Composable
private fun TypeSelector(
    type: WorkflowTemplateType,
    onTypeChange: (WorkflowTemplateType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("Type", style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = { expanded = true }) { Text(typeLabel(type)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WorkflowTemplateType.entries.forEach { t ->
                DropdownMenuItem(
                    text = { Text(typeLabel(t)) },
                    onClick = { onTypeChange(t); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun CategorySelector(
    category: WorkflowTemplateCategory,
    onCategoryChange: (WorkflowTemplateCategory) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("Category", style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = { expanded = true }) { Text(categoryLabel(category)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WorkflowTemplateCategory.entries.forEach { c ->
                DropdownMenuItem(
                    text = { Text(categoryLabel(c)) },
                    onClick = { onCategoryChange(c); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun VariableEditor(
    variable: TemplateVariable,
    onUpdate: (TemplateVariable) -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = variable.name,
                    onValueChange = { onUpdate(variable.copy(name = it)) },
                    label = { Text("Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete, "Remove",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            OutlinedTextField(
                value = variable.label,
                onValueChange = { onUpdate(variable.copy(label = it)) },
                label = { Text("Display Label") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            VariableTypeAndDefault(variable = variable, onUpdate = onUpdate)
            if (variable.type == TemplateVariableType.SLIDER) {
                SliderRangeRow(variable = variable, onUpdate = onUpdate)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Required",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = variable.required,
                    onCheckedChange = { onUpdate(variable.copy(required = it)) },
                )
            }
        }
    }
}

@Composable
private fun VariableTypeAndDefault(
    variable: TemplateVariable,
    onUpdate: (TemplateVariable) -> Unit,
) {
    var typeMenuExpanded by remember { mutableStateOf(false) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Type", style = MaterialTheme.typography.labelSmall)
            TextButton(onClick = { typeMenuExpanded = true }) { Text(variable.type.name) }
            DropdownMenu(
                expanded = typeMenuExpanded,
                onDismissRequest = { typeMenuExpanded = false },
            ) {
                TemplateVariableType.entries.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(t.name) },
                        onClick = { onUpdate(variable.copy(type = t)); typeMenuExpanded = false },
                    )
                }
            }
        }
        OutlinedTextField(
            value = variable.defaultValue,
            onValueChange = { onUpdate(variable.copy(defaultValue = it)) },
            label = { Text("Default") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
    }
}

@Composable
private fun SliderRangeRow(
    variable: TemplateVariable,
    onUpdate: (TemplateVariable) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = variable.min?.toString() ?: "",
            onValueChange = { onUpdate(variable.copy(min = it.toDoubleOrNull())) },
            label = { Text("Min") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        OutlinedTextField(
            value = variable.max?.toString() ?: "",
            onValueChange = { onUpdate(variable.copy(max = it.toDoubleOrNull())) },
            label = { Text("Max") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        OutlinedTextField(
            value = variable.step?.toString() ?: "",
            onValueChange = { onUpdate(variable.copy(step = it.toDoubleOrNull())) },
            label = { Text("Step") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
    }
}
