@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.domain.model.TemplateVariable
import com.riox432.civitdeck.domain.model.TemplateVariableType
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.domain.model.WorkflowTemplateCategory
import com.riox432.civitdeck.domain.model.WorkflowTemplateType
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowTemplateEditorScreen(
    initialTemplate: WorkflowTemplate,
    viewModel: WorkflowTemplateViewModel,
    onBack: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialTemplate.name) }
    var description by rememberSaveable { mutableStateOf(initialTemplate.description) }
    var type by rememberSaveable { mutableStateOf(initialTemplate.type) }
    var category by rememberSaveable { mutableStateOf(initialTemplate.category) }
    var variables by remember { mutableStateOf(initialTemplate.variables) }

    Scaffold(
        topBar = {
            EditorTopBar(
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
        },
    ) { padding ->
        EditorContent(
            modifier = Modifier.padding(padding),
            name = name,
            description = description,
            type = type,
            category = category,
            variables = variables,
            onNameChange = { name = it },
            onDescriptionChange = { description = it },
            onTypeChange = { newType ->
                type = newType
                variables = WorkflowTemplateViewModel.defaultVariablesFor(newType)
            },
            onCategoryChange = { category = it },
            onVariablesChange = { variables = it },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    isNew: Boolean,
    onBack: () -> Unit,
    canSave: Boolean,
    onSave: () -> Unit,
) {
    TopAppBar(
        title = { Text(if (isNew) "Create Template" else "Edit Template") },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        },
        actions = {
            TextButton(onClick = onSave, enabled = canSave) { Text("Save") }
        },
    )
}

@Suppress("LongParameterList")
@Composable
private fun EditorContent(
    modifier: Modifier,
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
        modifier = modifier,
        contentPadding = PaddingValues(Spacing.md),
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                TypeSelector(type = type, onTypeChange = onTypeChange)
                CategorySelector(category = category, onCategoryChange = onCategoryChange)
            }
        }
        item {
            VariablesHeader(onAdd = { onVariablesChange(variables + newVariable(variables.size)) })
        }
        itemsIndexed(variables, key = { _, variable -> variable.name }) { index, variable ->
            VariableEditor(
                variable = variable,
                onUpdate = { updated ->
                    onVariablesChange(variables.toMutableList().also { it[index] = updated })
                },
                onDelete = {
                    onVariablesChange(variables.toMutableList().also { it.removeAt(index) })
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
private fun VariablesHeader(onAdd: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Variables", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Add variable") }
    }
}

@Composable
private fun TypeSelector(type: WorkflowTemplateType, onTypeChange: (WorkflowTemplateType) -> Unit) {
    var typeMenuExpanded by remember { mutableStateOf(false) }
    Column {
        Text("Type", style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = { typeMenuExpanded = true }) { Text(typeLabel(type)) }
        DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
            WorkflowTemplateType.entries.forEach { t ->
                DropdownMenuItem(
                    text = { Text(typeLabel(t)) },
                    onClick = {
                        onTypeChange(t)
                        typeMenuExpanded = false
                    },
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
                    onClick = {
                        onCategoryChange(c)
                        expanded = false
                    },
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
            VariableNameRow(variable = variable, onUpdate = onUpdate, onDelete = onDelete)
            VariableLabelRow(variable = variable, onUpdate = onUpdate)
            VariableTypeAndDefault(variable = variable, onUpdate = onUpdate)
            if (variable.type == TemplateVariableType.SLIDER) {
                SliderRangeRow(variable = variable, onUpdate = onUpdate)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Required", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Switch(checked = variable.required, onCheckedChange = { onUpdate(variable.copy(required = it)) })
            }
        }
    }
}

@Composable
private fun VariableNameRow(
    variable: TemplateVariable,
    onUpdate: (TemplateVariable) -> Unit,
    onDelete: () -> Unit,
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
            Icon(Icons.Default.Delete, "Remove variable", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun VariableLabelRow(
    variable: TemplateVariable,
    onUpdate: (TemplateVariable) -> Unit,
) {
    OutlinedTextField(
        value = variable.label,
        onValueChange = { onUpdate(variable.copy(label = it)) },
        label = { Text("Display Label") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
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
            DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                TemplateVariableType.entries.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(t.name) },
                        onClick = {
                            onUpdate(variable.copy(type = t))
                            typeMenuExpanded = false
                        },
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

private fun typeLabel(type: WorkflowTemplateType) = when (type) {
    WorkflowTemplateType.TXT2IMG -> "txt2img"
    WorkflowTemplateType.IMG2IMG -> "img2img"
    WorkflowTemplateType.INPAINTING -> "Inpainting"
    WorkflowTemplateType.UPSCALE -> "Upscale"
    WorkflowTemplateType.LORA -> "LoRA"
}

private fun categoryLabel(category: WorkflowTemplateCategory) = when (category) {
    WorkflowTemplateCategory.GENERAL -> "General"
    WorkflowTemplateCategory.ANIME -> "Anime"
    WorkflowTemplateCategory.PHOTOREALISTIC -> "Photo"
    WorkflowTemplateCategory.ARTISTIC -> "Artistic"
    WorkflowTemplateCategory.UTILITY -> "Utility"
}
