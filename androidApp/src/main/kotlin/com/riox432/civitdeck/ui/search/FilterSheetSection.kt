package com.riox432.civitdeck.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.ui.theme.Spacing

/**
 * Groups callback parameters for filter sheet composables to reduce parameter count.
 */
data class FilterCallbacks(
    val onTypeSelected: (ModelType?) -> Unit,
    val onBaseModelToggled: (BaseModel) -> Unit,
    val onSortSelected: (SortOrder) -> Unit,
    val onPeriodSelected: (TimePeriod) -> Unit,
    val onFreshFindToggled: () -> Unit,
    val onQualityFilterToggled: () -> Unit,
    val onAddIncludedTag: (String) -> Unit,
    val onRemoveIncludedTag: (String) -> Unit,
    val onAddExcludedTag: (String) -> Unit,
    val onRemoveExcludedTag: (String) -> Unit,
    val onSourceToggled: (ModelSource) -> Unit,
)

internal fun countActiveFilters(uiState: ModelSearchUiState): Int {
    var count = 0
    if (uiState.selectedType != null) count++
    if (uiState.selectedBaseModels.isNotEmpty()) count++
    if (uiState.selectedSort != SortOrder.MostDownloaded) count++
    if (uiState.selectedPeriod != TimePeriod.AllTime) count++
    if (uiState.isFreshFindEnabled) count++
    if (uiState.isQualityFilterEnabled) count++
    if (uiState.includedTags.isNotEmpty()) count++
    if (uiState.excludedTags.isNotEmpty()) count++
    if (uiState.selectedSources != setOf(ModelSource.CIVITAI)) count++
    return count
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterBottomSheet(
    uiState: ModelSearchUiState,
    onDismiss: () -> Unit,
    onShowSavedFilters: () -> Unit,
    onSaveFilter: () -> Unit,
    onResetFilters: () -> Unit,
    filterCallbacks: FilterCallbacks,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        FilterSheetContent(
            uiState = uiState,
            onDismiss = onDismiss,
            onShowSavedFilters = onShowSavedFilters,
            onSaveFilter = onSaveFilter,
            onResetFilters = onResetFilters,
            filterCallbacks = filterCallbacks,
        )
    }
}

@Composable
private fun FilterSheetContent(
    uiState: ModelSearchUiState,
    onDismiss: () -> Unit,
    onShowSavedFilters: () -> Unit,
    onSaveFilter: () -> Unit,
    onResetFilters: () -> Unit,
    filterCallbacks: FilterCallbacks,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(bottom = Spacing.lg),
    ) {
        item {
            FilterSheetHeader(
                onShowSavedFilters = onShowSavedFilters,
                onSaveFilter = onSaveFilter,
                onReset = {
                    onResetFilters()
                    onDismiss()
                },
            )
        }
        filterSourceSection(uiState, filterCallbacks.onSourceToggled)
        filterModelSections(uiState, filterCallbacks.onTypeSelected, filterCallbacks.onBaseModelToggled)
        filterSortSections(uiState, filterCallbacks.onSortSelected, filterCallbacks.onPeriodSelected)
        filterToggleSections(uiState, filterCallbacks.onFreshFindToggled, filterCallbacks.onQualityFilterToggled)
        filterTagSections(
            uiState,
            filterCallbacks.onAddIncludedTag,
            filterCallbacks.onRemoveIncludedTag,
            filterCallbacks.onAddExcludedTag,
            filterCallbacks.onRemoveExcludedTag,
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.filterSourceSection(
    uiState: ModelSearchUiState,
    onSourceToggled: (ModelSource) -> Unit,
) {
    item {
        SourceFilterSection(
            selectedSources = uiState.selectedSources,
            onSourceToggled = onSourceToggled,
        )
    }
    item { HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SourceFilterSection(
    selectedSources: Set<ModelSource>,
    onSourceToggled: (ModelSource) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        FilterSectionHeader("Source", "(multiple)")
        FlowRow(
            modifier = Modifier.padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            ModelSource.entries.forEach { source ->
                FilterChipItem(
                    label = source.displayLabel(),
                    isSelected = source in selectedSources,
                    onClick = { onSourceToggled(source) },
                    showCheckmark = true,
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.filterModelSections(
    uiState: ModelSearchUiState,
    onTypeSelected: (ModelType?) -> Unit,
    onBaseModelToggled: (BaseModel) -> Unit,
) {
    item {
        TypeFilterSection(
            selectedType = uiState.selectedType,
            onTypeSelected = onTypeSelected,
        )
    }
    item {
        BaseModelFilterSection(
            selectedBaseModels = uiState.selectedBaseModels,
            onBaseModelToggled = onBaseModelToggled,
        )
    }
    item { HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg)) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.filterSortSections(
    uiState: ModelSearchUiState,
    onSortSelected: (SortOrder) -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit,
) {
    item {
        SortFilterSection(
            selectedSort = uiState.selectedSort,
            onSortSelected = onSortSelected,
        )
    }
    item {
        PeriodFilterSection(
            selectedPeriod = uiState.selectedPeriod,
            onPeriodSelected = onPeriodSelected,
        )
    }
    item { HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg)) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.filterToggleSections(
    uiState: ModelSearchUiState,
    onFreshFindToggled: () -> Unit,
    onQualityFilterToggled: () -> Unit,
) {
    item {
        FreshOnlyToggleRow(
            isFreshFindEnabled = uiState.isFreshFindEnabled,
            onFreshFindToggled = onFreshFindToggled,
        )
    }
    item {
        QualityFilterToggleRow(
            isQualityFilterEnabled = uiState.isQualityFilterEnabled,
            onQualityFilterToggled = onQualityFilterToggled,
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.filterTagSections(
    uiState: ModelSearchUiState,
    onAddIncludedTag: (String) -> Unit,
    onRemoveIncludedTag: (String) -> Unit,
    onAddExcludedTag: (String) -> Unit,
    onRemoveExcludedTag: (String) -> Unit,
) {
    item {
        TagFilterSection(
            tags = uiState.includedTags,
            onAddTag = onAddIncludedTag,
            onRemoveTag = onRemoveIncludedTag,
            placeholder = "Include tag...",
            header = "Tags",
            headerSubtitle = "(include)",
            chipBackground = { MaterialTheme.colorScheme.primaryContainer },
            chipForeground = { MaterialTheme.colorScheme.onPrimaryContainer },
        )
    }
    item {
        TagFilterSection(
            tags = uiState.excludedTags,
            onAddTag = onAddExcludedTag,
            onRemoveTag = onRemoveExcludedTag,
            placeholder = "Exclude tag...",
            chipBackground = { MaterialTheme.colorScheme.errorContainer },
            chipForeground = { MaterialTheme.colorScheme.onErrorContainer },
        )
    }
}

@Composable
private fun FilterSheetHeader(
    onShowSavedFilters: () -> Unit,
    onSaveFilter: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Filters", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onShowSavedFilters) {
                Icon(Icons.Default.Bookmarks, contentDescription = stringResource(R.string.cd_saved_filters))
            }
            IconButton(onClick = onSaveFilter) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = stringResource(R.string.cd_save_current_filter))
            }
            TextButton(onClick = onReset) {
                Text("Reset")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeFilterSection(
    selectedType: ModelType?,
    onTypeSelected: (ModelType?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        FilterSectionHeader("Type")
        FlowRow(
            modifier = Modifier.padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            filterTypes.forEach { type ->
                FilterChipItem(
                    label = type?.displayLabel() ?: "All",
                    isSelected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BaseModelFilterSection(
    selectedBaseModels: Set<BaseModel>,
    onBaseModelToggled: (BaseModel) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        FilterSectionHeader("Base Model", "(multiple)")
        FlowRow(
            modifier = Modifier.padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            BaseModel.entries.forEach { baseModel ->
                FilterChipItem(
                    label = baseModel.displayName,
                    isSelected = baseModel in selectedBaseModels,
                    onClick = { onBaseModelToggled(baseModel) },
                    showCheckmark = true,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SortFilterSection(
    selectedSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        FilterSectionHeader("Sort")
        FlowRow(
            modifier = Modifier.padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SortOrder.entries.forEach { sort ->
                FilterChipItem(
                    label = sort.displayLabel(),
                    isSelected = selectedSort == sort,
                    onClick = { onSortSelected(sort) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodFilterSection(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        FilterSectionHeader("Period")
        FlowRow(
            modifier = Modifier.padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            TimePeriod.entries.forEach { period ->
                FilterChipItem(
                    label = period.displayLabel(),
                    isSelected = selectedPeriod == period,
                    onClick = { onPeriodSelected(period) },
                )
            }
        }
    }
}

@Composable
private fun FreshOnlyToggleRow(
    isFreshFindEnabled: Boolean,
    onFreshFindToggled: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Fresh Only", style = MaterialTheme.typography.titleSmall)
        Switch(checked = isFreshFindEnabled, onCheckedChange = { onFreshFindToggled() })
    }
}

@Composable
private fun QualityFilterToggleRow(
    isQualityFilterEnabled: Boolean,
    onQualityFilterToggled: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = "Quality Filter", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Hide low-quality results",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = isQualityFilterEnabled, onCheckedChange = { onQualityFilterToggled() })
    }
}
