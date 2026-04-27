package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.riox432.civitdeck.feature.comfyui.domain.model.MaskEditorState
import com.riox432.civitdeck.feature.comfyui.domain.model.PathSegment

private const val MASK_ALPHA = 0.5f
private const val REFERENCE_CANVAS_SIZE = 400f
private val MASK_COLOR = Color.Red.copy(alpha = MASK_ALPHA)
private val ERASER_COLOR = Color.Transparent

/**
 * Touch-based mask painting canvas.
 * Renders existing path segments as a semi-transparent red overlay and
 * captures new strokes via drag gestures.
 */
@Composable
fun MaskPaintCanvas(
    state: MaskEditorState,
    onStrokeCompleted: (List<Pair<Float, Float>>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Canvas(
        modifier = modifier
            .pointerInput(state.isEraserMode, state.brushSize) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPoints = listOf(offset)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentPoints = currentPoints + change.position
                    },
                    onDragEnd = {
                        val canvasW = size.width.toFloat()
                        val canvasH = size.height.toFloat()
                        if (canvasW > 0 && canvasH > 0) {
                            val normalized = currentPoints.map { pt ->
                                (pt.x / canvasW) to (pt.y / canvasH)
                            }
                            onStrokeCompleted(normalized)
                        }
                        currentPoints = emptyList()
                    },
                    onDragCancel = {
                        currentPoints = emptyList()
                    },
                )
            },
    ) {
        val canvasW = size.width
        val canvasH = size.height

        // Draw committed segments
        for (segment in state.pathSegments) {
            drawSegment(segment, canvasW, canvasH)
        }

        // Draw current in-progress stroke
        if (currentPoints.isNotEmpty()) {
            val strokeW = state.brushSize * canvasW / REFERENCE_CANVAS_SIZE
            val color = if (state.isEraserMode) ERASER_COLOR else MASK_COLOR
            val blendMode = if (state.isEraserMode) BlendMode.Clear else BlendMode.SrcOver
            val path = buildSmoothPath(currentPoints)
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = strokeW,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
                blendMode = blendMode,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSegment(
    segment: PathSegment,
    canvasW: Float,
    canvasH: Float,
) {
    val points = segment.points.map { (x, y) ->
        Offset(x * canvasW, y * canvasH)
    }
    if (points.isEmpty()) return

    val strokeW = segment.brushSize * canvasW / REFERENCE_CANVAS_SIZE
    val color = if (segment.isEraser) ERASER_COLOR else MASK_COLOR
    val blendMode = if (segment.isEraser) BlendMode.Clear else BlendMode.SrcOver
    val path = buildSmoothPath(points)

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeW,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
        blendMode = blendMode,
    )
}

private fun buildSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    if (points.size == 1) {
        path.lineTo(points.first().x + 0.1f, points.first().y + 0.1f)
        return path
    }
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        val midX = (prev.x + curr.x) / 2f
        val midY = (prev.y + curr.y) / 2f
        path.quadraticBezierTo(prev.x, prev.y, midX, midY)
    }
    path.lineTo(points.last().x, points.last().y)
    return path
}
