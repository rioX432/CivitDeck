@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.feature.comfyui.data.encoder.MaskPngEncoder
import com.riox432.civitdeck.feature.comfyui.domain.model.MaskEditorState
import com.riox432.civitdeck.feature.comfyui.domain.model.PathSegment
import com.riox432.civitdeck.feature.comfyui.presentation.MaskEditorViewModel
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.theme.Spacing

private val ERASER_ICON_COLOR = Color(0xFFFF9800)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaskEditorScreen(
    viewModel: MaskEditorViewModel,
    sourceImageUrl: String,
    imageWidth: Int,
    imageHeight: Int,
    onBack: () -> Unit,
    onMaskReady: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val encoder = MaskPngEncoder()

    MaskUploadEffect(state, onMaskReady)

    Scaffold(
        topBar = {
            MaskEditorTopBar(
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
            MaskCanvasArea(state, sourceImageUrl, viewModel)
            MaskToolbar(state, viewModel)
            BrushSizeSlider(state, viewModel)
        }
    }
}

@Composable
private fun MaskUploadEffect(
    state: MaskEditorState,
    onMaskReady: (String) -> Unit,
) {
    val filename = state.uploadedMaskFilename
    if (filename != null) {
        androidx.compose.runtime.LaunchedEffect(filename) {
            onMaskReady(filename)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaskEditorTopBar(
    onBack: () -> Unit,
    onDone: () -> Unit,
    isUploading: Boolean,
    hasContent: Boolean,
) {
    TopAppBar(
        title = { Text("Mask Editor") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_navigate_back),
                )
            }
        },
        actions = {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(Spacing.md),
                )
            } else {
                IconButton(
                    onClick = onDone,
                    enabled = hasContent,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Apply mask",
                    )
                }
            }
        },
    )
}

@Composable
private fun ColumnScope.MaskCanvasArea(
    state: MaskEditorState,
    sourceImageUrl: String,
    viewModel: MaskEditorViewModel,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(Spacing.md)
            .clip(RoundedCornerShape(Spacing.md)),
    ) {
        // Source image as background
        CivitAsyncImage(
            imageUrl = sourceImageUrl,
            contentDescription = "Source image",
            modifier = Modifier.fillMaxSize(),
        )
        // Mask painting overlay
        MaskPaintCanvas(
            state = state,
            onStrokeCompleted = viewModel::onStrokeCompleted,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun MaskToolbar(
    state: MaskEditorState,
    viewModel: MaskEditorViewModel,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = viewModel::onToggleEraser) {
            Icon(
                Icons.Default.Brush,
                contentDescription = "Toggle eraser",
                tint = if (state.isEraserMode) {
                    ERASER_ICON_COLOR
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        IconButton(onClick = viewModel::onUndo, enabled = state.canUndo) {
            Icon(
                Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.cd_undo),
            )
        }
        IconButton(onClick = viewModel::onRedo, enabled = state.canRedo) {
            Icon(
                Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Redo",
            )
        }
        IconButton(onClick = viewModel::onClear, enabled = state.hasContent) {
            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.cd_clear))
        }
        IconButton(onClick = viewModel::onInvertMask) {
            Icon(Icons.Default.InvertColors, contentDescription = "Invert mask")
        }
    }
}

@Composable
private fun BrushSizeSlider(
    state: MaskEditorState,
    viewModel: MaskEditorViewModel,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Brush: ${state.brushSize.toInt()}",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Slider(
            value = state.brushSize,
            onValueChange = viewModel::onBrushSizeChanged,
            valueRange = PathSegment.MIN_BRUSH_SIZE..PathSegment.MAX_BRUSH_SIZE,
            modifier = Modifier.weight(1f),
        )
    }
}
