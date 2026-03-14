package com.riox432.civitdeck.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.Tween
import kotlin.math.roundToInt

private val ACTION_BUTTON_SIZE = 40.dp
private val ACTION_AREA_WIDTH = 64.dp
private const val DEFAULT_SWIPE_THRESHOLD = 0.3f

/**
 * Platform-agnostic swipeable card wrapper.
 *
 * Wraps any [content] composable with a horizontal swipe gesture
 * to reveal quick action buttons (favorite toggle, hide).
 * Pure Compose — no Android-specific APIs.
 */
@Composable
@Suppress("LongParameterList")
fun SwipeableCardLayout(
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
    swipeThreshold: Float = DEFAULT_SWIPE_THRESHOLD,
    onHapticFeedback: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val revealWidthPx = with(LocalDensity.current) {
        (ACTION_AREA_WIDTH * 2).toPx()
    }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    SwipeableCardLayoutInner(
        dragOffset = dragOffset,
        revealWidthPx = revealWidthPx,
        swipeThreshold = swipeThreshold,
        isFavorite = isFavorite,
        onFavoriteToggle = {
            onFavoriteToggle()
            dragOffset = 0f
        },
        onHide = {
            onHide()
            dragOffset = 0f
        },
        onDragDelta = { delta ->
            dragOffset = (dragOffset + delta).coerceIn(-revealWidthPx, 0f)
        },
        onDragStopped = {
            val thresholdPx = revealWidthPx * swipeThreshold
            dragOffset = if (-dragOffset >= thresholdPx) -revealWidthPx else 0f
        },
        onHapticFeedback = onHapticFeedback,
        modifier = modifier,
        content = content,
    )
}

@Suppress("LongParameterList")
@Composable
private fun SwipeableCardLayoutInner(
    dragOffset: Float,
    revealWidthPx: Float,
    swipeThreshold: Float,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onHide: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragStopped: () -> Unit,
    onHapticFeedback: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var hasTriggeredHaptic by remember { mutableFloatStateOf(0f) }

    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = Tween.fast(),
        label = "swipeOffset",
    )

    val draggableState = rememberDraggableState { delta ->
        onDragDelta(delta)
        val thresholdPx = revealWidthPx * swipeThreshold
        if (-dragOffset >= thresholdPx && hasTriggeredHaptic == 0f) {
            onHapticFeedback()
            hasTriggeredHaptic = 1f
        } else if (-dragOffset < thresholdPx) {
            hasTriggeredHaptic = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.card)),
    ) {
        ActionButtonsRow(
            isFavorite = isFavorite,
            onFavoriteToggle = onFavoriteToggle,
            onHide = onHide,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        onDragStopped()
                        hasTriggeredHaptic = 0f
                    },
                ),
        ) {
            content()
        }
    }
}

@Composable
private fun ActionButtonsRow(
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(
                Spacing.xs,
                Alignment.CenterVertically,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FavoriteActionButton(
                isFavorite = isFavorite,
                onClick = onFavoriteToggle,
            )
            HideActionButton(onClick = onHide)
        }
    }
}

@Composable
private fun FavoriteActionButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(ACTION_BUTTON_SIZE)) {
        Icon(
            imageVector = if (isFavorite) {
                Icons.Filled.Favorite
            } else {
                Icons.Filled.FavoriteBorder
            },
            contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
            tint = if (isFavorite) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
        )
    }
}

@Composable
private fun HideActionButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(ACTION_BUTTON_SIZE)) {
        Icon(
            imageVector = Icons.Filled.VisibilityOff,
            contentDescription = "Hide model",
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
