package com.riox432.civitdeck.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.MediaContentType
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.ui.adaptive.adaptiveGridColumns
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.gallery.ImageViewerOverlay
import com.riox432.civitdeck.ui.gallery.ViewerImage
import com.riox432.civitdeck.ui.theme.CivitDeckColors
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun DetailOverlays(
    images: List<ModelImage>,
    selectedCarouselIndex: Int?,
    onDismissCarousel: () -> Unit,
    showImageGrid: Boolean,
    onDismissGrid: () -> Unit,
    onGridImageClick: (Int) -> Unit,
    gridSelectedIndex: Int?,
    onDismissGridViewer: () -> Unit,
) {
    if (selectedCarouselIndex != null && images.isNotEmpty()) {
        ImageViewerOverlay(
            images = images.map { ViewerImage(url = it.url, meta = it.meta, contentType = it.contentType) },
            initialIndex = selectedCarouselIndex,
            onDismiss = onDismissCarousel,
        )
    }

    if (showImageGrid && images.isNotEmpty()) {
        ImageGridBottomSheet(
            images = images,
            onDismiss = onDismissGrid,
            onImageClick = onGridImageClick,
        )
    }

    if (gridSelectedIndex != null && images.isNotEmpty()) {
        ImageViewerOverlay(
            images = images.map { ViewerImage(url = it.url, meta = it.meta, contentType = it.contentType) },
            initialIndex = gridSelectedIndex,
            onDismiss = onDismissGridViewer,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageGridBottomSheet(
    images: List<ModelImage>,
    onDismiss: () -> Unit,
    onImageClick: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Text(
            text = "Version Images (${images.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(adaptiveGridColumns()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalItemSpacing = Spacing.sm,
            contentPadding = PaddingValues(Spacing.sm),
        ) {
            itemsIndexed(images, key = { _, img -> img.url }) { index, image ->
                ImageGridItem(
                    image = image,
                    onClick = { onImageClick(index) },
                )
            }
        }
    }
}

@Composable
private fun ImageGridItem(
    image: ModelImage,
    onClick: () -> Unit,
) {
    val aspectRatio = if (image.width > 0 && image.height > 0) {
        image.width.toFloat() / image.height.toFloat()
    } else {
        1f
    }

    Box(contentAlignment = Alignment.Center) {
        CivitAsyncImage(
            imageUrl = image.url,
            contentDescription = stringResource(R.string.cd_version_image),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(CornerRadius.image))
                .clickable(onClick = onClick),
        )
        if (image.contentType == MediaContentType.VIDEO) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = stringResource(R.string.cd_video),
                tint = CivitDeckColors.onScrim,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}
