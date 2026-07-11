package com.riox432.civitdeck.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.feature.search.presentation.ModelSearchUiState
import com.riox432.civitdeck.ui.theme.Spacing

// Compose UI: state/callback params are an intrinsic UI contract; a param object only hides them.
@Suppress("UnusedParameter", "LongParameterList")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopFilterBar(
    uiState: ModelSearchUiState,
    onTypeSelected: (ModelType?) -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit,
    onBaseModelToggled: (BaseModel) -> Unit,
    onQualityFilterToggled: () -> Unit,
    onSourceToggled: (ModelSource) -> Unit,
    onNsfwLevelSelected: (NsfwFilterLevel) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        // Source filter chips
        ModelSource.entries.forEach { source ->
            TypeChip(
                label = source.displayLabel(),
                selected = source in uiState.selectedSources,
            ) { onSourceToggled(source) }
        }

        // Type filter chips
        TypeChip(label = "All", selected = uiState.selectedType == null) {
            onTypeSelected(null)
        }
        FILTER_TYPES.forEach { type ->
            TypeChip(
                label = type.displayLabel(),
                selected = uiState.selectedType == type,
            ) { onTypeSelected(type) }
        }

        // Sort chips
        SortOrder.entries.forEach { sort ->
            TypeChip(
                label = sort.displayLabel(),
                selected = uiState.selectedSort == sort,
            ) { onSortSelected(sort) }
        }

        // Period chips
        TimePeriod.entries.forEach { period ->
            TypeChip(
                label = period.displayLabel(),
                selected = uiState.selectedPeriod == period,
            ) { onPeriodSelected(period) }
        }

        // Quality filter toggle
        QualityToggle(
            checked = uiState.isQualityFilterEnabled,
            onToggle = onQualityFilterToggled,
        )

        // NSFW browsing level (same 3 levels as Settings; writes the same
        // shared preference through the ViewModel)
        NsfwFilterLevel.entries.forEach { level ->
            TypeChip(
                label = level.displayLabel(),
                selected = uiState.nsfwFilterLevel == level,
            ) { onNsfwLevelSelected(level) }
        }

        TextButton(onClick = onResetFilters) {
            Text("Reset", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
    )
}

@Composable
private fun QualityToggle(
    checked: Boolean,
    onToggle: () -> Unit,
) {
    FilterChip(
        selected = checked,
        onClick = onToggle,
        label = { Text("Quality", style = MaterialTheme.typography.labelSmall) },
    )
}

private val FILTER_TYPES = listOf(
    ModelType.Checkpoint,
    ModelType.LORA,
    ModelType.LoCon,
    ModelType.TextualInversion,
    ModelType.Controlnet,
    ModelType.Upscaler,
    ModelType.VAE,
    ModelType.Poses,
    ModelType.Wildcards,
    ModelType.Workflows,
    ModelType.Other,
)

private fun ModelType.displayLabel(): String = when (this) {
    ModelType.TextualInversion -> "Textual Inv."
    ModelType.AestheticGradient -> "Aesthetic Grad."
    ModelType.MotionModule -> "Motion Module"
    else -> name
}

private fun SortOrder.displayLabel(): String = when (this) {
    SortOrder.HighestRated -> "Highest Rated"
    SortOrder.MostDownloaded -> "Most Downloaded"
    SortOrder.Quality -> "Quality Score"
    else -> name
}

private fun TimePeriod.displayLabel(): String = when (this) {
    TimePeriod.AllTime -> "All Time"
    else -> name
}

private fun ModelSource.displayLabel(): String = when (this) {
    ModelSource.CIVITAI -> "CivitAI"
    ModelSource.HUGGING_FACE -> "HuggingFace"
    ModelSource.TENSOR_ART -> "TensorArt"
}

/** Same user-facing labels as the Android/iOS 3-level NSFW control. */
internal fun NsfwFilterLevel.displayLabel(): String = when (this) {
    NsfwFilterLevel.Off -> "Safe"
    NsfwFilterLevel.Soft -> "Moderate"
    NsfwFilterLevel.All -> "Everything"
}
