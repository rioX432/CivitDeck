package com.riox432.civitdeck.ui.compare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.riox432.civitdeck.ui.components.ImageErrorPlaceholder
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.filterByNsfwLevel
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailUiState
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.shimmer
import com.riox432.civitdeck.util.FormatUtils

@Composable
fun DesktopCompareScreen(
    leftViewModel: ModelDetailViewModel,
    rightViewModel: ModelDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val leftState by leftViewModel.uiState.collectAsState()
    val rightState by rightViewModel.uiState.collectAsState()

    Surface(modifier = modifier.fillMaxSize()) {
        Column {
            CompareToolbar(onBack = onBack)
            CompareBody(
                leftState = leftState,
                rightState = rightState,
                onLeftVersionSelected = leftViewModel::onVersionSelected,
                onRightVersionSelected = rightViewModel::onVersionSelected,
            )
        }
    }
}

@Composable
private fun CompareToolbar(onBack: () -> Unit) {
    Surface(tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Compare Models",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
    }
}

@Composable
private fun CompareBody(
    leftState: ModelDetailUiState,
    rightState: ModelDetailUiState,
    onLeftVersionSelected: (Int) -> Unit,
    onRightVersionSelected: (Int) -> Unit,
) {
    val leftModel = leftState.model
    val rightModel = rightState.model

    if (leftState.isLoading || rightState.isLoading) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("Loading...", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    if (leftModel == null || rightModel == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(
                leftState.error ?: rightState.error ?: "Failed to load models",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.lg),
    ) {
        item { ComparePanelsRow(leftState, rightState) }
        item {
            CompareVersionSelectors(
                leftModel = leftModel,
                rightModel = rightModel,
                leftSelectedIndex = leftState.selectedVersionIndex,
                rightSelectedIndex = rightState.selectedVersionIndex,
                onLeftVersionSelected = onLeftVersionSelected,
                onRightVersionSelected = onRightVersionSelected,
            )
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm)) }
        item {
            CompareSpecsTable(
                leftModel = leftModel,
                rightModel = rightModel,
                leftVersionIndex = leftState.selectedVersionIndex,
                rightVersionIndex = rightState.selectedVersionIndex,
            )
        }
    }
}

@Composable
private fun ComparePanelsRow(
    leftState: ModelDetailUiState,
    rightState: ModelDetailUiState,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        CompareModelPanel(state = leftState, modifier = Modifier.weight(1f))
        VerticalDivider()
        CompareModelPanel(state = rightState, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CompareModelPanel(
    state: ModelDetailUiState,
    modifier: Modifier = Modifier,
) {
    val model = state.model ?: return
    val version = model.modelVersions.getOrNull(state.selectedVersionIndex)
    val images = (version?.images ?: emptyList()).filterByNsfwLevel(state.nsfwFilterLevel)

    Column(modifier = modifier.padding(Spacing.xs)) {
        if (images.isNotEmpty()) {
            SubcomposeAsyncImage(
                model = images.first().url,
                contentDescription = "Model comparison image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                loading = { Box(Modifier.fillMaxSize().shimmer()) },
                error = { ImageErrorPlaceholder(modifier = Modifier.fillMaxSize()) },
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text("No image", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(
            text = model.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        )
        Text(
            text = model.type.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.xs),
        )
    }
}

@Composable
private fun CompareVersionSelectors(
    leftModel: Model,
    rightModel: Model,
    leftSelectedIndex: Int,
    rightSelectedIndex: Int,
    onLeftVersionSelected: (Int) -> Unit,
    onRightVersionSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
    ) {
        VersionChipRow(
            versions = leftModel.modelVersions,
            selectedIndex = leftSelectedIndex,
            onSelected = onLeftVersionSelected,
            modifier = Modifier.weight(1f),
        )
        VersionChipRow(
            versions = rightModel.modelVersions,
            selectedIndex = rightSelectedIndex,
            onSelected = onRightVersionSelected,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun VersionChipRow(
    versions: List<ModelVersion>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (versions.size <= 1) return
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        itemsIndexed(versions, key = { _, v -> v.id }) { index, version ->
            FilterChip(
                selected = index == selectedIndex,
                onClick = { onSelected(index) },
                label = { Text(version.name, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

@Composable
private fun CompareSpecsTable(
    leftModel: Model,
    rightModel: Model,
    leftVersionIndex: Int,
    rightVersionIndex: Int,
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.md)) {
        Text(
            text = "Comparison",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = Spacing.sm),
        )
        SpecRow("Type", leftModel.type.name, rightModel.type.name)
        SpecRow(
            "Base Model",
            leftModel.modelVersions.getOrNull(leftVersionIndex)?.baseModel ?: "-",
            rightModel.modelVersions.getOrNull(rightVersionIndex)?.baseModel ?: "-",
        )
        SpecRow(
            "Downloads",
            FormatUtils.formatCount(leftModel.stats.downloadCount),
            FormatUtils.formatCount(rightModel.stats.downloadCount),
        )
        SpecRow(
            "Favorites",
            FormatUtils.formatCount(leftModel.stats.favoriteCount),
            FormatUtils.formatCount(rightModel.stats.favoriteCount),
        )
        SpecRow(
            "Rating",
            FormatUtils.formatRating(leftModel.stats.rating),
            FormatUtils.formatRating(rightModel.stats.rating),
        )
        SpecRow(
            "File Size",
            primaryFileSize(leftModel, leftVersionIndex),
            primaryFileSize(rightModel, rightVersionIndex),
        )
    }
}

private fun primaryFileSize(model: Model, versionIndex: Int): String {
    val version = model.modelVersions.getOrNull(versionIndex) ?: return "-"
    val file = version.files.firstOrNull { it.primary } ?: version.files.firstOrNull()
    return if (file != null) FormatUtils.formatFileSize(file.sizeKB) else "-"
}

@Composable
private fun SpecRow(label: String, leftValue: String, rightValue: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = leftValue,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = rightValue,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
