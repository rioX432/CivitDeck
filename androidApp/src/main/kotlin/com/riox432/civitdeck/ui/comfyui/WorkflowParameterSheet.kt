@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.R
import com.riox432.civitdeck.feature.comfyui.domain.model.ExtractedParameter
import com.riox432.civitdeck.feature.comfyui.domain.model.ParameterType
import com.riox432.civitdeck.ui.components.SectionHeader
import com.riox432.civitdeck.ui.theme.Spacing
import kotlin.random.Random

private const val IMAGE_PREVIEW_SIZE = 64
private const val MAX_SLIDER_STEPS = 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowParameterSheet(
    parameters: List<ExtractedParameter>,
    onParameterChanged: (nodeId: String, paramName: String, newValue: String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    onImagePickRequested: ((nodeId: String, paramName: String) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        ParameterSheetContent(
            parameters = parameters,
            onParameterChanged = onParameterChanged,
            onRefresh = onRefresh,
            onImagePickRequested = onImagePickRequested,
        )
    }
}

@Composable
private fun ParameterSheetContent(
    parameters: List<ExtractedParameter>,
    onParameterChanged: (nodeId: String, paramName: String, newValue: String) -> Unit,
    onRefresh: () -> Unit,
    onImagePickRequested: ((nodeId: String, paramName: String) -> Unit)?,
) {
    val (grouped, advancedParams) = remember(parameters) {
        buildGroupedParameters(parameters)
    }
    var showAdvanced by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item { SheetHeader(onRefresh) }
        renderGroupedSections(grouped, onParameterChanged, onImagePickRequested)
        if (advancedParams.isNotEmpty()) {
            renderAdvancedSection(advancedParams, showAdvanced, onParameterChanged, onImagePickRequested) {
                showAdvanced = !showAdvanced
            }
        }
    }
}

/**
 * Splits parameters into grouped (APP mode) sections and advanced (ungrouped) sections.
 * If no groups are present, all parameters go into node-based groups.
 */
private fun buildGroupedParameters(
    parameters: List<ExtractedParameter>,
): Pair<List<ParameterGroup>, List<ExtractedParameter>> {
    val hasGroups = parameters.any { it.group != null }
    if (!hasGroups) {
        // Legacy: group by node, no advanced section
        val groups = parameters
            .groupBy { it.nodeId to it.nodeTitle }
            .map { (key, params) -> ParameterGroup(key.second, params.sortedBy { it.order }) }
        return groups to emptyList()
    }
    // APP mode: grouped params go into named sections, ungrouped become "advanced"
    val grouped = parameters.filter { it.group != null }
        .groupBy { it.group!! }
        .map { (name, params) -> ParameterGroup(name, params.sortedBy { it.order }) }
    val advanced = parameters.filter { it.group == null }
    return grouped to advanced
}

private data class ParameterGroup(
    val title: String,
    val parameters: List<ExtractedParameter>,
)

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

// Extension functions on LazyListScope for rendering sections
private fun androidx.compose.foundation.lazy.LazyListScope.renderGroupedSections(
    groups: List<ParameterGroup>,
    onParameterChanged: (String, String, String) -> Unit,
    onImagePickRequested: ((String, String) -> Unit)?,
) {
    groups.forEach { group ->
        item(key = "group_${group.title}") {
            GroupSection(group, onParameterChanged, onImagePickRequested)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.renderAdvancedSection(
    advancedParams: List<ExtractedParameter>,
    showAdvanced: Boolean,
    onParameterChanged: (String, String, String) -> Unit,
    onImagePickRequested: ((String, String) -> Unit)?,
    onToggle: () -> Unit,
) {
    item(key = "advanced_header") {
        AdvancedSectionHeader(isExpanded = showAdvanced, onToggle = onToggle)
    }
    if (showAdvanced) {
        item(key = "advanced_content") {
            AdvancedSectionContent(advancedParams, onParameterChanged, onImagePickRequested)
        }
    }
}

@Composable
private fun GroupSection(
    group: ParameterGroup,
    onParameterChanged: (String, String, String) -> Unit,
    onImagePickRequested: ((String, String) -> Unit)?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            SectionHeader(title = group.title, showDivider = false)
            Column(
                modifier = Modifier.padding(top = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                group.parameters.forEach { param ->
                    ParameterWidget(param, onParameterChanged, onImagePickRequested)
                }
            }
        }
    }
}

@Composable
private fun AdvancedSectionHeader(isExpanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.label_advanced_parameters),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onToggle) {
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) {
                    stringResource(R.string.cd_collapse_section)
                } else {
                    stringResource(R.string.cd_expand_section)
                },
            )
        }
    }
}

@Composable
private fun AdvancedSectionContent(
    params: List<ExtractedParameter>,
    onParameterChanged: (String, String, String) -> Unit,
    onImagePickRequested: ((String, String) -> Unit)?,
) {
    // Group advanced params by node
    val nodeGroups = params.groupBy { it.nodeId to it.nodeTitle }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        nodeGroups.forEach { (nodeKey, nodeParams) ->
            NodeSection(
                nodeTitle = nodeKey.second,
                parameters = nodeParams,
                onParameterChanged = onParameterChanged,
                onImagePickRequested = onImagePickRequested,
            )
        }
    }
}

@Composable
private fun NodeSection(
    nodeTitle: String,
    parameters: List<ExtractedParameter>,
    onParameterChanged: (String, String, String) -> Unit,
    onImagePickRequested: ((String, String) -> Unit)?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(nodeTitle, style = MaterialTheme.typography.labelLarge)
            Column(
                modifier = Modifier.padding(top = Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                parameters.forEach { param ->
                    ParameterWidget(param, onParameterChanged, onImagePickRequested)
                }
            }
        }
    }
}

@Composable
private fun ParameterWidget(
    param: ExtractedParameter,
    onParameterChanged: (String, String, String) -> Unit,
    onImagePickRequested: ((String, String) -> Unit)?,
) {
    when (param.paramType) {
        ParameterType.TEXT -> TextParameterWidget(param, onParameterChanged)
        ParameterType.NUMBER -> NumberParameterWidget(param, onParameterChanged)
        ParameterType.SELECT -> SelectParameterWidget(param, onParameterChanged)
        ParameterType.SEED -> SeedParameterWidget(param, onParameterChanged)
        ParameterType.BOOLEAN -> BooleanParameterWidget(param, onParameterChanged)
        ParameterType.IMAGE -> ImageParameterWidget(param, onImagePickRequested)
    }
}

@Composable
private fun BooleanParameterWidget(
    param: ExtractedParameter,
    onParameterChanged: (String, String, String) -> Unit,
) {
    val isChecked = param.currentValue.toBooleanLenient()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(param.paramName, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = isChecked,
            onCheckedChange = { newValue ->
                onParameterChanged(param.nodeId, param.paramName, newValue.toString())
            },
        )
    }
}

@Composable
private fun ImageParameterWidget(
    param: ExtractedParameter,
    onImagePickRequested: ((String, String) -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(param.paramName, style = MaterialTheme.typography.bodySmall)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Image preview placeholder
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier
                    .size(IMAGE_PREVIEW_SIZE.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(Spacing.md),
                    )
                    .padding(Spacing.md),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = param.currentValue.ifBlank {
                        stringResource(R.string.label_no_image_selected)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
                OutlinedButton(
                    onClick = { onImagePickRequested?.invoke(param.nodeId, param.paramName) },
                ) {
                    Text(stringResource(R.string.label_select_from_gallery))
                }
            }
        }
    }
}

@Composable
private fun TextParameterWidget(
    param: ExtractedParameter,
    onParameterChanged: (String, String, String) -> Unit,
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
    onParameterChanged: (String, String, String) -> Unit,
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
    onParameterChanged: (String, String, String) -> Unit,
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
    onParameterChanged: (String, String, String) -> Unit,
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
    onParameterChanged: (String, String, String) -> Unit,
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
    onParameterChanged: (String, String, String) -> Unit,
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

/**
 * Parses a boolean value leniently from common ComfyUI representations.
 */
private fun String.toBooleanLenient(): Boolean {
    return equals("true", ignoreCase = true) || this == "1"
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
    return numSteps.coerceAtMost(MAX_SLIDER_STEPS) - 1
}
