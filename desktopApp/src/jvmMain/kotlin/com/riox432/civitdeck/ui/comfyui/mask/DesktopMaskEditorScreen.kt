@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.comfyui.mask

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import com.riox432.civitdeck.feature.comfyui.data.encoder.MaskPngEncoder
import com.riox432.civitdeck.feature.comfyui.domain.model.MaskEditorState
import com.riox432.civitdeck.feature.comfyui.domain.model.PathSegment
import com.riox432.civitdeck.feature.comfyui.presentation.MaskEditorViewModel
import com.riox432.civitdeck.ui.theme.Spacing

private const val MASK_ALPHA = 0.5f
private const val REFERENCE_CANVAS_SIZE = 400f
private val ERASER_ICON_COLOR = Color(0xFFFF9800)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopMaskEditorScreen(
    viewModel: MaskEditorViewModel,
    imageWidth: Int,
    imageHeight: Int,
    onBack: () -> Unit,
    onMaskReady: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val encoder = MaskPngEncoder()

    val filename = state.uploadedMaskFilename
    LaunchedEffect(filename) {
        if (filename != null) onMaskReady(filename)
    }

    Scaffold(
        topBar = {
            DesktopMaskTopBar(
                onBack = onBack,
                onDone = {
                    val bytes = encoder.encode(
                        segments = state.pathSegments,
                        width = imageWidth,
                        height = imageHeight,
                        inverted = state.isInverted,
                    )
                    viewModel.onUploadMask(bytes)
                },
                isUploading = state.isUploading,
                hasContent = state.hasContent,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            DesktopMaskCanvas(
                state = state,
                onStrokeCompleted = viewModel::onStrokeCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(Spacing.md)
                    .clip(RoundedCornerShape(Spacing.md))
                    .pointerHoverIcon(PointerIcon.Crosshair),
            )
            DesktopMaskToolbar(state, viewModel)
            DesktopBrushSlider(state, viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopMaskTopBar(
    onBack: () -> Unit,
    onDone: () -> Unit,
    isUploading: Boolean,
    hasContent: Boolean,
) {
    TopAppBar(
        title = { Text("Mask Editor") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.padding(Spacing.md))
            } else {
                IconButton(onClick = onDone, enabled = hasContent) {
                    Icon(Icons.Default.Check, contentDescription = "Apply mask")
                }
            }
        },
    )
}

@Composable
private fun DesktopMaskCanvas(
    state: MaskEditorState,
    onStrokeCompleted: (List<Pair<Float, Float>>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val maskColor = Color.Red.copy(alpha = MASK_ALPHA)

    Canvas(
        modifier = modifier
            .pointerInput(state.isEraserMode, state.brushSize) {
                detectDragGestures(
                    onDragStart = { offset -> currentPoints = listOf(offset) },
                    onDrag = { change, _ ->
                        change.consume()
                        currentPoints = currentPoints + change.position
                    },
                    onDragEnd = {
                        val cw = size.width.toFloat()
                        val ch = size.height.toFloat()
                        if (cw > 0 && ch > 0) {
                            val n = currentPoints.map { (it.x / cw) to (it.y / ch) }
                            onStrokeCompleted(n)
                        }
                        currentPoints = emptyList()
                    },
                    onDragCancel = { currentPoints = emptyList() },
                )
            },
    ) {
        drawSegments(state, maskColor)
        drawCurrentStroke(currentPoints, state, maskColor)
    }
}

private fun DrawScope.drawSegments(state: MaskEditorState, maskColor: Color) {
    for (seg in state.pathSegments) {
        val pts = seg.points.map { (x, y) -> Offset(x * size.width, y * size.height) }
        if (pts.isEmpty()) continue
        val sw = seg.brushSize * size.width / REFERENCE_CANVAS_SIZE
        val col = if (seg.isEraser) Color.Transparent else maskColor
        val bm = if (seg.isEraser) BlendMode.Clear else BlendMode.SrcOver
        drawPath(
            buildSmoothPath(pts), col,
            style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round),
            blendMode = bm,
        )
    }
}

private fun DrawScope.drawCurrentStroke(
    currentPoints: List<Offset>,
    state: MaskEditorState,
    maskColor: Color,
) {
    if (currentPoints.isEmpty()) return
    val sw = state.brushSize * size.width / REFERENCE_CANVAS_SIZE
    val col = if (state.isEraserMode) Color.Transparent else maskColor
    val bm = if (state.isEraserMode) BlendMode.Clear else BlendMode.SrcOver
    drawPath(
        buildSmoothPath(currentPoints), col,
        style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round),
        blendMode = bm,
    )
}

private fun buildSmoothPath(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts.first().x, pts.first().y)
    if (pts.size == 1) {
        path.lineTo(pts.first().x + 0.1f, pts.first().y + 0.1f)
        return path
    }
    for (i in 1 until pts.size) {
        val p = pts[i - 1]; val c = pts[i]
        path.quadraticBezierTo(p.x, p.y, (p.x + c.x) / 2f, (p.y + c.y) / 2f)
    }
    path.lineTo(pts.last().x, pts.last().y)
    return path
}

@Composable
private fun DesktopMaskToolbar(state: MaskEditorState, vm: MaskEditorViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = vm::onToggleEraser) {
            Icon(
                Icons.Default.Brush, "Toggle eraser",
                tint = if (state.isEraserMode) ERASER_ICON_COLOR
                else MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = vm::onUndo, enabled = state.canUndo) {
            Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
        }
        IconButton(onClick = vm::onRedo, enabled = state.canRedo) {
            Icon(Icons.AutoMirrored.Filled.Redo, "Redo")
        }
        IconButton(onClick = vm::onClear, enabled = state.hasContent) {
            Icon(Icons.Default.Clear, "Clear")
        }
        IconButton(onClick = vm::onInvertMask) {
            Icon(Icons.Default.InvertColors, "Invert mask")
        }
    }
}

@Composable
private fun DesktopBrushSlider(state: MaskEditorState, vm: MaskEditorViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Brush: ${state.brushSize.toInt()}", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.width(Spacing.sm))
        Slider(
            value = state.brushSize,
            onValueChange = vm::onBrushSizeChanged,
            valueRange = PathSegment.MIN_BRUSH_SIZE..PathSegment.MAX_BRUSH_SIZE,
            modifier = Modifier.weight(1f),
        )
    }
}
