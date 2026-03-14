package com.riox432.civitdeck.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.CircularProgressIndicator
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
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = OVERLAY_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            Text("No images available", color = MaterialTheme.colorScheme.onSurface)
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.lg),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
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
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Escape -> { onClose(); true }
                        Key.DirectionLeft -> {
                            if (currentIndex > 0) currentIndex--
                            true
                        }
                        Key.DirectionRight -> {
                            if (currentIndex < imageUrls.lastIndex) currentIndex++
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                val zoomFactor = if (scrollDelta < 0) ZOOM_IN_FACTOR else ZOOM_OUT_FACTOR
                scale = (scale * zoomFactor).coerceIn(MIN_SCALE, MAX_SCALE)
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = imageUrls[currentIndex],
            contentDescription = "Image ${currentIndex + 1} of ${imageUrls.size}",
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

        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.lg),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        Text(
            text = "${currentIndex + 1} / ${imageUrls.size}",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.BottomCenter).padding(Spacing.lg),
        )
    }
}

private const val OVERLAY_ALPHA = 0.92f
private const val MIN_SCALE = 0.5f
private const val MAX_SCALE = 5f
private const val ZOOM_IN_FACTOR = 1.1f
private const val ZOOM_OUT_FACTOR = 0.9f
