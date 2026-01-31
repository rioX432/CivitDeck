package com.omooooori.civitdeck.ui.gallery

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.omooooori.civitdeck.domain.model.Image
import com.omooooori.civitdeck.domain.model.SortOrder
import com.omooooori.civitdeck.domain.model.TimePeriod

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
            images = uiState.images,
            initialIndex = uiState.selectedImageIndex!!,
            onDismiss = viewModel::onDismissViewer,
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

        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null && uiState.images.isEmpty() -> {
                ErrorState(error = uiState.error!!, onRetry = onRetry)
            }
            else -> {
                ImageGrid(
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
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SortFilterRow(
            selectedSort = uiState.selectedSort,
            onSortSelected = onSortSelected,
        )
        Spacer(modifier = Modifier.height(4.dp))
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
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
            Spacer(modifier = Modifier.height(16.dp))
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
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(images, key = { _, image -> image.id }) { index, image ->
            ImageGridItem(
                image = image,
                onClick = { onImageClick(index) },
            )
        }
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ImageGridItem(image: Image, onClick: () -> Unit) {
    val aspectRatio = if (image.width > 0 && image.height > 0) {
        image.width.toFloat() / image.height.toFloat()
    } else {
        1f
    }

    AsyncImage(
        model = image.url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    )
}

private const val LOAD_MORE_THRESHOLD = 6
