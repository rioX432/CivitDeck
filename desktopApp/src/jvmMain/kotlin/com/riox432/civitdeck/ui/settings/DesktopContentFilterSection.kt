package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.feature.settings.presentation.ContentFilterSettingsViewModel
import com.riox432.civitdeck.ui.search.displayLabel
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun ContentFilterSection(viewModel: ContentFilterSettingsViewModel) {
    val state by viewModel.uiState.collectAsState()

    SettingsCard(title = "Content Filter") {
        SettingsDropdown(
            label = "NSFW Browsing Level",
            selected = state.nsfwFilterLevel.displayLabel(),
            options = NsfwFilterLevel.entries.map { it.displayLabel() },
            onSelected = { label ->
                NsfwFilterLevel.entries.firstOrNull { it.displayLabel() == label }
                    ?.let(viewModel::onNsfwFilterChanged)
            },
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        BlurIntensitySliders(
            softIntensity = state.nsfwBlurSettings.softIntensity,
            matureIntensity = state.nsfwBlurSettings.matureIntensity,
            explicitIntensity = state.nsfwBlurSettings.explicitIntensity,
            onSoftChanged = { intensity ->
                viewModel.onNsfwBlurSettingsChanged(
                    state.nsfwBlurSettings.copy(softIntensity = intensity),
                )
            },
            onMatureChanged = { intensity ->
                viewModel.onNsfwBlurSettingsChanged(
                    state.nsfwBlurSettings.copy(matureIntensity = intensity),
                )
            },
            onExplicitChanged = { intensity ->
                viewModel.onNsfwBlurSettingsChanged(
                    state.nsfwBlurSettings.copy(explicitIntensity = intensity),
                )
            },
        )
        ExcludedTagsList(
            tags = state.excludedTags,
            onRemoveTag = viewModel::onRemoveExcludedTag,
        )
    }
}

@Composable
private fun BlurIntensitySliders(
    softIntensity: Int,
    matureIntensity: Int,
    explicitIntensity: Int,
    onSoftChanged: (Int) -> Unit,
    onMatureChanged: (Int) -> Unit,
    onExplicitChanged: (Int) -> Unit,
) {
    SliderSetting(
        label = "Soft Blur Intensity",
        value = softIntensity.toFloat(),
        valueRange = 0f..100f,
        steps = 9,
        valueLabel = "$softIntensity%",
        onValueChange = { onSoftChanged(it.toInt()) },
    )
    Spacer(modifier = Modifier.height(Spacing.xs))
    SliderSetting(
        label = "Mature Blur Intensity",
        value = matureIntensity.toFloat(),
        valueRange = 0f..100f,
        steps = 9,
        valueLabel = "$matureIntensity%",
        onValueChange = { onMatureChanged(it.toInt()) },
    )
    Spacer(modifier = Modifier.height(Spacing.xs))
    SliderSetting(
        label = "Explicit Blur Intensity",
        value = explicitIntensity.toFloat(),
        valueRange = 0f..100f,
        steps = 9,
        valueLabel = "$explicitIntensity%",
        onValueChange = { onExplicitChanged(it.toInt()) },
    )
}

@Composable
private fun ExcludedTagsList(
    tags: List<String>,
    onRemoveTag: (String) -> Unit,
) {
    if (tags.isNotEmpty()) {
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text("Excluded Tags:", style = MaterialTheme.typography.labelMedium)
        tags.forEach { tag ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onRemoveTag(tag) }) {
                    Text("Remove")
                }
            }
        }
    }
}
