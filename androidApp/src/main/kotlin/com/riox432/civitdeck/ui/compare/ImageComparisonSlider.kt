package com.riox432.civitdeck.ui.compare

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.riox432.civitdeck.R
import com.riox432.civitdeck.ui.theme.Duration
import kotlin.math.roundToInt

/** Slider orientation for image comparison. */
enum class SliderOrientation { Horizontal, Vertical }

/**
 * A before/after image comparison slider with drag-to-reveal and pinch-to-zoom.
 *
 * The before image is clipped by the slider position, revealing the after
 * image underneath. Users can drag the divider to compare and pinch to zoom.
 */
@Composable
fun ImageComparisonSlider(
    beforeImageUrl: String,
    afterImageUrl: String,
    modifier: Modifier = Modifier,
    orientation: SliderOrientation = SliderOrientation.Horizontal,
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var sliderFraction by remember { mutableFloatStateOf(INITIAL_FRACTION) }
    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
        scale = newScale
        panOffset = if (newScale > MIN_SCALE) panOffset + panChange else Offset.Zero
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { containerSize = it }
            .transformable(state = transformState),
    ) {
        val zoomModifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = panOffset.x
            translationY = panOffset.y
        }

        // After image (bottom layer, fully visible)
        ComparisonImage(afterImageUrl, Modifier.fillMaxSize().then(zoomModifier))

        // Before image (top layer, clipped by slider)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    clip = true
                    shape = SliderClipShape(sliderFraction, orientation)
                },
        ) {
            ComparisonImage(beforeImageUrl, Modifier.fillMaxSize().then(zoomModifier))
        }

        // Divider line
        SliderDivider(sliderFraction, orientation, containerSize)

        // Drag handle
        SliderHandle(sliderFraction, orientation, containerSize) { delta ->
            val max = when (orientation) {
                SliderOrientation.Horizontal -> containerSize.width.toFloat()
                SliderOrientation.Vertical -> containerSize.height.toFloat()
            }
            if (max > 0f) {
                val primary = when (orientation) {
                    SliderOrientation.Horizontal -> delta.x
                    SliderOrientation.Vertical -> delta.y
                }
                sliderFraction = (sliderFraction + primary / max).coerceIn(0f, 1f)
            }
        }
    }
}

// MARK: - Internal composables

@Composable
private fun ComparisonImage(imageUrl: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl).crossfade(Duration.normal).build(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@Composable
private fun SliderDivider(
    fraction: Float,
    orientation: SliderOrientation,
    size: IntSize,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        when (orientation) {
            SliderOrientation.Horizontal -> {
                val x = size.width * fraction
                drawLine(Color.White, Offset(x, 0f), Offset(x, size.height.toFloat()), DIVIDER_PX)
            }
            SliderOrientation.Vertical -> {
                val y = size.height * fraction
                drawLine(Color.White, Offset(0f, y), Offset(size.width.toFloat(), y), DIVIDER_PX)
            }
        }
    }
}

@Composable
private fun SliderHandle(
    fraction: Float,
    orientation: SliderOrientation,
    containerSize: IntSize,
    onDrag: (Offset) -> Unit,
) {
    val halfHandle = HANDLE_DP / 2f
    val ox = when (orientation) {
        SliderOrientation.Horizontal -> containerSize.width * fraction - halfHandle
        SliderOrientation.Vertical -> containerSize.width / 2f - halfHandle
    }.roundToInt()
    val oy = when (orientation) {
        SliderOrientation.Horizontal -> containerSize.height / 2f - halfHandle
        SliderOrientation.Vertical -> containerSize.height * fraction - halfHandle
    }.roundToInt()

    Box(
        modifier = Modifier
            .offset { IntOffset(ox, oy) }
            .size(HANDLE_DP.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = HANDLE_ALPHA))
            .pointerInput(Unit) { detectDragGestures { _, d -> onDrag(d) } },
        contentAlignment = Alignment.Center,
    ) {
        val iconRes = when (orientation) {
            SliderOrientation.Horizontal -> R.drawable.ic_slider_horizontal
            SliderOrientation.Vertical -> R.drawable.ic_slider_vertical
        }
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = "Slider handle",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(ICON_DP.dp),
        )
    }
}

private const val INITIAL_FRACTION = 0.5f
private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DIVIDER_PX = 3f
private const val HANDLE_DP = 40f
private const val ICON_DP = 20f
private const val HANDLE_ALPHA = 0.85f
