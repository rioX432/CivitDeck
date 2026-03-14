package com.riox432.civitdeck.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.shimmer

/**
 * Standard image composable with shimmer placeholder and error fallback.
 *
 * For cases requiring an onError callback, use [SubcomposeAsyncImage] directly
 * (e.g. ModelCardThumbnail's multi-image fallback logic).
 */
@Composable
fun CivitAsyncImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(Duration.normal)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            Box(modifier = Modifier.fillMaxSize().shimmer())
        },
        error = {
            ImageErrorPlaceholder(modifier = Modifier.fillMaxSize())
        },
    )
}
