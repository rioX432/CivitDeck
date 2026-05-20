package com.riox432.civitdeck.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.riox432.civitdeck.feature.comfyui.domain.model.ExtractedParameter
import com.riox432.civitdeck.feature.comfyui.domain.model.ParameterType
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.GenerationUiState
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun DesktopCustomWorkflowSection(
    state: GenerationUiState,
    viewModel: ComfyUIGenerationViewModel,
) {
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showParams by remember { mutableStateOf(false) }

    Text("Custom Workflow", style = MaterialTheme.typography.labelMedium)
    Spacer(modifier = Modifier.height(Spacing.xs))
    val customJson = state.customWorkflowJson
    if (customJson != null) {
        WorkflowLoadedContent(
            customJsonLength = customJson.length,
            extractedParameters = state.extractedParameters,
            isLoadingParameters = state.isLoadingParameters,
            showParams = showParams,
            onToggleParams = { showParams = !showParams },
            onClearWorkflow = viewModel::onClearCustomWorkflow,
            viewModel = viewModel,
        )
    } else {
        OutlinedButton(onClick = { showImport = !showImport }) {
            Text(if (showImport) "Cancel Import" else "Import Workflow JSON")
        }
    }
    state.workflowImportError?.let { err ->
        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    if (showImport && customJson == null) {
        WorkflowImportForm(
            importText = importText,
            onImportTextChanged = { importText = it },
            onImport = {
                viewModel.onImportWorkflow(importText)
                showImport = false
            },
        )
    }
}

@Composable
private fun WorkflowLoadedContent(
    customJsonLength: Int,
    extractedParameters: List<ExtractedParameter>,
    isLoadingParameters: Boolean,
    showParams: Boolean,
    onToggleParams: () -> Unit,
    onClearWorkflow: () -> Unit,
    viewModel: ComfyUIGenerationViewModel,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Workflow loaded ($customJsonLength chars)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        TextButton(onClick = onClearWorkflow) {
            Text("Clear")
        }
    }
    if (extractedParameters.isNotEmpty()) {
        OutlinedButton(onClick = onToggleParams) {
            Text(
                if (showParams) "Hide Parameters" else "Edit Parameters (${extractedParameters.size})",
            )
        }
    } else if (isLoadingParameters) {
        Text(
            "Loading parameters...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    AnimatedVisibility(visible = showParams && extractedParameters.isNotEmpty()) {
        DesktopParameterEditor(extractedParameters, viewModel)
    }
}

@Composable
private fun WorkflowImportForm(
    importText: String,
    onImportTextChanged: (String) -> Unit,
    onImport: () -> Unit,
) {
    Spacer(modifier = Modifier.height(Spacing.sm))
    OutlinedTextField(
        value = importText,
        onValueChange = onImportTextChanged,
        label = { Text("Workflow JSON") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 4,
        maxLines = 8,
    )
    Spacer(modifier = Modifier.height(Spacing.xs))
    Button(onClick = onImport) {
        Text("Import")
    }
}

@Composable
private fun DesktopParameterEditor(
    parameters: List<ExtractedParameter>,
    viewModel: ComfyUIGenerationViewModel,
) {
    val grouped = remember(parameters) {
        parameters.groupBy { it.nodeId to it.nodeTitle }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Spacer(modifier = Modifier.height(Spacing.xs))
        grouped.forEach { (nodeKey, nodeParams) ->
            DesktopNodeParamGroup(nodeKey.second, nodeParams, viewModel)
        }
    }
}

@Composable
private fun DesktopNodeParamGroup(
    nodeTitle: String,
    parameters: List<ExtractedParameter>,
    viewModel: ComfyUIGenerationViewModel,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(nodeTitle, style = MaterialTheme.typography.labelMedium)
            parameters.forEach { param ->
                DesktopParamWidget(param, viewModel::onParameterValueChanged)
            }
        }
    }
}

@Composable
private fun DesktopParamWidget(
    param: ExtractedParameter,
    onChanged: (String, String, String) -> Unit,
) {
    when (param.paramType) {
        ParameterType.TEXT -> ParamTextInput(param, onChanged)
        ParameterType.NUMBER -> ParamNumberInput(param, onChanged)
        ParameterType.SELECT -> DesktopParamDropdown(param, onChanged)
        ParameterType.SEED -> ParamSeedInput(param, onChanged)
    }
}

@Composable
private fun ParamTextInput(
    param: ExtractedParameter,
    onChanged: (String, String, String) -> Unit,
) {
    OutlinedTextField(
        value = param.currentValue,
        onValueChange = { onChanged(param.nodeId, param.paramName, it) },
        label = { Text(param.paramName) },
        modifier = Modifier.fillMaxWidth(),
        minLines = if (param.paramName == "text") 2 else 1,
        maxLines = if (param.paramName == "text") 6 else 2,
    )
}

@Composable
private fun ParamNumberInput(
    param: ExtractedParameter,
    onChanged: (String, String, String) -> Unit,
) {
    val min = param.min
    val max = param.max
    if (min != null && max != null && max > min) {
        DesktopNumberSlider(param, min, max, onChanged)
    } else {
        OutlinedTextField(
            value = param.currentValue,
            onValueChange = { onChanged(param.nodeId, param.paramName, it) },
            label = { Text(param.paramName) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}

@Composable
private fun ParamSeedInput(
    param: ExtractedParameter,
    onChanged: (String, String, String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = param.currentValue,
            onValueChange = { onChanged(param.nodeId, param.paramName, it) },
            label = { Text(param.paramName) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedButton(onClick = {
            val seed = kotlin.random.Random.nextLong(0, Long.MAX_VALUE)
            onChanged(param.nodeId, param.paramName, seed.toString())
        }) {
            Text("Random")
        }
    }
}

@Composable
private fun DesktopNumberSlider(
    param: ExtractedParameter,
    min: Double,
    max: Double,
    onChanged: (String, String, String) -> Unit,
) {
    val currentFloat = param.currentValue.toFloatOrNull() ?: min.toFloat()
    val step = param.step
    val isInteger = step != null && step >= 1.0
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(param.paramName, style = MaterialTheme.typography.bodySmall)
            Text(
                if (isInteger) currentFloat.toInt().toString() else "%.2f".format(currentFloat),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Slider(
            value = currentFloat.coerceIn(min.toFloat(), max.toFloat()),
            onValueChange = { newVal ->
                val formatted = if (isInteger) newVal.toInt().toString() else "%.2f".format(newVal)
                onChanged(param.nodeId, param.paramName, formatted)
            },
            valueRange = min.toFloat()..max.toFloat(),
        )
    }
}

@Composable
private fun DesktopParamDropdown(
    param: ExtractedParameter,
    onChanged: (String, String, String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(param.paramName, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { expanded = true }) {
            Text(param.currentValue.ifBlank { "Select..." }, maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            param.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.substringAfterLast('/'), maxLines = 1) },
                    onClick = {
                        onChanged(param.nodeId, param.paramName, option)
                        expanded = false
                    },
                )
            }
        }
    }
}
