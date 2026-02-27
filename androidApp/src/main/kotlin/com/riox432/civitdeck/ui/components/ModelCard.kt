package com.riox432.civitdeck.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.SubcomposeAsyncImage
import coil3.request.crossfade
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.thumbnailUrl
import com.riox432.civitdeck.ui.navigation.LocalSharedTransitionScope
import com.riox432.civitdeck.ui.navigation.SharedElementKeys
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.IconSize
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.shimmer

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
    var pressed by remember { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val cardModifier = modifier
        .fillMaxWidth()
        .springScale(pressed = pressed, reducedMotion = reducedMotion)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                },
                onTap = { currentOnClick?.invoke() },
                onLongPress = { currentOnLongPress?.invoke() },
            )
        }
    val shape = RoundedCornerShape(CornerRadius.card)
    val elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    Card(
        modifier = cardModifier,
        shape = shape,
        elevation = elevation,
        colors = colors,
    ) { ModelCardContent(model, sharedElementSuffix, isOwned, parallaxOffset, reducedMotion) }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ModelCardContent(
    model: Model,
    sharedElementSuffix: String,
    isOwned: Boolean = false,
    parallaxOffset: Float = 0f,
    reducedMotion: Boolean = false,
) {
    Column {
        val imageUrls = model.modelVersions
            .firstOrNull()?.images?.map { it.thumbnailUrl() } ?: emptyList()
        var currentImageIndex by remember { mutableIntStateOf(0) }
        val thumbnailUrl = imageUrls.getOrNull(currentImageIndex)

        if (thumbnailUrl != null) {
            ModelCardThumbnail(
                thumbnailUrl = thumbnailUrl,
                modelId = model.id,
                contentDescription = model.name,
                sharedElementSuffix = sharedElementSuffix,
                parallaxOffset = parallaxOffset,
                reducedMotion = reducedMotion,
                onError = {
                    if (currentImageIndex + 1 < imageUrls.size) {
                        currentImageIndex++
                    }
                },
            )
        } else {
            ImageErrorPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
        }

        ModelCardInfo(model = model, isOwned = isOwned)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ModelCardThumbnail(
    thumbnailUrl: String,
    modelId: Long,
    contentDescription: String,
    sharedElementSuffix: String = "",
    parallaxOffset: Float = 0f,
    reducedMotion: Boolean = false,
    onError: () -> Unit = {},
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalNavAnimatedContentScope.current

    val baseModifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clipToBounds()

    val imageModifier = if (sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            baseModifier.sharedElement(
                rememberSharedContentState(
                    key = SharedElementKeys.modelThumbnail(modelId, sharedElementSuffix),
                ),
                animatedVisibilityScope = animatedContentScope,
            )
        }
    } else {
        baseModifier
    }

    SubcomposeAsyncImage(
        model = coil3.request.ImageRequest.Builder(
            androidx.compose.ui.platform.LocalContext.current,
        )
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

@Composable
private fun ModelCardInfo(model: Model, isOwned: Boolean = false) {
    Column(
        modifier = Modifier.padding(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isOwned) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Owned",
                    modifier = Modifier.size(IconSize.statIcon),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Text(
            text = model.type.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(CornerRadius.chip),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )

        ModelStatsRow(
            downloadCount = model.stats.downloadCount,
            favoriteCount = model.stats.favoriteCount,
            rating = model.stats.rating,
        )
    }
}
