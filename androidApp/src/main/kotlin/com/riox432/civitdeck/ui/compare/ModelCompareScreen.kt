package com.riox432.civitdeck.ui.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.filterByNsfwLevel
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailUiState
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.ui.components.ErrorStateView
import com.riox432.civitdeck.ui.components.ImageErrorPlaceholder
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.shimmer
import com.riox432.civitdeck.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelCompareScreen(
    leftViewModel: ModelDetailViewModel,
    rightViewModel: ModelDetailViewModel,
    onBack: () -> Unit,
) {
    val leftState by leftViewModel.uiState.collectAsStateWithLifecycle()
    val rightState by rightViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Compare Models") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        CompareBody(
            leftState = leftState,
            rightState = rightState,
            onLeftVersionSelected = leftViewModel::onVersionSelected,
            onRightVersionSelected = rightViewModel::onVersionSelected,
            contentPadding = padding,
        )
    }
}

@Composable
private fun CompareBody(
    leftState: ModelDetailUiState,
    rightState: ModelDetailUiState,
    onLeftVersionSelected: (Int) -> Unit,
    onRightVersionSelected: (Int) -> Unit,
    contentPadding: PaddingValues,
) {
    val leftModel = leftState.model
    val rightModel = rightState.model

    val bothLoading = leftState.isLoading || rightState.isLoading
    val bothMissing = leftModel == null || rightModel == null
    if (bothLoading && bothMissing) {
        LoadingStateOverlay(modifier = Modifier.padding(contentPadding))
        return
    }
    if (leftModel == null || rightModel == null) {
        ErrorStateView(
            message = leftState.error ?: rightState.error ?: "Failed to load models",
            modifier = Modifier.padding(contentPadding),
        )
        return
    }

    CompareLoadedBody(
        leftState,
        rightState,
        leftModel,
        rightModel,
        onLeftVersionSelected,
        onRightVersionSelected,
        contentPadding
    )
}

@Suppress("LongParameterList")
@Composable
private fun CompareLoadedBody(
    leftState: ModelDetailUiState,
    rightState: ModelDetailUiState,
    leftModel: Model,
    rightModel: Model,
    onLeftVersionSelected: (Int) -> Unit,
    onRightVersionSelected: (Int) -> Unit,
    contentPadding: PaddingValues,
) {
    var showImageComparison by remember { mutableStateOf(false) }
    val leftImages = leftModel.selectedImages(leftState)
    val rightImages = rightModel.selectedImages(rightState)
    val canCompareImages = leftImages.isNotEmpty() && rightImages.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + Spacing.lg,
        ),
    ) {
        item { ComparePanelsRow(leftState, rightState) }
        if (canCompareImages) {
            item { CompareImagesButton(onClick = { showImageComparison = true }) }
        }
        item {
            CompareVersionSelectors(
                leftModel,
                rightModel,
                leftState.selectedVersionIndex,
                rightState.selectedVersionIndex,
                onLeftVersionSelected,
                onRightVersionSelected
            )
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm)) }
        item {
            CompareSpecsTable(
                leftModel,
                rightModel,
                leftState.selectedVersionIndex,
                rightState.selectedVersionIndex
            )
        }
    }

    if (showImageComparison && canCompareImages) {
        ImageComparisonOverlay(leftImages.first().url, rightImages.first().url, leftModel.name, rightModel.name) {
            showImageComparison = false
        }
    }
}

private fun Model.selectedImages(state: ModelDetailUiState): List<ModelImage> {
    val version = modelVersions.getOrNull(state.selectedVersionIndex) ?: return emptyList()
    return version.images.filterByNsfwLevel(state.nsfwFilterLevel)
}

@Composable
private fun CompareImagesButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Button(onClick = onClick) {
            Text("Compare Images")
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
        CompareModelPanel(
            state = leftState,
            modifier = Modifier.weight(1f),
        )
        VerticalDivider()
        CompareModelPanel(
            state = rightState,
            modifier = Modifier.weight(1f),
        )
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
        CompareImagePager(images = images)
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
private fun CompareImagePager(images: List<ModelImage>) {
    if (images.isEmpty()) {
        ImageErrorPlaceholder(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        return
    }

    val pagerState = rememberPagerState { images.size }
    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            CompareCarouselPage(image = images[page])
        }
        if (images.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1} / ${images.size}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = Spacing.xs),
            )
        }
    }
}

@Composable
private fun CompareCarouselPage(image: ModelImage) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(image.url)
            .crossfade(Duration.normal)
            .build(),
        contentDescription = "Model comparison image",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shimmer(),
            )
        },
        error = {
            ImageErrorPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
        },
    )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
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
        itemsIndexed(versions, key = { _, version -> version.id }) { index, version ->
            FilterChip(
                selected = index == selectedIndex,
                onClick = { onSelected(index) },
                label = {
                    Text(version.name, style = MaterialTheme.typography.labelSmall)
                },
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
            baseModelForVersion(leftModel, leftVersionIndex),
            baseModelForVersion(rightModel, rightVersionIndex),
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

private fun baseModelForVersion(model: Model, versionIndex: Int): String =
    model.modelVersions.getOrNull(versionIndex)?.baseModel ?: "-"

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
