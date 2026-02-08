package com.riox432.civitdeck.ui.gallery

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.riox432.civitdeck.domain.model.Image
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.ui.components.ImageErrorPlaceholder
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Easing
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.shimmer

@Composable
fun ImageGalleryScreen(
    viewModel: ImageGalleryViewModel,
    onBack: () -> Unit,
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
            onNsfwToggle = viewModel::onNsfwToggle,
            onImageClick = viewModel::onImageSelected,
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::retry,
            contentPadding = padding,
        )
    }

    if (uiState.selectedImageIndex != null) {
        ImageViewerOverlay(
            images = uiState.images.map { ViewerImage(url = it.url, meta = it.meta) },
            initialIndex = uiState.selectedImageIndex!!,
            onDismiss = viewModel::onDismissViewer,
            onSavePrompt = viewModel::savePrompt,
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
    )
}

@Composable
private fun ImageGalleryBody(
    uiState: ImageGalleryUiState,
    onSortSelected: (SortOrder) -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit,
    onNsfwToggle: () -> Unit,
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
            onNsfwToggle = onNsfwToggle,
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
    onNsfwToggle: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
        SortFilterRow(
            selectedSort = uiState.selectedSort,
            onSortSelected = onSortSelected,
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        PeriodAndNsfwRow(
            selectedPeriod = uiState.selectedPeriod,
            showNsfw = uiState.showNsfw,
            onPeriodSelected = onPeriodSelected,
            onNsfwToggle = onNsfwToggle,
        )
    }
}

@Composable
private fun SortFilterRow(
    selectedSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SortOrder.entries.forEach { sort ->
            FilterChip(
                selected = sort == selectedSort,
                onClick = { onSortSelected(sort) },
                label = { Text(sort.name, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

@Composable
private fun PeriodAndNsfwRow(
    selectedPeriod: TimePeriod,
    showNsfw: Boolean,
    onPeriodSelected: (TimePeriod) -> Unit,
    onNsfwToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            TimePeriod.entries.forEach { period ->
                FilterChip(
                    selected = period == selectedPeriod,
                    onClick = { onPeriodSelected(period) },
                    label = {
                        Text(period.name, style = MaterialTheme.typography.labelSmall)
                    },
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text("NSFW", style = MaterialTheme.typography.labelSmall)
            Switch(checked = showNsfw, onCheckedChange = { onNsfwToggle() })
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = error, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(Spacing.lg))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun ImageGrid(
    images: List<Image>,
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

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = gridState,
        contentPadding = PaddingValues(Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalItemSpacing = Spacing.sm,
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(images, key = { _, image -> image.id }) { index, image ->
            ImageGridItem(
                image = image,
                onClick = { onImageClick(index) },
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val aspectRatio = if (image.width > 0 && image.height > 0) {
        image.width.toFloat() / image.height.toFloat()
    } else {
        1f
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(image.url)
            .crossfade(Duration.normal)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(CornerRadius.image))
            .clickable(onClick = onClick),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .shimmer(),
            )
        },
        error = {
            ImageErrorPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio),
            )
        },
    )
}

private const val LOAD_MORE_THRESHOLD = 6
