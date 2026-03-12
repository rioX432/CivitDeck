package com.riox432.civitdeck.ui.externalserver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationChoice
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationJob
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationJobStatus
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationOption
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationOptionType
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerGalleryUiState
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalServerGenerationSheet(
    state: ExternalServerGalleryUiState,
    onParamChanged: (String, String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("Generate", style = MaterialTheme.typography.titleMedium)

            if (state.isLoadingOptions) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                state.generationOptions.forEach { option ->
                    DynamicFormField(
                        option = option,
                        currentValue = state.generationParams[option.key] ?: "",
                        dependentChoices = state.dependentChoices[option.key],
                        onValueChanged = { onParamChanged(option.key, it) },
                    )
                }

                state.generationError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Button(
                    onClick = onSubmit,
                    enabled = !state.isSubmittingGeneration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.lg),
                ) {
                    if (state.isSubmittingGeneration) {
                        CircularProgressIndicator()
                    } else {
                        Text("Start Generation")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DynamicFormField(
    option: GenerationOption,
    currentValue: String,
    dependentChoices: List<GenerationChoice>?,
    onValueChanged: (String) -> Unit,
) {
    when (option.type) {
        GenerationOptionType.SELECT -> {
            SelectField(
                label = option.label,
                choices = dependentChoices ?: option.choices,
                selectedValue = currentValue,
                onValueChanged = onValueChanged,
            )
        }
        GenerationOptionType.TEXT -> {
            OutlinedTextField(
                value = currentValue,
                onValueChange = onValueChanged,
                label = { Text(option.label) },
                placeholder = option.placeholder?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        GenerationOptionType.NUMBER -> {
            NumberField(
                label = option.label,
                value = currentValue,
                min = option.min ?: 1,
                max = option.max ?: 100,
                onValueChanged = onValueChanged,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectField(
    label: String,
    choices: List<GenerationChoice>,
    selectedValue: String,
    onValueChanged: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = choices.find { it.value == selectedValue }?.label ?: selectedValue

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(choice.label)
                            choice.description?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    onClick = {
                        onValueChanged(choice.value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    min: Int,
    max: Int,
    onValueChanged: (String) -> Unit,
) {
    val numValue = value.toFloatOrNull() ?: min.toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(numValue.toInt().toString(), style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = numValue,
            onValueChange = { onValueChanged(it.toInt().toString()) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = (max - min - 1).coerceAtLeast(0),
        )
    }
}

@Composable
fun GenerationJobDialog(
    job: GenerationJob,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (job.status == GenerationJobStatus.COMPLETED ||
                job.status == GenerationJobStatus.ERROR
            ) {
                onDismiss()
            }
        },
        title = {
            Text(
                when (job.status) {
                    GenerationJobStatus.QUEUED -> "Queued"
                    GenerationJobStatus.RUNNING -> "Generating..."
                    GenerationJobStatus.COMPLETED -> "Complete"
                    GenerationJobStatus.ERROR -> "Error"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                if (job.status == GenerationJobStatus.RUNNING) {
                    LinearProgressIndicator(
                        progress = { job.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (job.message.isNotBlank()) {
                    Text(job.message)
                }
                if (job.total > 0) {
                    Text("${job.completed}/${job.total} images")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                val isFinished = job.status == GenerationJobStatus.COMPLETED ||
                    job.status == GenerationJobStatus.ERROR
                Text(if (isFinished) "Close" else "Dismiss")
            }
        },
    )
}
