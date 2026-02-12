package com.riox432.civitdeck.ui.gallery

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.ui.components.ImageErrorPlaceholder
import com.riox432.civitdeck.ui.theme.Duration
import kotlinx.coroutines.launch
import kotlin.math.abs

data class ViewerImage(
    val url: String,
    val meta: ImageGenerationMeta? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerOverlay(
    images: List<ViewerImage>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onSavePrompt: (ImageGenerationMeta, String?) -> Unit = { _, _ -> },
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BackHandler(onBack = onDismiss)
        ImageViewerContent(images, initialIndex, onDismiss, onSavePrompt)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageViewerContent(
    images: List<ViewerImage>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onSavePrompt: (ImageGenerationMeta, String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }
    var showMetadata by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentDragY by remember { mutableFloatStateOf(0f) }

    Box(Modifier.fillMaxSize()) {
        // Layer 1: Background (stays in place, fades with drag)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = backgroundAlpha(currentDragY))))

        // Layer 2: Image pager
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            ZoomableImage(
                imageUrl = images[page].url,
                onDismiss = if (page == pagerState.currentPage) onDismiss else null,
                onDragYChanged = { if (page == pagerState.currentPage) currentDragY = it },
                onTap = { controlsVisible = !controlsVisible },
            )
        }

        // Layer 3: Controls (stays in place)
        OverlayControls(
            images,
            pagerState,
            controlsVisible && currentDragY == 0f,
            onDismiss,
            onInfoClick = { showMetadata = true },
            onShareClick = { shareImage(context, it.url, it.meta) },
            onDownloadClick = { image ->
                scope.launch {
                    val success = ImageDownloader.download(context, image.url)
                    val msg = if (success) "Saved to Pictures" else "Download failed"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    ViewerMetadataSheet(images, pagerState, sheetState, showMetadata, { showMetadata = false }, onSavePrompt)
}

private fun backgroundAlpha(dragY: Float): Float {
    return (1f - abs(dragY) / BG_FADE_DISTANCE).coerceIn(0f, 1f)
}

@Composable
private fun ZoomableImage(
    imageUrl: String,
    onDismiss: (() -> Unit)?,
    onDragYChanged: (Float) -> Unit,
    onTap: () -> Unit,
) {
    val scale = remember { Animatable(MIN_ZOOM) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val dragY = remember { Animatable(0f) }
    var containerHeight by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl).crossfade(Duration.normal).build(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerHeight = it.height.toFloat() }
            .pointerInput(onDismiss) {
                detectZoomableGestures(
                    onTap = { onTap() },
                    onDoubleTap = { handleDoubleTap(scope, scale, offset) },
                    onTransform = { _, pan, zoom -> handleTransform(scope, scale, offset, pan, zoom) },
                    onSwipeVertical = { delta ->
                        scope.launch {
                            dragY.snapTo(dragY.value + delta)
                            onDragYChanged(dragY.value)
                        }
                    },
                    onSwipeEnd = { total ->
                        handleSwipeEnd(scope, dragY, containerHeight, total, onDismiss, onDragYChanged)
                    },
                    canPan = { scale.value > MIN_ZOOM },
                )
            }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationX = offset.value.x
                translationY = offset.value.y + dragY.value
            },
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        },
        error = {
            ImageErrorPlaceholder(
                modifier = Modifier.fillMaxSize(),
                iconTint = Color.White,
                backgroundColor = Color.Transparent,
            )
        },
    )
}

private fun handleDoubleTap(
    scope: kotlinx.coroutines.CoroutineScope,
    scale: Animatable<Float, *>,
    offset: Animatable<Offset, *>,
) {
    scope.launch {
        if (scale.value > MIN_ZOOM + 0.01f) {
            kotlinx.coroutines.coroutineScope {
                launch { scale.animateTo(MIN_ZOOM, tween(ZOOM_ANIM_MS)) }
                launch { offset.animateTo(Offset.Zero, tween(ZOOM_ANIM_MS)) }
            }
        } else {
            scale.animateTo(DOUBLE_TAP_ZOOM, tween(ZOOM_ANIM_MS))
        }
    }
}

private fun handleTransform(
    scope: kotlinx.coroutines.CoroutineScope,
    scale: Animatable<Float, *>,
    offset: Animatable<Offset, *>,
    pan: Offset,
    zoom: Float,
) {
    scope.launch {
        val next = (scale.value * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
        scale.snapTo(next)
        offset.snapTo(if (next > MIN_ZOOM) offset.value + pan else Offset.Zero)
    }
}

private fun handleSwipeEnd(
    scope: kotlinx.coroutines.CoroutineScope,
    dragY: Animatable<Float, *>,
    containerHeight: Float,
    totalSwipe: Float,
    onDismiss: (() -> Unit)?,
    onDragYChanged: (Float) -> Unit,
) {
    val threshold = containerHeight * DISMISS_THRESHOLD_RATIO
    if (abs(totalSwipe) > threshold && onDismiss != null) {
        onDismiss()
    } else {
        scope.launch {
            dragY.animateTo(0f, tween(SNAP_BACK_MS))
            onDragYChanged(0f)
        }
    }
}

@Composable
private fun OverlayControls(
    images: List<ViewerImage>,
    pagerState: PagerState,
    visible: Boolean,
    onDismiss: () -> Unit,
    onInfoClick: () -> Unit,
    onShareClick: (ViewerImage) -> Unit,
    onDownloadClick: (ViewerImage) -> Unit,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        val currentImage = images.getOrNull(pagerState.currentPage)
        ViewerControls(
            onDismiss = onDismiss,
            onInfoClick = onInfoClick,
            onShareClick = { currentImage?.let(onShareClick) },
            onDownloadClick = { currentImage?.let(onDownloadClick) },
            hasMetadata = currentImage?.meta != null,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewerMetadataSheet(
    images: List<ViewerImage>,
    pagerState: PagerState,
    sheetState: androidx.compose.material3.SheetState,
    showMetadata: Boolean,
    onDismiss: () -> Unit,
    onSavePrompt: (ImageGenerationMeta, String?) -> Unit,
) {
    if (showMetadata) {
        val currentImage = images.getOrNull(pagerState.currentPage)
        currentImage?.meta?.let { meta ->
            MetadataBottomSheet(
                meta = meta,
                sheetState = sheetState,
                onDismiss = onDismiss,
                onSavePrompt = { onSavePrompt(meta, currentImage.url) },
            )
        }
    }
}

@Composable
private fun ViewerControls(
    onDismiss: () -> Unit,
    onInfoClick: () -> Unit,
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit,
    hasMetadata: Boolean,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }

        Row(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            ControlButton(onClick = onDownloadClick, icon = Icons.Outlined.Download, label = "Download")
            Spacer(modifier = Modifier.width(8.dp))
            ControlButton(onClick = onShareClick, icon = Icons.Default.Share, label = "Share")
            if (hasMetadata) {
                Spacer(modifier = Modifier.width(8.dp))
                ControlButton(onClick = onInfoClick, icon = Icons.Default.Info, label = "Metadata")
            }
        }
    }
}

@Composable
private fun ControlButton(onClick: () -> Unit, icon: ImageVector, label: String) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(icon, contentDescription = label)
    }
}

private fun shareImage(
    context: android.content.Context,
    imageUrl: String,
    meta: ImageGenerationMeta?,
) {
    val text = formatShareText(imageUrl, meta)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share image"))
}

private fun formatShareText(imageUrl: String, meta: ImageGenerationMeta?): String {
    return buildString {
        appendLine(imageUrl)
        if (meta != null) {
            appendLine()
            meta.prompt?.let { appendLine("Prompt: $it") }
            meta.negativePrompt?.let { appendLine("Negative: $it") }
            val params = listOfNotNull(
                meta.model?.let { "Model: $it" },
                meta.steps?.let { "Steps: $it" },
                meta.cfgScale?.let { "CFG: $it" },
                meta.sampler?.let { "Sampler: $it" },
            )
            if (params.isNotEmpty()) {
                appendLine(params.joinToString(" | "))
            }
        }
        appendLine()
        append("Shared via CivitDeck")
    }
}

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f
private const val DISMISS_THRESHOLD_RATIO = 0.18f
private const val BG_FADE_DISTANCE = 600f
private const val ZOOM_ANIM_MS = 200
private const val SNAP_BACK_MS = 180
