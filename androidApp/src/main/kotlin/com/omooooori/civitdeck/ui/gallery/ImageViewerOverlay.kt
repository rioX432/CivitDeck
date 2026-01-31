package com.omooooori.civitdeck.ui.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.omooooori.civitdeck.domain.model.Image

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerOverlay(
    images: List<Image>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }
    var showMetadata by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        ImagePager(
            images = images,
            pagerState = pagerState,
        )

        ViewerControls(
            onDismiss = onDismiss,
            onInfoClick = { showMetadata = true },
            hasMetadata = images.getOrNull(pagerState.currentPage)?.meta != null,
        )
    }

    if (showMetadata) {
        val currentImage = images.getOrNull(pagerState.currentPage)
        currentImage?.meta?.let { meta ->
            MetadataBottomSheet(
                meta = meta,
                sheetState = sheetState,
                onDismiss = { showMetadata = false },
            )
        }
    }
}

@Composable
private fun ImagePager(
    images: List<Image>,
    pagerState: androidx.compose.foundation.pager.PagerState,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        ZoomableImage(imageUrl = images[page].url)
    }
}

@Composable
private fun ViewerControls(
    onDismiss: () -> Unit,
    onInfoClick: () -> Unit,
    hasMetadata: Boolean,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color.White,
            ),
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }

        if (hasMetadata) {
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Icon(Icons.Default.Info, contentDescription = "Metadata")
            }
        }
    }
}

@Composable
private fun ZoomableImage(imageUrl: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
        offset = if (scale > 1f) {
            offset + panChange
        } else {
            Offset.Zero
        }
    }

    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = DOUBLE_TAP_ZOOM
                        }
                    },
                )
            }
            .transformable(state = transformableState)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y,
            ),
    )
}

private const val MIN_ZOOM = 0.5f
private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f
