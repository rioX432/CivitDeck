package com.riox432.civitdeck.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.layout.ContentScale
import coil3.PlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.ui.components.ImageErrorPlaceholder
import com.riox432.civitdeck.ui.components.ModelCardLayout
import com.riox432.civitdeck.ui.desktopFocusRing
import com.riox432.civitdeck.ui.theme.shimmer

/**
 * Desktop model card. NSFW blur and badge are handled inside the shared
 * [ModelCardLayout] based on the actually shown (safest-first) image.
 */
@Composable
fun DesktopModelCard(
    model: Model,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModelCardLayout(
        model = model,
        onClick = onClick,
        modifier = modifier.desktopFocusRing().focusTarget(),
    ) { thumbnailUrl, contentDescription, imageModifier, onError ->
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(PlatformContext.INSTANCE)
                .data(thumbnailUrl)
                // Explicit key so detail/viewer requests can reference this cache
                // entry via placeholderMemoryCacheKey for a seamless handoff.
                .memoryCacheKey(thumbnailUrl)
                .size(Size(GRID_THUMBNAIL_SIZE, GRID_THUMBNAIL_SIZE))
                .build(),
            contentDescription = contentDescription,
            modifier = imageModifier,
            contentScale = ContentScale.Crop,
            loading = {
                Box(modifier = imageModifier.shimmer())
            },
            error = {
                // Advance to the next thumbnail candidate on load failure
                // (same multi-candidate fallback as the Android card).
                LaunchedEffect(thumbnailUrl) { onError() }
                ImageErrorPlaceholder(modifier = imageModifier)
            },
        )
    }
}

private const val GRID_THUMBNAIL_SIZE = 400
