package com.riox432.civitdeck.ui.discovery

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.imageLoader
import coil3.request.ImageRequest
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.thumbnailUrl
import com.riox432.civitdeck.ui.components.ImageErrorPlaceholder
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.shimmer
import kotlin.math.roundToInt
import androidx.compose.animation.core.Spring as SpringSpec

private const val MAX_VISIBLE_CARDS = 3
private const val PREFETCH_COUNT = 5
private const val CARD_OFFSET_STEP = 12f
private const val IMAGE_ASPECT_RATIO = 0.75f
private const val MAX_ROTATION_DEGREES = 15f

private data class ExitingCard(
    val model: Model,
    val direction: SwipeDirection,
    val startOffset: Offset,
)

@Composable
fun CardStack(
    cards: List<Model>,
    onSwiped: (Model, SwipeDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleCards = cards.take(MAX_VISIBLE_CARDS)
    val context = LocalContext.current
    val exitingCards = remember { mutableStateListOf<ExitingCard>() }

    // Prefetch images for upcoming cards beyond the visible stack
    LaunchedEffect(cards) {
        val prefetchCards = cards.drop(MAX_VISIBLE_CARDS).take(PREFETCH_COUNT)
        val imageLoader = context.imageLoader
        prefetchCards.forEach { model ->
            val url = model.modelVersions
                .firstOrNull()?.images?.firstOrNull()?.thumbnailUrl()
            if (url != null) {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // Background cards (non-interactive)
        visibleCards.reversed().forEachIndexed { reversedIdx, model ->
            val cardIndex = visibleCards.size - 1 - reversedIdx
            val offsetY = cardIndex * CARD_OFFSET_STEP

            if (cardIndex == 0) {
                key(model.id) {
                    SwipeCard(
                        onSwiped = { result ->
                            exitingCards.add(
                                ExitingCard(model, result.direction, result.releaseOffset),
                            )
                            onSwiped(model, result.direction)
                        },
                        modifier = Modifier.graphicsLayer { translationY = offsetY },
                    ) {
                        DiscoveryCard(model = model)
                    }
                }
            } else {
                Box(
                    modifier = Modifier.graphicsLayer { translationY = offsetY },
                ) {
                    DiscoveryCard(model = model)
                }
            }
        }

        // Exiting cards overlay — animates out independently
        exitingCards.forEach { exiting ->
            key("exit-${exiting.model.id}") {
                ExitAnimationOverlay(
                    exiting = exiting,
                    onComplete = { exitingCards.remove(exiting) },
                )
            }
        }
    }
}

@Composable
private fun ExitAnimationOverlay(exiting: ExitingCard, onComplete: () -> Unit) {
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val offset = remember { Animatable(exiting.startOffset, Offset.VectorConverter) }

    LaunchedEffect(Unit) {
        val target = when (exiting.direction) {
            SwipeDirection.Right -> Offset(screenWidthPx * 2, exiting.startOffset.y)
            SwipeDirection.Left -> Offset(-screenWidthPx * 2, exiting.startOffset.y)
            SwipeDirection.Up -> Offset(exiting.startOffset.x, -screenHeightPx * 2)
        }
        offset.animateTo(
            target,
            spring(SpringSpec.DampingRatioNoBouncy, SpringSpec.StiffnessMediumLow),
        )
        onComplete()
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
            .graphicsLayer {
                rotationZ = (offset.value.x / screenWidthPx) * MAX_ROTATION_DEGREES
            },
    ) {
        DiscoveryCard(model = exiting.model)
    }
}

@Composable
private fun DiscoveryCard(model: Model) {
    val thumbnailUrl = model.modelVersions
        .firstOrNull()?.images?.firstOrNull()?.thumbnailUrl()

    Card(
        shape = RoundedCornerShape(CornerRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            CardThumbnail(url = thumbnailUrl, name = model.name)
            CardInfo(model = model)
        }
    }
}

@Composable
private fun CardThumbnail(url: String?, name: String) {
    val imageModifier = Modifier.fillMaxWidth().aspectRatio(IMAGE_ASPECT_RATIO)
    if (url != null) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = imageModifier,
        ) {
            when (painter.state) {
                is coil3.compose.AsyncImagePainter.State.Error ->
                    ImageErrorPlaceholder(modifier = imageModifier)
                is coil3.compose.AsyncImagePainter.State.Loading ->
                    Box(modifier = imageModifier.shimmer())
                else -> SubcomposeAsyncImageContent()
            }
        }
    } else {
        ImageErrorPlaceholder(modifier = imageModifier)
    }
}

@Composable
private fun CardInfo(model: Model) {
    Column(modifier = Modifier.padding(Spacing.md)) {
        Text(
            text = model.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = model.type.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        model.creator?.username?.let { username ->
            Text(
                text = "by $username",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
