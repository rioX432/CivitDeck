package com.riox432.civitdeck.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.ui.components.ImageErrorPlaceholder
import com.riox432.civitdeck.ui.components.ModelCardLayout
import com.riox432.civitdeck.ui.components.NsfwBlurOverlay
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
        modifier = modifier,
    ) { thumbnailUrl, contentDescription, imageModifier, onError ->
        NsfwBlurOverlay(
            nsfwLevel = nsfwLevel,
            blurSettings = nsfwBlurSettings,
        ) {
            SubcomposeAsyncImage(
                model = thumbnailUrl,
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
