package com.riox432.civitdeck.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.RatingTotals
import com.riox432.civitdeck.domain.model.ResourceReview
import com.riox432.civitdeck.domain.model.ReviewSortOrder
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun ReviewsSection(
    reviews: List<ResourceReview>,
    ratingTotals: RatingTotals?,
    sortOrder: ReviewSortOrder,
    isLoading: Boolean,
    onSortChanged: (ReviewSortOrder) -> Unit,
    onWriteReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
    ) {
        HorizontalDivider(modifier = Modifier.padding(bottom = Spacing.sm))

        ReviewsSectionHeader(
            totalCount = ratingTotals?.total ?: 0,
            sortOrder = sortOrder,
            onSortChanged = onSortChanged,
            onWriteReview = onWriteReview,
        )

        if (ratingTotals != null && ratingTotals.total > 0) {
            Spacer(Modifier.height(Spacing.sm))
            ThumbsDistributionChart(totals = ratingTotals)
        }

        Spacer(Modifier.height(Spacing.md))

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(Spacing.lg),
                strokeWidth = 2.dp,
            )
        } else if (reviews.isEmpty()) {
            Text(
                text = "No reviews yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            reviews.take(MAX_VISIBLE_REVIEWS).forEach { review ->
                key(review.id) {
                    ReviewCard(review = review)
                    Spacer(Modifier.height(Spacing.sm))
                }
            }
        }
    }
}

@Composable
private fun ReviewsSectionHeader(
    totalCount: Int,
    sortOrder: ReviewSortOrder,
    onSortChanged: (ReviewSortOrder) -> Unit,
    onWriteReview: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Reviews ($totalCount)",
            style = MaterialTheme.typography.titleSmall,
        )
        Row {
            SortDropdown(sortOrder = sortOrder, onSortChanged = onSortChanged)
            TextButton(onClick = onWriteReview) {
                Text("Write", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SortDropdown(
    sortOrder: ReviewSortOrder,
    onSortChanged: (ReviewSortOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = sortOrder.label(),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ReviewSortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label()) },
                    onClick = {
                        onSortChanged(order)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ThumbsDistributionChart(
    totals: RatingTotals,
    modifier: Modifier = Modifier,
) {
    val maxCount = maxOf(totals.thumbsUp, totals.thumbsDown, 1)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        ThumbsBar(
            icon = Icons.Outlined.ThumbUp,
            label = "Recommended",
            count = totals.thumbsUp,
            maxCount = maxCount,
            tint = MaterialTheme.colorScheme.primary,
        )
        ThumbsBar(
            icon = Icons.Outlined.ThumbDown,
            label = "Not recommended",
            count = totals.thumbsDown,
            maxCount = maxCount,
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ThumbsBar(
    icon: ImageVector,
    label: String,
    count: Int,
    maxCount: Int,
    tint: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(Spacing.xs))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(BAR_HEIGHT)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(BAR_HEIGHT)
                    .clip(RoundedCornerShape(2.dp))
                    .background(tint),
            )
        }
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
    }
}

@Composable
fun ReviewCard(review: ResourceReview, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.card))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(Spacing.md),
    ) {
        ReviewCardHeader(review = review)
        ReviewCardRecommendation(recommended = review.recommended)
        ReviewCardBody(details = review.details, createdAt = review.createdAt)
    }
}

@Composable
private fun ReviewCardHeader(review: ResourceReview) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (review.username?.firstOrNull() ?: '?').uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = review.username ?: "Anonymous",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (review.recommended) Icons.Outlined.ThumbUp else Icons.Outlined.ThumbDown,
            contentDescription = if (review.recommended) "Recommended" else "Not recommended",
            modifier = Modifier.size(14.dp),
            tint = if (review.recommended) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
    }
}

@Composable
private fun ReviewCardRecommendation(recommended: Boolean) {
    Spacer(Modifier.height(Spacing.xs))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (recommended) Icons.Outlined.ThumbUp else Icons.Outlined.ThumbDown,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = if (recommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = if (recommended) "Recommended" else "Not Recommended",
            style = MaterialTheme.typography.labelSmall,
            color = if (recommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ReviewCardBody(details: String?, createdAt: String) {
    if (!details.isNullOrBlank()) {
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = details,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(Spacing.xs))
    Text(
        text = formatReviewDate(createdAt),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
    )
}

private fun formatReviewDate(isoDate: String): String = isoDate.take(DATE_PREFIX_LENGTH)

private fun ReviewSortOrder.label(): String = when (this) {
    ReviewSortOrder.Newest -> "Newest"
    ReviewSortOrder.HighestRated -> "Highest"
    ReviewSortOrder.LowestRated -> "Lowest"
}

private val BAR_HEIGHT = 6.dp
private const val MAX_VISIBLE_REVIEWS = 5
private const val DATE_PREFIX_LENGTH = 10
