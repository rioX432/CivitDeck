package com.riox432.civitdeck.ui.comfyui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.riox432.civitdeck.R
import com.riox432.civitdeck.feature.comfyui.domain.model.ExtractedParameter
import com.riox432.civitdeck.feature.comfyui.domain.model.ParameterType
import com.riox432.civitdeck.ui.theme.Spacing
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowParameterSheet(
    parameters: List<ExtractedParameter>,
    onParameterChanged: (nodeId: String, paramName: String, newValue: String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        ParameterSheetContent(parameters, onParameterChanged, onRefresh)
    }
}

@Composable
private fun ParameterSheetContent(
    parameters: List<ExtractedParameter>,
    onParameterChanged: (nodeId: String, paramName: String, newValue: String) -> Unit,
    onRefresh: () -> Unit,
) {
    // Group parameters by nodeId + nodeTitle
    val grouped = remember(parameters) {
        parameters.groupBy { it.nodeId to it.nodeTitle }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            SheetHeader(onRefresh)
        }
        grouped.forEach { (nodeKey, nodeParams) ->
            item(key = nodeKey.first) {
                NodeSection(
                    nodeId = nodeKey.first,
                    nodeTitle = nodeKey.second,
                    classType = nodeParams.first().nodeClassType,
                    parameters = nodeParams,
                    onParameterChanged = onParameterChanged,
                )
            }
        }
    }
}

@Composable
private fun SheetHeader(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Workflow Parameters",
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = onRefresh) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.cd_refresh_parameters),
            )
        }
    }
}

@Composable
private fun NodeSection(
    nodeId: String,
    nodeTitle: String,
    classType: String,
    parameters: List<ExtractedParameter>,
    onParameterChanged: (nodeId: String, paramName: String, newValue: String) -> Unit,
) {
    val expandedNodes = remember { mutableStateMapOf<String, Boolean>() }
    val isExpanded = expandedNodes.getOrPut(nodeId) { true }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            NodeSectionHeader(
                nodeTitle = nodeTitle,
                classType = classType,
                isExpanded = isExpanded,
                onToggle = { expandedNodes[nodeId] = !isExpanded },
            )
            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    parameters.forEach { param ->
                        ParameterWidget(param, onParameterChanged)
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeSectionHeader(
    nodeTitle: String,
    classType: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(nodeTitle, style = MaterialTheme.typography.labelLarge)
            Text(
                classType,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
            )
        }
    }
}

@Composable
private fun ParameterWidget(
    param: ExtractedParameter,
    onParameterChanged: (nodeId: String, paramName: String, newValue: String) -> Unit,
) {
    when (param.paramType) {
        ParameterType.TEXT -> TextParameterWidget(param, onParameterChanged)
        ParameterType.NUMBER -> NumberParameterWidget(param, onParameterChanged)
        ParameterType.SELECT -> SelectParameterWidget(param, onParameterChanged)
        ParameterType.SEED -> SeedParameterWidget(param, onParameterChanged)
    }
}

@Composable
private fun TextParameterWidget(
    param: ExtractedParameter,
    onParameterChanged: (nodeId: String, paramName: String, newValue: String) -> Unit,
) {
    OutlinedTextField(
        value = param.currentValue,
        onValueChange = { onParameterChanged(param.nodeId, param.paramName, it) },
        label = { Text(param.paramName) },
        modifier = Modifier.fillMaxWidth(),
        minLines = if (param.paramName == "text") 3 else 1,
        maxLines = if (param.paramName == "text") 8 else 3,
    )
}

@Composable
private fun NumberParameterWidget(
    param: ExtractedParameter,
    onParameterChanged: (nodeId: String, paramName: String, newValue: String) -> Unit,
) {
    val min = param.min
    val max = param.max
    if (min != null && max != null && max > min) {
        NumberSliderWidget(param, min, max, onParameterChanged)
    } else {
        NumberFieldWidget(param, onParameterChanged)
    }
}

@Composable
private fun NumberSliderWidget(
    param: ExtractedParameter,
    min: Double,
    max: Double,
    onParameterChanged: (nodeId: String, paramName: String, newValue: String) -> Unit,
) {
    val currentFloat = param.currentValue.toFloatOrNull() ?: min.toFloat()
    Column {
        Text(
            "${param.paramName}: ${formatNumber(param.currentValue)}",
            style = MaterialTheme.typography.bodySmall,
        )
        Slider(
            value = currentFloat.coerceIn(min.toFloat(), max.toFloat()),
            onValueChange = { newVal ->
                val step = param.step
                val formatted = if (step != null && step >= 1.0) {
                    newVal.toInt().toString()
                } else {
                    "%.2f".format(newVal)
                }
                onParameterChanged(param.nodeId, param.paramName, formatted)
            },
            valueRange = min.toFloat()..max.toFloat(),
            steps = calculateSteps(min, max, param.step),
        )
    }
}

@Composable
private fun NumberFieldWidget(
    param: ExtractedParameter,
    onParameterChanged: (nodeId: String, paramName: String, newValue: String) -> Unit,
) {
    OutlinedTextField(
        value = param.currentValue,
        onValueChange = { onParameterChanged(param.nodeId, param.paramName, it) },
        label = { Text(param.paramName) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

@Composable
private fun SelectParameterWidget(
    param: ExtractedParameter,
    onParameterChanged: (nodeId: String, paramName: String, newValue: String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(param.paramName, style = MaterialTheme.typography.bodySmall)
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                param.currentValue.ifBlank { "Select..." },
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            param.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.substringAfterLast('/'), maxLines = 1) },
                    onClick = {
                        onParameterChanged(param.nodeId, param.paramName, option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SeedParameterWidget(
    param: ExtractedParameter,
    onParameterChanged: (nodeId: String, paramName: String, newValue: String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = param.currentValue,
            onValueChange = { onParameterChanged(param.nodeId, param.paramName, it) },
            label = { Text(param.paramName) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        IconButton(onClick = {
            val randomSeed = Random.nextLong(0, Long.MAX_VALUE)
            onParameterChanged(param.nodeId, param.paramName, randomSeed.toString())
        }) {
            Icon(
                Icons.Default.Casino,
                contentDescription = stringResource(R.string.cd_randomize_seed),
            )
        }
    }
}

private fun formatNumber(value: String): String {
    val doubleVal = value.toDoubleOrNull() ?: return value
    return if (doubleVal == doubleVal.toLong().toDouble()) {
        doubleVal.toLong().toString()
    } else {
        "%.2f".format(doubleVal)
    }
}

private fun calculateSteps(min: Double, max: Double, step: Double?): Int {
    if (step == null || step <= 0) return 0
    val range = max - min
    if (range <= 0) return 0
    val numSteps = (range / step).toInt()
    // Limit to avoid performance issues with very fine-grained sliders
    return numSteps.coerceAtMost(MAX_SLIDER_STEPS) - 1
}

private const val MAX_SLIDER_STEPS = 1000
