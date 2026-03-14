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
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.feature.search.presentation.ModelSearchUiState
import com.riox432.civitdeck.ui.theme.Spacing

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
    return count
}

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterBottomSheet(
    uiState: ModelSearchUiState,
    onDismiss: () -> Unit,
    onShowSavedFilters: () -> Unit,
    onSaveFilter: () -> Unit,
    onResetFilters: () -> Unit,
    onTypeSelected: (ModelType?) -> Unit,
    onBaseModelToggled: (BaseModel) -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit,
    onFreshFindToggled: () -> Unit,
    onQualityFilterToggled: () -> Unit,
    onAddIncludedTag: (String) -> Unit,
    onRemoveIncludedTag: (String) -> Unit,
    onAddExcludedTag: (String) -> Unit,
    onRemoveExcludedTag: (String) -> Unit,
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
            onTypeSelected = onTypeSelected,
            onBaseModelToggled = onBaseModelToggled,
            onSortSelected = onSortSelected,
            onPeriodSelected = onPeriodSelected,
            onFreshFindToggled = onFreshFindToggled,
            onQualityFilterToggled = onQualityFilterToggled,
            onAddIncludedTag = onAddIncludedTag,
            onRemoveIncludedTag = onRemoveIncludedTag,
            onAddExcludedTag = onAddExcludedTag,
            onRemoveExcludedTag = onRemoveExcludedTag,
        )
    }
}

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun FilterSheetContent(
    uiState: ModelSearchUiState,
    onDismiss: () -> Unit,
    onShowSavedFilters: () -> Unit,
    onSaveFilter: () -> Unit,
    onResetFilters: () -> Unit,
    onTypeSelected: (ModelType?) -> Unit,
    onBaseModelToggled: (BaseModel) -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit,
    onFreshFindToggled: () -> Unit,
    onQualityFilterToggled: () -> Unit,
    onAddIncludedTag: (String) -> Unit,
    onRemoveIncludedTag: (String) -> Unit,
    onAddExcludedTag: (String) -> Unit,
    onRemoveExcludedTag: (String) -> Unit,
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
                Icon(Icons.Default.Bookmarks, contentDescription = "Saved filters")
            }
            IconButton(onClick = onSaveFilter) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = "Save current filter")
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
