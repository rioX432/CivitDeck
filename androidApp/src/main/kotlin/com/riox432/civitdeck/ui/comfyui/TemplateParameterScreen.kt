@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.TemplateVariable
import com.riox432.civitdeck.domain.model.TemplateVariableType
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.ui.theme.Spacing
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateParameterScreen(
    template: WorkflowTemplate,
    onBack: () -> Unit,
    onApply: (Map<String, String>) -> Unit,
) {
    val values = remember {
        mutableStateMapOf<String, String>().apply {
            template.variables.forEach { v -> put(v.name, v.defaultValue) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(template.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (template.description.isNotBlank()) {
                item {
                    Text(
                        template.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(template.variables, key = { it.name }) { variable ->
                ParameterInput(
                    variable = variable,
                    value = values[variable.name] ?: variable.defaultValue,
                    onValueChange = { values[variable.name] = it },
                )
            }
            item {
                Button(
                    onClick = { onApply(values.toMap()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = template.variables
                        .filter { it.required }
                        .all { (values[it.name] ?: "").isNotBlank() },
                ) {
                    Text("Generate")
                }
            }
        }
    }
}

@Composable
private fun ParameterInput(
    variable: TemplateVariable,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                variable.label.ifBlank { variable.name },
                style = MaterialTheme.typography.titleSmall,
            )
            if (variable.description.isNotBlank()) {
                Text(
                    variable.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (variable.type) {
                TemplateVariableType.SLIDER -> SliderInput(variable, value, onValueChange)
                TemplateVariableType.SELECT -> SelectInput(variable, value, onValueChange)
                TemplateVariableType.NUMBER -> NumberInput(variable, value, onValueChange)
                TemplateVariableType.TEXT -> TextInput(variable, value, onValueChange)
            }
        }
    }
}

@Composable
private fun SliderInput(
    variable: TemplateVariable,
    value: String,
    onValueChange: (String) -> Unit,
) {
    val min = variable.min?.toFloat() ?: 0f
    val max = variable.max?.toFloat() ?: 100f
    val step = variable.step?.toFloat() ?: 1f
    val current = value.toFloatOrNull()?.coerceIn(min, max) ?: min

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatSliderValue(min, step), style = MaterialTheme.typography.bodySmall)
            Text(
                formatSliderValue(current, step),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(formatSliderValue(max, step), style = MaterialTheme.typography.bodySmall)
        }
        val steps = if (step > 0f && max > min) {
            ((max - min) / step).roundToInt() - 1
        } else {
            0
        }
        Slider(
            value = current,
            onValueChange = { onValueChange(formatSliderValue(it, step)) },
            valueRange = min..max,
            steps = steps.coerceAtLeast(0),
        )
    }
}

@Composable
private fun SelectInput(
    variable: TemplateVariable,
    value: String,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        TextButton(onClick = { expanded = true }) {
            Text(value.ifBlank { "Select..." })
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            variable.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun NumberInput(
    variable: TemplateVariable,
    value: String,
    onValueChange: (String) -> Unit,
) {
    // If has min/max, show as slider instead
    if (variable.min != null && variable.max != null) {
        SliderInput(variable, value, onValueChange)
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Composable
private fun TextInput(
    variable: TemplateVariable,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        minLines = if (variable.name.contains("prompt")) 3 else 1,
        maxLines = if (variable.name.contains("prompt")) 6 else 1,
        placeholder = {
            if (variable.required) Text("Required") else Text("Optional")
        },
    )
}

private fun formatSliderValue(value: Float, step: Float): String {
    return if (step >= 1f) {
        value.roundToInt().toString()
    } else {
        val decimals = when {
            step >= 0.1f -> 1
            step >= 0.01f -> 2
            else -> 3
        }
        "%.${decimals}f".format(value)
    }
}
