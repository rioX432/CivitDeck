package com.riox432.civitdeck.ui.gallery

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.AspectRatioFilter
import com.riox432.civitdeck.domain.model.Image
import com.riox432.civitdeck.domain.model.MediaContentType
import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.model.ShareHashtag
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.ui.adaptive.adaptiveGridColumns
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.components.ErrorStateView
import com.riox432.civitdeck.ui.components.FilterChipRow
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.components.NsfwBlurOverlay
import com.riox432.civitdeck.ui.theme.CivitDeckColors
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Easing
import com.riox432.civitdeck.ui.theme.IconSize
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun ImageGalleryScreen(
    viewModel: ImageGalleryViewModel,
    onBack: () -> Unit,
    shareHashtags: List<ShareHashtag> = emptyList(),
    onToggleShareHashtag: (String, Boolean) -> Unit = { _, _ -> },
    onAddShareHashtag: (String) -> Unit = {},
    onRemoveShareHashtag: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ImageGalleryTopBar(onBack = onBack)
        },
    ) { padding ->
        ImageGalleryBody(
            uiState = uiState,
            onSortSelected = viewModel::onSortSelected,
            onPeriodSelected = viewModel::onPeriodSelected,
            onAspectRatioSelected = viewModel::onAspectRatioSelected,
            onImageClick = viewModel::onImageSelected,
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::retry,
            contentPadding = padding,
        )
    }

    val selectedIndex = uiState.selectedImageIndex
    if (selectedIndex != null) {
        ImageViewerOverlay(
            images = uiState.images.map {
                ViewerImage(url = it.url, meta = it.meta, contentType = it.contentType)
            },
            initialIndex = selectedIndex,
            onDismiss = viewModel::onDismissViewer,
            onSavePrompt = viewModel::savePrompt,
            shareHashtags = shareHashtags,
            onToggleShareHashtag = onToggleShareHashtag,
            onAddShareHashtag = onAddShareHashtag,
            onRemoveShareHashtag = onRemoveShareHashtag,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageGalleryTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text("Images") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_navigate_back)
                )
            }
        },
    )
}

@Composable
private fun ImageGalleryBody(
    uiState: ImageGalleryUiState,
    onSortSelected: (SortOrder) -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit,
    onAspectRatioSelected: (AspectRatioFilter?) -> Unit,
    onImageClick: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        FilterBar(
            uiState = uiState,
            onSortSelected = onSortSelected,
            onPeriodSelected = onPeriodSelected,
            onAspectRatioSelected = onAspectRatioSelected,
        )

        val stateKey = when {
            uiState.isLoading -> "loading"
            uiState.error != null && uiState.images.isEmpty() -> "error"
            else -> "content"
        }

        Crossfade(
            targetState = stateKey,
            animationSpec = tween(
                durationMillis = Duration.normal,
                easing = Easing.standard,
            ),
            label = "galleryContent",
            modifier = Modifier.weight(1f),
        ) { state ->
            when (state) {
                "loading" -> LoadingState()
                "error" -> ErrorState(error = uiState.error ?: "", onRetry = onRetry)
                else -> ImageGrid(
                    images = uiState.images,
                    blurSettings = uiState.nsfwBlurSettings,
                    isLoadingMore = uiState.isLoadingMore,
                    onImageClick = onImageClick,
                    onLoadMore = onLoadMore,
                )
            }
        }
    }
}

@Composable
private fun FilterBar(
    uiState: ImageGalleryUiState,
    onSortSelected: (SortOrder) -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit,
    onAspectRatioSelected: (AspectRatioFilter?) -> Unit,
) {
    val aspectRatioOptions = listOf(null) + AspectRatioFilter.entries
    Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
        FilterChipRow(
            options = SortOrder.entries,
            selected = uiState.selectedSort,
            onSelect = onSortSelected,
            label = { it.name },
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        FilterChipRow(
            options = aspectRatioOptions,
            selected = uiState.selectedAspectRatio,
            onSelect = onAspectRatioSelected,
            label = { it?.name ?: "All" },
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        FilterChipRow(
            options = TimePeriod.entries,
            selected = uiState.selectedPeriod,
            onSelect = onPeriodSelected,
            label = { it.name },
        )
    }
}

@Composable
private fun LoadingState() {
    LoadingStateOverlay()
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    ErrorStateView(message = error, onRetry = onRetry)
}

@Composable
private fun ImageGrid(
    images: List<Image>,
    blurSettings: NsfwBlurSettings,
    isLoadingMore: Boolean,
    onImageClick: (Int) -> Unit,
    onLoadMore: () -> Unit,
) {
    val gridState = rememberLazyStaggeredGridState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= images.size - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    val gridColumns = adaptiveGridColumns()
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(gridColumns),
        state = gridState,
        contentPadding = PaddingValues(Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalItemSpacing = Spacing.sm,
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(images, key = { _, image -> image.id }) { index, image ->
            ImageGridItem(
                image = image,
                blurSettings = blurSettings,
                onClick = { onImageClick(index) },
                contentDescription = stringResource(R.string.cd_image_number, index + 1),
                modifier = Modifier.animateItem(),
            )
        }
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ImageGridItem(
    image: Image,
    blurSettings: NsfwBlurSettings,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val aspectRatio = if (image.width > 0 && image.height > 0) {
        image.width.toFloat() / image.height.toFloat()
    } else {
        1f
    }

    NsfwBlurOverlay(
        nsfwLevel = image.nsfwLevel,
        blurSettings = blurSettings,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.image))
            .clickable(onClickLabel = stringResource(R.string.cd_open_image), onClick = onClick),
    ) {
        Box {
            CivitAsyncImage(
                imageUrl = image.url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio),
            )
            if (image.contentType == MediaContentType.VIDEO) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = stringResource(R.string.cd_video),
                    tint = CivitDeckColors.onScrim,
                    modifier = Modifier
                        .size(IconSize.large)
                        .align(Alignment.Center),
                )
            }
        }
    }
}

private const val LOAD_MORE_THRESHOLD = 6
