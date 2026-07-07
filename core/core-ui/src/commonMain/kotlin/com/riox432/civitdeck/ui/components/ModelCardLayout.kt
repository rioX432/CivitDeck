package com.riox432.civitdeck.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.model.browseThumbnailCandidates
import com.riox432.civitdeck.domain.model.thumbnailUrl
import com.riox432.civitdeck.ui.theme.CivitDeckColors
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Elevation
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
    val elevation = CardDefaults.cardElevation(defaultElevation = Elevation.sm)
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
        val candidates = remember(model) { model.browseThumbnailCandidates() }
        var currentImageIndex by remember { mutableIntStateOf(0) }
        val currentImage = candidates.getOrNull(currentImageIndex)
        val thumbnailUrl = currentImage?.thumbnailUrl()

        val imageModifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)

        val blurRadius = cardBlurRadiusFor(currentImage?.nsfwLevel)
        Box {
            Box(modifier = Modifier.blur(blurRadius)) {
                imageContent(
                    thumbnailUrl,
                    model.name,
                    imageModifier,
                ) {
                    if (currentImageIndex + 1 < candidates.size) {
                        currentImageIndex++
                    }
                }
            }
            // Modifier.blur is a no-op below Android 12 (RenderEffect); cover the
            // thumbnail with an opaque scrim there so NSFW content never shows raw.
            if (blurRadius > 0.dp && !isBlurSupported()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
            NsfwLevelBadge(
                nsfwLevel = currentImage?.nsfwLevel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Spacing.sm),
            )
        }

        ModelCardInfoSection(model = model, isOwned = isOwned)
    }
}

/**
 * Blur strength for browse-card thumbnails by the shown image's own NSFW level.
 * Cards use fixed strengths (not the gallery's user sliders) so the grid stays
 * predictable; tapping the card opens the detail where full controls apply.
 */
private fun cardBlurRadiusFor(level: NsfwLevel?): Dp = when (level) {
    NsfwLevel.Mature -> 16.dp
    NsfwLevel.X -> 24.dp
    else -> 0.dp
}

@Composable
private fun NsfwLevelBadge(nsfwLevel: NsfwLevel?, modifier: Modifier = Modifier) {
    if (nsfwLevel != NsfwLevel.Mature && nsfwLevel != NsfwLevel.X) return
    Text(
        text = "NSFW",
        style = MaterialTheme.typography.labelSmall,
        color = CivitDeckColors.onScrim,
        modifier = modifier
            .background(
                color = CivitDeckColors.scrim.copy(alpha = 0.6f),
                shape = RoundedCornerShape(CornerRadius.chip),
            )
            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
    )
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = model.type.name,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(CornerRadius.chip),
                    )
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
            )
            SourceBadge(source = model.source)
        }

        ModelStatsRow(
            downloadCount = model.stats.downloadCount,
            favoriteCount = model.stats.favoriteCount,
            rating = model.stats.rating,
        )
    }
}

@Composable
private fun SourceBadge(source: ModelSource) {
    if (source == ModelSource.CIVITAI) return
    val (label, color) = when (source) {
        ModelSource.HUGGING_FACE -> "HF" to CivitDeckColors.huggingFaceBadge
        ModelSource.TENSOR_ART -> "TA" to CivitDeckColors.tensorArtBadge
        ModelSource.CIVITAI -> return
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .background(
                color = color,
                shape = RoundedCornerShape(CornerRadius.chip),
            )
            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
    )
}
