package com.riox432.civitdeck.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.SubcomposeAsyncImage
import coil3.request.crossfade
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.ui.navigation.LocalSharedTransitionScope
import com.riox432.civitdeck.ui.navigation.SharedElementKeys
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.IconSize
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.shimmer
import com.riox432.civitdeck.util.FormatUtils

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ModelCard(
    model: Model,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedElementSuffix: String = "",
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column {
            val thumbnailUrl = model.modelVersions
                .firstOrNull()?.images?.firstOrNull()?.url

            if (thumbnailUrl != null) {
                ModelCardThumbnail(
                    thumbnailUrl = thumbnailUrl,
                    modelId = model.id,
                    contentDescription = model.name,
                    sharedElementSuffix = sharedElementSuffix,
                )
            } else {
                ImageErrorPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
            }

            ModelCardInfo(model = model)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ModelCardThumbnail(
    thumbnailUrl: String,
    modelId: Long,
    contentDescription: String,
    sharedElementSuffix: String = "",
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalNavAnimatedContentScope.current

    val imageModifier = if (sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
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
            .aspectRatio(1f)
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
        modifier = imageModifier,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shimmer(),
            )
        },
        error = {
            ImageErrorPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
        },
    )
}

@Composable
private fun ModelCardInfo(model: Model) {
    Column(
        modifier = Modifier.padding(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = model.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

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

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatItem(
                label = FormatUtils.formatCount(model.stats.downloadCount),
                icon = Icons.Outlined.Download,
            )
            StatItem(
                label = FormatUtils.formatCount(model.stats.favoriteCount),
                icon = Icons.Outlined.FavoriteBorder,
            )
            StatItem(
                label = FormatUtils.formatRating(model.stats.rating),
                icon = Icons.Outlined.Star,
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(IconSize.statIcon),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
