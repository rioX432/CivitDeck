package com.riox432.civitdeck.ui.gallery

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs

/**
 * A unified gesture detector for zoomable image views.
 *
 * Handles conflicts between single-tap, double-tap, vertical swipe (dismiss),
 * and multi-touch transformations (pan and zoom) in a single detector.
 *
 * Logic:
 * 1. Awaits the first finger down.
 * 2. If finger lifts without moving past slop, check for double-tap.
 * 3. If another finger goes down, start a transform (pinch-zoom) gesture.
 * 4. If finger moves past slop, start swipe/drag based on direction and canPan.
 * 5. Vertical swipe is only detected when the image is not panned (canPan is false).
 */
internal suspend fun PointerInputScope.detectZoomableGestures(
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
    onTransform: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onSwipeVertical: (dragAmount: Float) -> Unit,
    onSwipeEnd: (velocity: Float) -> Unit,
    canPan: () -> Boolean,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val tracker = GestureTracker(down.position, viewConfiguration.touchSlop)

        while (true) {
            val event = awaitPointerEvent()
            val anyPressed = event.changes.fastAny { it.pressed }

            if (!anyPressed) {
                handleAllFingersUp(event, tracker, onTap, onDoubleTap, onSwipeEnd)
                break
            }

            if (event.changes.size > 1) {
                handleMultiTouch(event, tracker, onTransform)
            } else if (!tracker.isTransforming) {
                handleSingleFinger(event, tracker, onTransform, onSwipeVertical, canPan)
            }
        }
    }
}

private class GestureTracker(
    val downPosition: Offset,
    val touchSlop: Float,
) {
    var isTransforming = false
    var isSwiping = false
    var isHorizontalDrag = false
    var hasMovedPastSlop = false
    var totalSwipe = 0f
}

private suspend fun AwaitPointerEventScope.handleAllFingersUp(
    event: PointerEvent,
    tracker: GestureTracker,
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
    onSwipeEnd: (Float) -> Unit,
) {
    if (tracker.isSwiping) {
        onSwipeEnd(tracker.totalSwipe)
    } else if (!tracker.isTransforming && !tracker.hasMovedPastSlop) {
        val upPosition = event.changes.first().position
        val up = waitForSecondTap(upPosition)
        if (up != null) {
            onDoubleTap(up.position)
        } else {
            onTap(upPosition)
        }
    }
}

private fun handleMultiTouch(
    event: PointerEvent,
    tracker: GestureTracker,
    onTransform: (Offset, Offset, Float) -> Unit,
) {
    tracker.isTransforming = true
    tracker.isSwiping = false

    val centroid = event.calculateCentroid(useCurrent = false)
    val panChange = event.calculatePan()
    val zoomChange = event.calculateZoom()

    if (zoomChange != 1f || panChange != Offset.Zero) {
        onTransform(centroid, panChange, zoomChange)
    }
    event.changes.fastForEach {
        if (it.positionChanged()) it.consume()
    }
}

private fun handleSingleFinger(
    event: PointerEvent,
    tracker: GestureTracker,
    onTransform: (Offset, Offset, Float) -> Unit,
    onSwipeVertical: (Float) -> Unit,
    canPan: () -> Boolean,
) {
    val change = event.changes.first()
    val dragTotal = change.position - tracker.downPosition
    val dragDelta = change.position - change.previousPosition

    if (tracker.isSwiping) {
        tracker.totalSwipe += dragDelta.y
        onSwipeVertical(dragDelta.y)
        change.consume()
        return
    }

    // Already committed to horizontal drag — let HorizontalPager handle
    if (tracker.isHorizontalDrag) return

    if (abs(dragTotal.x) <= tracker.touchSlop && abs(dragTotal.y) <= tracker.touchSlop) return
    tracker.hasMovedPastSlop = true

    if (canPan()) {
        tracker.isTransforming = true
        onTransform(change.position, dragDelta, 1f)
        change.consume()
    } else if (abs(dragTotal.y) > abs(dragTotal.x)) {
        tracker.isSwiping = true
        tracker.totalSwipe = dragTotal.y
        onSwipeVertical(dragTotal.y)
        change.consume()
    } else {
        // Direction locked to horizontal — stop checking for vertical swipe
        tracker.isHorizontalDrag = true
    }
}

/**
 * Wait for a second tap within double-tap timeout.
 * Returns the second tap's up event, or null if timeout or cancelled.
 */
private suspend fun AwaitPointerEventScope.waitForSecondTap(
    firstTapPosition: Offset,
): PointerInputChange? {
    return try {
        withTimeout(viewConfiguration.doubleTapTimeoutMillis) {
            val secondDown = awaitFirstDown(requireUnconsumed = false)

            val distance = (secondDown.position - firstTapPosition).getDistance()
            if (distance > viewConfiguration.touchSlop * DOUBLE_TAP_DISTANCE_FACTOR) {
                return@withTimeout null
            }

            waitForUpOrCancellation()
        }
    } catch (_: Exception) {
        null
    }
}

private const val DOUBLE_TAP_DISTANCE_FACTOR = 4
