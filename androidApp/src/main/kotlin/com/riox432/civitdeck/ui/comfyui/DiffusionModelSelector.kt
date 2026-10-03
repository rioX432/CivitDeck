package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.DiffusionModelFamily
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.GenerationUiState
import com.riox432.civitdeck.ui.theme.Spacing

/** Family, text encoder and VAE pickers shown under the model picker for a diffusion model. */
@Composable
internal fun DiffusionModelSelector(state: GenerationUiState, viewModel: ComfyUIGenerationViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ModelFamilyDropdown(
            selected = state.selectedFamily,
            serverClipTypes = state.serverClipTypes,
            onSelected = viewModel::onModelFamilySelected,
        )
        ModelFileDropdown(
            label = stringResource(R.string.comfyui_text_encoder_label),
            placeholder = stringResource(R.string.comfyui_select_text_encoder),
            selected = state.selectedTextEncoder,
            options = state.textEncoders,
            onSelected = viewModel::onTextEncoderSelected,
        )
        ModelFileDropdown(
            label = stringResource(R.string.comfyui_vae_label),
            placeholder = stringResource(R.string.comfyui_select_vae),
            selected = state.selectedVae,
            options = state.vaes,
            onSelected = viewModel::onVaeSelected,
        )
    }
}

// The family cannot be guessed from a file name, so every family is listed; one whose CLIP type the
// server lacks stays visible but disabled, telling the user an update would unlock it.
@Composable
private fun ModelFamilyDropdown(
    selected: DiffusionModelFamily?,
    serverClipTypes: List<String>,
    onSelected: (DiffusionModelFamily) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val needsNewerServer = stringResource(R.string.comfyui_model_family_needs_newer_server)
    Column {
        Text(stringResource(R.string.comfyui_model_family_label), style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.baseModel ?: stringResource(R.string.comfyui_select_model_family), maxLines = 1)
        }
        // Prefill can select a family the server does not support; Generate then stays disabled, so say why.
        if (selected != null && !selected.isSupportedBy(serverClipTypes)) {
            Text(needsNewerServer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DiffusionModelFamily.entries.forEach { family ->
                val supported = family.isSupportedBy(serverClipTypes)
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(family.baseModel, maxLines = 1)
                            if (!supported) Text(needsNewerServer, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = {
                        onSelected(family)
                        expanded = false
                    },
                    enabled = supported,
                )
            }
        }
    }
}

@Composable
private fun ModelFileDropdown(
    label: String,
    placeholder: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        TextButton(
            onClick = { expanded = true },
            enabled = options.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(selected.ifBlank { placeholder }, maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, maxLines = 1) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
