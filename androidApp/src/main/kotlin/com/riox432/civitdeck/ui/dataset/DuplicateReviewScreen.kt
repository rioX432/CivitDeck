@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.dataset

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.DuplicateGroup
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

private const val IMAGE_ASPECT_RATIO = 1f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateReviewScreen(
    viewModel: DuplicateReviewViewModel,
    onBack: () -> Unit,
) {
    val groups by viewModel.duplicateGroups.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Duplicates") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        DuplicateReviewContent(
            groups = groups,
            onKeep = viewModel::keepImage,
            onRemove = viewModel::removeImage,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun DuplicateReviewContent(
    groups: List<DuplicateGroup>,
    onKeep: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (groups.isEmpty()) {
        EmptyStateMessage(
            icon = Icons.Default.FindReplace,
            title = "No duplicates found",
            subtitle = "All images in this dataset appear to be unique",
            modifier = modifier.fillMaxSize(),
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
        ) {
            itemsIndexed(items = groups, key = { _, group -> group.images.first().id }) { index, group ->
                DuplicateGroupItem(
                    groupIndex = index + 1,
                    group = group,
                    onKeep = onKeep,
                    onRemove = onRemove,
                )
            }
        }
    }
}

@Composable
private fun DuplicateGroupItem(
    groupIndex: Int,
    group: DuplicateGroup,
    onKeep: (Long) -> Unit,
    onRemove: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.card))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = "Group $groupIndex (${group.images.size} images)",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.xs),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            group.images.forEach { image ->
                DuplicateImageCard(
                    image = image,
                    onKeep = { onKeep(image.id) },
                    onRemove = { onRemove(image.id) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DuplicateImageCard(
    image: DatasetImage,
    onKeep: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DuplicateImageThumbnail(image = image)
        DuplicateImageActions(
            isExcluded = image.excluded,
            onKeep = onKeep,
            onRemove = onRemove,
        )
    }
}

@Composable
private fun DuplicateImageThumbnail(image: DatasetImage) {
    Box {
        CivitAsyncImage(
            imageUrl = image.imageUrl,
            contentDescription = stringResource(R.string.cd_duplicate_image),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(IMAGE_ASPECT_RATIO)
                .clip(RoundedCornerShape(CornerRadius.image)),
        )
        if (image.excluded) {
            ExcludedBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Spacing.xs),
            )
        }
    }
}

@Composable
private fun ExcludedBadge(modifier: Modifier = Modifier) {
    Text(
        text = "Excluded",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onError,
        modifier = modifier
            .clip(RoundedCornerShape(CornerRadius.chip))
            .background(MaterialTheme.colorScheme.error)
            .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
    )
}

@Composable
private fun DuplicateImageActions(
    isExcluded: Boolean,
    onKeep: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        OutlinedButton(
            onClick = onKeep,
            modifier = Modifier.weight(1f),
            enabled = isExcluded,
        ) {
            Text(text = "Keep", style = MaterialTheme.typography.labelSmall)
        }
        Button(
            onClick = onRemove,
            modifier = Modifier.weight(1f),
            enabled = !isExcluded,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(text = "Remove", style = MaterialTheme.typography.labelSmall)
        }
    }
}
