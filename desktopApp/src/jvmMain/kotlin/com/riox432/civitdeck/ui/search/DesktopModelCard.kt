package com.riox432.civitdeck.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.PlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import androidx.compose.ui.focus.focusTarget
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.ui.components.ImageErrorPlaceholder
import com.riox432.civitdeck.ui.components.ModelCardLayout
import com.riox432.civitdeck.ui.components.NsfwBlurOverlay
import com.riox432.civitdeck.ui.desktopFocusRing
import com.riox432.civitdeck.ui.theme.shimmer

@Composable
fun DesktopModelCard(
    model: Model,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    nsfwBlurSettings: NsfwBlurSettings = NsfwBlurSettings(),
) {
    val nsfwLevel = model.modelVersions.firstOrNull()?.images?.firstOrNull()?.nsfwLevel
        ?: NsfwLevel.None

    ModelCardLayout(
        model = model,
        onClick = onClick,
        modifier = modifier.desktopFocusRing().focusTarget(),
    ) { thumbnailUrl, contentDescription, imageModifier, onError ->
        NsfwBlurOverlay(
            nsfwLevel = nsfwLevel,
            blurSettings = nsfwBlurSettings,
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(PlatformContext.INSTANCE)
                    .data(thumbnailUrl)
                    .size(Size(GRID_THUMBNAIL_SIZE, GRID_THUMBNAIL_SIZE))
                    .build(),
                contentDescription = contentDescription,
                modifier = imageModifier,
                contentScale = ContentScale.Crop,
                loading = {
                    Box(modifier = imageModifier.shimmer())
                },
                error = {
                    ImageErrorPlaceholder(modifier = imageModifier)
                },
            )
        }
    }
}

private const val GRID_THUMBNAIL_SIZE = 400
