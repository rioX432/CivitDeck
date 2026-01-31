package com.omooooori.civitdeck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.omooooori.civitdeck.domain.model.Model
import com.omooooori.civitdeck.ui.util.FormatUtils

@Composable
fun ModelCard(
    model: Model,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            val thumbnailUrl = model.modelVersions
                .firstOrNull()?.images?.firstOrNull()?.url

            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = model.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium),
                )
            }

            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = model.type.name,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatItem(
                        label = FormatUtils.formatCount(model.stats.downloadCount),
                        icon = "downloads",
                    )
                    StatItem(
                        label = FormatUtils.formatCount(model.stats.favoriteCount),
                        icon = "favorites",
                    )
                    StatItem(
                        label = FormatUtils.formatRating(model.stats.rating),
                        icon = "rating",
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    icon: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = when (icon) {
                "downloads" -> "\u2B07"
                "favorites" -> "\u2764"
                "rating" -> "\u2B50"
                else -> ""
            },
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
