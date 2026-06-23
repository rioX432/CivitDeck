package com.riox432.civitdeck.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import com.riox432.civitdeck.ui.components.ImageErrorPlaceholder
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DesktopImageViewer(
    imageUrls: List<String>,
    initialIndex: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (imageUrls.isEmpty()) {
        EmptyImageViewer(onClose = onClose, modifier = modifier)
        return
    }

    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, imageUrls.lastIndex)) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val focusRequester = remember { FocusRequester() }

    // Reset transform when index changes
    LaunchedEffect(currentIndex) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    // Auto-focus for keyboard input
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = OVERLAY_ALPHA))
            .focusRequester(focusRequester)
            .focusable()
            .imageViewerGestures(
                onKey = { keyEvent ->
                    handleViewerKeyEvent(
                        keyEvent = keyEvent,
                        onClose = onClose,
                        canGoPrevious = currentIndex > 0,
                        canGoNext = currentIndex < imageUrls.lastIndex,
                        onPrevious = { currentIndex-- },
                        onNext = { currentIndex++ },
                    )
                },
                onZoom = { zoomFactor -> scale = (scale * zoomFactor).coerceIn(MIN_SCALE, MAX_SCALE) },
                onDrag = { dx, dy ->
                    offsetX += dx
                    offsetY += dy
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        ImageViewerBody(
            imageUrls = imageUrls,
            currentIndex = currentIndex,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            onClose = onClose,
        )
    }
}

@Composable
private fun BoxScope.ImageViewerBody(
    imageUrls: List<String>,
    currentIndex: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onClose: () -> Unit,
) {
    ZoomableViewerImage(
        url = imageUrls[currentIndex],
        contentDescription = "Image ${currentIndex + 1} of ${imageUrls.size}",
        scale = scale,
        offsetX = offsetX,
        offsetY = offsetY,
    )
    ImageViewerControls(
        indexLabel = "${currentIndex + 1} / ${imageUrls.size}",
        onClose = onClose,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.imageViewerGestures(
    onKey: (androidx.compose.ui.input.key.KeyEvent) -> Boolean,
    onZoom: (Float) -> Unit,
    onDrag: (Float, Float) -> Unit,
): Modifier = this
    .onKeyEvent(onKey)
    .onPointerEvent(PointerEventType.Scroll) { event ->
        val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
        val zoomFactor = if (scrollDelta < 0) ZOOM_IN_FACTOR else ZOOM_OUT_FACTOR
        onZoom(zoomFactor)
    }
    .pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consume()
            onDrag(dragAmount.x, dragAmount.y)
        }
    }

@Composable
private fun BoxScope.ImageViewerControls(
    indexLabel: String,
    onClose: () -> Unit,
) {
    ImageViewerCloseButton(
        onClose = onClose,
        modifier = Modifier.align(Alignment.TopEnd),
    )

    Text(
        text = indexLabel,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.align(Alignment.BottomCenter).padding(Spacing.lg),
    )
}

@Composable
private fun ZoomableViewerImage(
    url: String,
    contentDescription: String,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            },
        contentScale = ContentScale.Fit,
        loading = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
            }
        },
        error = { ImageErrorPlaceholder(modifier = Modifier.fillMaxSize()) },
    )
}

private fun handleViewerKeyEvent(
    keyEvent: androidx.compose.ui.input.key.KeyEvent,
    onClose: () -> Unit,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
): Boolean {
    if (keyEvent.type != KeyEventType.KeyDown) return false
    return when (keyEvent.key) {
        Key.Escape -> {
            onClose()
            true
        }
        Key.DirectionLeft -> {
            if (canGoPrevious) onPrevious()
            true
        }
        Key.DirectionRight -> {
            if (canGoNext) onNext()
            true
        }
        else -> false
    }
}

@Composable
private fun EmptyImageViewer(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = OVERLAY_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Text("No images available", color = MaterialTheme.colorScheme.onSurface)
        ImageViewerCloseButton(
            onClose = onClose,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun ImageViewerCloseButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClose,
        modifier = modifier.padding(Spacing.lg),
    ) {
        Icon(
            Icons.Default.Close,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private const val OVERLAY_ALPHA = 0.92f
private const val MIN_SCALE = 0.5f
private const val MAX_SCALE = 5f
private const val ZOOM_IN_FACTOR = 1.1f
private const val ZOOM_OUT_FACTOR = 0.9f
