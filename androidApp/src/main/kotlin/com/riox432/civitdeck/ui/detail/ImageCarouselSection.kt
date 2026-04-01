package com.riox432.civitdeck.ui.detail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.MediaContentType
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.ui.components.ImageErrorPlaceholder
import com.riox432.civitdeck.ui.navigation.LocalSharedTransitionScope
import com.riox432.civitdeck.ui.navigation.SharedElementKeys
import com.riox432.civitdeck.ui.theme.CivitDeckColors
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.IconSize
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.shimmer

internal const val CAROUSEL_ASPECT_RATIO = 1f

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun CarouselWithGridButton(
    images: List<ModelImage>,
    modelId: Long,
    sharedElementSuffix: String,
    onImageClick: (Int) -> Unit,
    onImageError: (String) -> Unit,
    onShowGrid: () -> Unit,
) {
    val pagerState = rememberPagerState { images.size }
    Box {
        ImageCarousel(
            images = images,
            modelId = modelId,
            sharedElementSuffix = sharedElementSuffix,
            pagerState = pagerState,
            onImageClick = onImageClick,
            onImageError = onImageError,
        )
        if (images.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.sm)
                    .clip(RoundedCornerShape(CornerRadius.chip))
                    .background(CivitDeckColors.scrim.copy(alpha = 0.55f))
                    .clickable(onClick = onShowGrid)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = stringResource(R.string.cd_view_all_images),
                    tint = CivitDeckColors.onScrim,
                    modifier = Modifier.size(IconSize.small),
                )
                Text(
                    text = "${pagerState.currentPage + 1}/${images.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CivitDeckColors.onScrim,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedThumbnailPlaceholder(
    thumbnailUrl: String,
    modelId: Long,
    sharedElementSuffix: String = "",
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalNavAnimatedContentScope.current

    val imageModifier = if (sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            Modifier
                .fillMaxWidth()
                .aspectRatio(CAROUSEL_ASPECT_RATIO)
                .sharedElement(
                    rememberSharedContentState(
                        key = SharedElementKeys.modelThumbnail(modelId, sharedElementSuffix),
                    ),
                    animatedVisibilityScope = animatedContentScope,
                )
        }
    } else {
        Modifier
            .fillMaxWidth()
            .aspectRatio(CAROUSEL_ASPECT_RATIO)
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(thumbnailUrl)
            .crossfade(Duration.normal)
            .build(),
        contentDescription = stringResource(R.string.cd_model_thumbnail),
        contentScale = ContentScale.Fit,
        modifier = imageModifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(CAROUSEL_ASPECT_RATIO)
                    .shimmer(),
            )
        },
        error = {
            ImageErrorPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(CAROUSEL_ASPECT_RATIO),
            )
        },
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ImageCarousel(
    images: List<ModelImage>,
    modelId: Long,
    pagerState: PagerState,
    sharedElementSuffix: String = "",
    onImageClick: (Int) -> Unit = {},
    onImageError: (String) -> Unit = {},
) {
    if (images.isEmpty()) return

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
    ) { page ->
        CarouselPage(
            image = images[page],
            modelId = modelId,
            sharedElementSuffix = sharedElementSuffix,
            applySharedElement = page == pagerState.currentPage,
            onClick = { onImageClick(page) },
            onError = { onImageError(images[page].url) },
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CarouselPage(
    image: ModelImage,
    modelId: Long,
    sharedElementSuffix: String = "",
    applySharedElement: Boolean,
    onClick: () -> Unit = {},
    onError: () -> Unit = {},
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalNavAnimatedContentScope.current

    val pageModifier = if (applySharedElement && sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            Modifier
                .fillMaxWidth()
                .aspectRatio(CAROUSEL_ASPECT_RATIO)
                .clip(MaterialTheme.shapes.medium)
                .sharedElement(
                    rememberSharedContentState(
                        key = SharedElementKeys.modelThumbnail(modelId, sharedElementSuffix),
                    ),
                    animatedVisibilityScope = animatedContentScope,
                )
        }
    } else {
        Modifier
            .fillMaxWidth()
            .aspectRatio(CAROUSEL_ASPECT_RATIO)
            .clip(MaterialTheme.shapes.medium)
    }

    CarouselImage(image = image, modifier = pageModifier, onClick = onClick, onError = onError)
}

@Composable
private fun CarouselImage(
    image: ModelImage,
    modifier: Modifier,
    onClick: () -> Unit,
    onError: () -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(image.url)
                .crossfade(Duration.normal)
                .build(),
            contentDescription = stringResource(R.string.cd_model_image),
            contentScale = ContentScale.Fit,
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable(onClick = onClick),
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(CAROUSEL_ASPECT_RATIO)
                        .shimmer(),
                )
            },
            error = {
                LaunchedEffect(image.url) { onError() }
                ImageErrorPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(CAROUSEL_ASPECT_RATIO),
                )
            },
        )
        if (image.contentType == MediaContentType.VIDEO) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = stringResource(R.string.cd_video),
                tint = CivitDeckColors.onScrim,
                modifier = Modifier.size(IconSize.large),
            )
        }
    }
}
