package com.riox432.civitdeck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.ui.theme.IconSize
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.util.FormatUtils

/**
 * Displays model statistics (downloads, favorites, rating, optional comments).
 *
 * When [commentCount] is null, renders a compact icon+value row (3 items).
 * When [commentCount] is provided, renders an expanded value+label column row (4 items).
 */
@Composable
fun ModelStatsRow(
    downloadCount: Int,
    favoriteCount: Int,
    rating: Double?,
    commentCount: Int? = null,
    modifier: Modifier = Modifier,
) {
    if (commentCount != null) {
        // Expanded (detail) style: value above label, spaced evenly
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatColumn(
                value = FormatUtils.formatCount(downloadCount),
                label = "Downloads",
            )
            StatColumn(
                value = FormatUtils.formatCount(favoriteCount),
                label = "Favorites",
            )
            StatColumn(
                value = if (rating != null) FormatUtils.formatRating(rating) else "-",
                label = "Rating",
            )
            StatColumn(
                value = FormatUtils.formatCount(commentCount),
                label = "Comments",
            )
        }
    } else {
        // Compact style: icon + value inline
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatItem(
                label = FormatUtils.formatCount(downloadCount),
                icon = Icons.Outlined.Download,
            )
            StatItem(
                label = FormatUtils.formatCount(favoriteCount),
                icon = Icons.Outlined.FavoriteBorder,
            )
            StatItem(
                label = if (rating != null) FormatUtils.formatRating(rating) else "-",
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

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
