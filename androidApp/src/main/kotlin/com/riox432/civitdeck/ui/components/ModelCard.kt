package com.riox432.civitdeck.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.ui.navigation.LocalSharedTransitionScope
import com.riox432.civitdeck.ui.navigation.SharedElementKeys
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.shimmer

/**
 * Android ModelCard — thin wrapper around [ModelCardLayout] that adds
 * Coil image loading and optional SharedTransition animation.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
@Suppress("LongParameterList")
fun ModelCard(
    model: Model,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    sharedElementSuffix: String = "",
    isOwned: Boolean = false,
    parallaxOffset: Float = 0f,
    reducedMotion: Boolean = false,
) {
    ModelCardLayout(
        model = model,
        onClick = onClick,
        onLongPress = onLongPress,
        modifier = modifier,
        isOwned = isOwned,
        reducedMotion = reducedMotion,
    ) { thumbnailUrl, contentDescription, imageModifier, onError ->
        if (thumbnailUrl != null) {
            CoilThumbnail(
                thumbnailUrl = thumbnailUrl,
                modelId = model.id,
                contentDescription = contentDescription,
                sharedElementSuffix = sharedElementSuffix,
                parallaxOffset = parallaxOffset,
                reducedMotion = reducedMotion,
                modifier = imageModifier,
                onError = onError,
            )
        } else {
            ImageErrorPlaceholder(
                modifier = imageModifier,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
@Suppress("LongParameterList")
private fun CoilThumbnail(
    thumbnailUrl: String,
    modelId: Long,
    contentDescription: String,
    sharedElementSuffix: String,
    parallaxOffset: Float,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    onError: () -> Unit = {},
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalNavAnimatedContentScope.current

    val imageModifier = if (sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            modifier
                .clipToBounds()
                .sharedElement(
                    rememberSharedContentState(
                        key = SharedElementKeys.modelThumbnail(
                            modelId,
                            sharedElementSuffix,
                        ),
                    ),
                    animatedVisibilityScope = animatedContentScope,
                )
        }
    } else {
        modifier.clipToBounds()
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(thumbnailUrl)
            .crossfade(Duration.normal)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = imageModifier.parallaxEffect(
            scrollOffset = parallaxOffset,
            reducedMotion = reducedMotion,
        ),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shimmer(),
            )
        },
        error = {
            LaunchedEffect(thumbnailUrl) { onError() }
            ImageErrorPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
        },
    )
}
