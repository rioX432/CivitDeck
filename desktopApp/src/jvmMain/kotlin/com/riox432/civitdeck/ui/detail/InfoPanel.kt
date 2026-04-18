package com.riox432.civitdeck.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import coil3.PlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.util.FormatUtils
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailUiState
import com.riox432.civitdeck.ui.components.ImageErrorPlaceholder
import com.riox432.civitdeck.ui.components.ModelStatsRow
import com.riox432.civitdeck.ui.desktopFocusRing
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.shimmer

@Composable
internal fun InfoPanel(
    model: Model,
    uiState: ModelDetailUiState,
    selectedVersion: ModelVersion?,
    onVersionSelected: (Int) -> Unit,
    onCreatorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item { ModelInfoHeader(model = model, onCreatorClick = onCreatorClick) }
        item {
            ModelStatsRow(
                downloadCount = model.stats.downloadCount,
                favoriteCount = model.stats.favoriteCount,
                rating = model.stats.rating,
                commentCount = model.stats.commentCount,
            )
        }
        if (model.modelVersions.size > 1) {
            item {
                VersionDropdown(
                    versions = model.modelVersions,
                    selectedIndex = uiState.selectedVersionIndex,
                    onVersionSelected = onVersionSelected,
                )
            }
        }
        if (selectedVersion != null) {
            item { VersionInfo(version = selectedVersion) }
        }
        if (model.tags.isNotEmpty()) {
            item { TagsSection(tags = model.tags) }
        }
        if (selectedVersion != null && selectedVersion.files.isNotEmpty()) {
            item { FilesSection(files = selectedVersion.files) }
        }
        if (!model.description.isNullOrBlank()) {
            item { DescriptionSection(description = model.description!!) }
        }
    }
}

@Composable
private fun ModelInfoHeader(
    model: Model,
    onCreatorClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = model.name,
            style = MaterialTheme.typography.headlineSmall,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = model.type.name,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(CornerRadius.chip),
                    )
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            )
            model.creator?.let { creator ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.desktopFocusRing().clickable { onCreatorClick(creator.username) },
                ) {
                    creator.image?.let { avatarUrl ->
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(PlatformContext.INSTANCE)
                                .data(avatarUrl)
                                .size(Size(AVATAR_IMAGE_SIZE, AVATAR_IMAGE_SIZE))
                                .build(),
                            contentDescription = creator.username,
                            modifier = Modifier
                                .size(AVATAR_SIZE)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            loading = { Box(Modifier.fillMaxSize().shimmer()) },
                            error = { ImageErrorPlaceholder(modifier = Modifier.fillMaxSize()) },
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                    }
                    Text(
                        text = creator.username,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionDropdown(
    versions: List<ModelVersion>,
    selectedIndex: Int,
    onVersionSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedVersion = versions.getOrNull(selectedIndex)

    Column {
        Text("Version", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(Spacing.xs))
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(selectedVersion?.name ?: "Select version")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                versions.forEachIndexed { index, version ->
                    DropdownMenuItem(
                        text = { Text(version.name) },
                        onClick = {
                            onVersionSelected(index)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionInfo(version: ModelVersion) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        version.baseModel?.let { base ->
            Text(
                text = "Base: $base",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (version.trainedWords.isNotEmpty()) {
            Text(
                text = "Trigger: ${version.trainedWords.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(tags: List<String>) {
    Column {
        Text("Tags", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(Spacing.xs))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            tags.forEach { tag ->
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(CornerRadius.chip),
                        )
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun FilesSection(
    files: List<com.riox432.civitdeck.domain.model.ModelFile>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Files", style = MaterialTheme.typography.titleSmall)
        files.forEach { file ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = FormatUtils.formatFileSize(file.sizeKB),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DescriptionSection(description: String) {
    Column {
        Text("Description", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
