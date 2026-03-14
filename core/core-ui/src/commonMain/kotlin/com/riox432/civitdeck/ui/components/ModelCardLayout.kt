package com.riox432.civitdeck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.thumbnailUrl
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.IconSize
import com.riox432.civitdeck.ui.theme.Spacing

/**
 * Platform-agnostic ModelCard layout.
 *
 * The image section is provided via the [imageContent] composable lambda,
 * allowing each platform to supply its own image loading implementation
 * (e.g. Coil on Android, or a custom loader on Desktop).
 *
 * @param imageContent Composable that renders the thumbnail image.
 *   Receives the thumbnail URL (nullable), content description, modifier
 *   (with fillMaxWidth + aspectRatio applied), and an onError callback
 *   for multi-image fallback.
 */
@Composable
@Suppress("LongParameterList")
fun ModelCardLayout(
    model: Model,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isOwned: Boolean = false,
    reducedMotion: Boolean = false,
    imageContent: @Composable (
        thumbnailUrl: String?,
        contentDescription: String,
        modifier: Modifier,
        onError: () -> Unit,
    ) -> Unit,
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
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
    )
    Card(
        modifier = cardModifier,
        shape = shape,
        elevation = elevation,
        colors = colors,
    ) {
        ModelCardLayoutContent(
            model = model,
            isOwned = isOwned,
            imageContent = imageContent,
        )
    }
}

@Composable
private fun ModelCardLayoutContent(
    model: Model,
    isOwned: Boolean,
    imageContent: @Composable (
        thumbnailUrl: String?,
        contentDescription: String,
        modifier: Modifier,
        onError: () -> Unit,
    ) -> Unit,
) {
    Column {
        val imageUrls = model.modelVersions
            .firstOrNull()?.images?.map { it.thumbnailUrl() } ?: emptyList()
        var currentImageIndex by remember { mutableIntStateOf(0) }
        val thumbnailUrl = imageUrls.getOrNull(currentImageIndex)

        val imageModifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)

        imageContent(
            thumbnailUrl,
            model.name,
            imageModifier,
        ) {
            if (currentImageIndex + 1 < imageUrls.size) {
                currentImageIndex++
            }
        }

        ModelCardInfoSection(model = model, isOwned = isOwned)
    }
}

@Composable
private fun ModelCardInfoSection(model: Model, isOwned: Boolean = false) {
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
