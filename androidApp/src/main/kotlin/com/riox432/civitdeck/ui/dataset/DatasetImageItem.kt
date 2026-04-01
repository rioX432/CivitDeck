package com.riox432.civitdeck.ui.dataset

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.ImageSource
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

private const val IMAGE_ASPECT_RATIO = 1f

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DatasetImageItem(
    image: DatasetImage,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    callbacks: DatasetGridCallbacks,
    modifier: Modifier = Modifier,
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.combinedClickable(
            onClick = {
                if (isSelectionMode) callbacks.onToggleSelection(image.id) else callbacks.onShowDetail(image)
            },
            onLongClick = { if (!isSelectionMode) showContextMenu = true },
        ),
    ) {
        CivitAsyncImage(
            imageUrl = image.imageUrl,
            contentDescription = stringResource(R.string.cd_dataset_image),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(IMAGE_ASPECT_RATIO)
                .clip(RoundedCornerShape(CornerRadius.image)),
        )
        if (isSelectionMode) {
            DatasetImageSelectionOverlay(isSelected = isSelected)
        } else {
            SourceBadgeMini(
                sourceType = image.sourceType,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.xs),
            )
            if (image.excluded) {
                FlaggedBadge(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.xs),
                )
            }
        }
        ImageContextMenu(
            expanded = showContextMenu,
            onDismiss = { showContextMenu = false },
            onEditCaption = {
                showContextMenu = false
                callbacks.onEditCaption(image)
            },
            onBatchEditTags = {
                showContextMenu = false
                callbacks.onNavigateToBatchTagEditor()
            },
            onSelect = {
                showContextMenu = false
                callbacks.onEnterSelectionMode(image.id)
            },
        )
    }
}

@Composable
internal fun FlaggedBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.dataset_flagged),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onError,
        modifier = modifier
            .clip(RoundedCornerShape(CornerRadius.chip))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
            .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
    )
}

@Composable
internal fun SourceBadgeMini(sourceType: ImageSource, modifier: Modifier = Modifier) {
    val (label, color) = when (sourceType) {
        ImageSource.CIVITAI -> "CI" to MaterialTheme.colorScheme.primaryContainer
        ImageSource.LOCAL -> "LO" to MaterialTheme.colorScheme.secondaryContainer
        ImageSource.GENERATED -> "GN" to MaterialTheme.colorScheme.tertiaryContainer
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .clip(RoundedCornerShape(CornerRadius.chip))
            .background(color.copy(alpha = 0.85f))
            .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
    )
}

@Composable
internal fun ImageContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEditCaption: () -> Unit,
    onBatchEditTags: () -> Unit,
    onSelect: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.dataset_edit_caption)) },
            onClick = onEditCaption,
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.dataset_batch_edit_tags)) },
            onClick = onBatchEditTags,
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.dataset_select)) },
            onClick = onSelect,
        )
    }
}

@Composable
internal fun DatasetImageSelectionOverlay(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .padding(Spacing.sm)
            .size(24.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.cd_selected),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun QualitySummaryChip(
    imageCount: Int,
    duplicateCount: Int,
    lowResCount: Int,
    modifier: Modifier = Modifier,
) {
    val imagesText = stringResource(R.string.dataset_images_count, imageCount)
    val duplicatesText = if (duplicateCount > 0) {
        " • ${stringResource(
            R.string.dataset_duplicates_count,
            duplicateCount
        )}"
    } else {
        ""
    }
    val lowResText = if (lowResCount > 0) {
        " • ${stringResource(R.string.dataset_below_threshold, lowResCount)}"
    } else {
        ""
    }
    val label = "$imagesText$duplicatesText$lowResText"
    SuggestionChip(
        onClick = {},
        label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier,
    )
}
