package com.riox432.civitdeck.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.PlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.thumbnailUrl
import com.riox432.civitdeck.ui.components.ImageErrorPlaceholder
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.shimmer

@Composable
internal fun ImageGridPanel(
    images: List<ModelImage>,
    selectedIndex: Int?,
    onImageSelect: (Int) -> Unit,
    onImageFullscreen: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val urls = images.map { it.url }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(IMAGE_GRID_MIN_SIZE),
        modifier = modifier.padding(Spacing.md),
        contentPadding = PaddingValues(Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        itemsIndexed(images, key = { _, img -> img.url }) { index, image ->
            val isSelected = selectedIndex == index
            Box {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(PlatformContext.INSTANCE)
                        .data(image.thumbnailUrl())
                        .size(Size(DETAIL_GRID_IMAGE_SIZE, DETAIL_GRID_IMAGE_SIZE))
                        .build(),
                    contentDescription = "Image ${index + 1}",
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(CornerRadius.card))
                        .then(
                            if (isSelected) {
                                Modifier.background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onImageSelect(index) },
                    contentScale = ContentScale.Crop,
                    loading = { Box(Modifier.fillMaxSize().shimmer()) },
                    error = { ImageErrorPlaceholder(modifier = Modifier.fillMaxSize()) },
                )
            }
        }
    }
}
