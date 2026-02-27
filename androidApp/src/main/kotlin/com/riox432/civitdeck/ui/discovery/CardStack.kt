package com.riox432.civitdeck.ui.discovery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.crossfade
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.thumbnailUrl
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.shimmer

private const val MAX_VISIBLE_CARDS = 3
private const val CARD_SCALE_STEP = 0.05f
private const val CARD_OFFSET_STEP = 12f
private const val IMAGE_ASPECT_RATIO = 0.75f

@Composable
fun CardStack(
    cards: List<Model>,
    onSwiped: (Model, SwipeDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleCards = cards.take(MAX_VISIBLE_CARDS)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // Render back-to-front: last card first (bottom), top card last
        visibleCards.reversed().forEachIndexed { reversedIdx, model ->
            val cardIndex = visibleCards.size - 1 - reversedIdx
            val scale = 1f - (cardIndex * CARD_SCALE_STEP)
            val offsetY = cardIndex * CARD_OFFSET_STEP

            if (cardIndex == 0) {
                key(model.id) {
                    SwipeCard(
                        onSwiped = { direction -> onSwiped(model, direction) },
                        modifier = Modifier.graphicsLayer {
                            translationY = offsetY
                            scaleX = scale
                            scaleY = scale
                        },
                    ) {
                        DiscoveryCard(model = model)
                    }
                }
            } else {
                Box(
                    modifier = Modifier.graphicsLayer {
                        translationY = offsetY
                        scaleX = scale
                        scaleY = scale
                    },
                ) {
                    DiscoveryCard(model = model)
                }
            }
        }
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
    if (url != null) {
        SubcomposeAsyncImage(
            model = coil3.request.ImageRequest.Builder(
                androidx.compose.ui.platform.LocalContext.current,
            )
                .data(url)
                .crossfade(Duration.normal)
                .build(),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(IMAGE_ASPECT_RATIO),
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(IMAGE_ASPECT_RATIO)
                        .shimmer(),
                )
            },
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(IMAGE_ASPECT_RATIO)
                .shimmer(),
        )
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
