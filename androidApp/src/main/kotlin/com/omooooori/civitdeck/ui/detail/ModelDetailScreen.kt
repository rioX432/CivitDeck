package com.omooooori.civitdeck.ui.detail

import android.content.Intent
import android.text.Html
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.omooooori.civitdeck.domain.model.Model
import com.omooooori.civitdeck.domain.model.ModelFile
import com.omooooori.civitdeck.domain.model.ModelImage
import com.omooooori.civitdeck.domain.model.ModelVersion
import com.omooooori.civitdeck.ui.util.FormatUtils

@Composable
fun ModelDetailScreen(
    viewModel: ModelDetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ModelDetailTopBar(
                uiState = uiState,
                onBack = onBack,
                onFavoriteToggle = viewModel::onFavoriteToggle,
            )
        },
    ) { padding ->
        ModelDetailBody(
            uiState = uiState,
            onRetry = viewModel::retry,
            onVersionSelected = viewModel::onVersionSelected,
            contentPadding = padding,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelDetailTopBar(
    uiState: ModelDetailUiState,
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    val context = LocalContext.current
    TopAppBar(
        title = {
            Text(
                text = uiState.model?.name ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(
                onClick = {
                    val model = uiState.model ?: return@IconButton
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "https://civitai.com/models/${model.id}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share model"))
                },
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (uiState.isFavorite) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = "Favorite",
                    tint = if (uiState.isFavorite) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        },
    )
}

@Composable
private fun ModelDetailBody(
    uiState: ModelDetailUiState,
    onRetry: () -> Unit,
    onVersionSelected: (Int) -> Unit,
    contentPadding: PaddingValues,
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
        uiState.model != null -> {
            ModelDetailContent(
                model = uiState.model!!,
                selectedVersionIndex = uiState.selectedVersionIndex,
                onVersionSelected = onVersionSelected,
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun ModelDetailContent(
    model: Model,
    selectedVersionIndex: Int,
    onVersionSelected: (Int) -> Unit,
    contentPadding: PaddingValues,
) {
    val selectedVersion = model.modelVersions.getOrNull(selectedVersionIndex)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
    ) {
        // Image carousel
        item {
            ImageCarousel(images = selectedVersion?.images ?: emptyList())
        }

        // Model header
        item {
            ModelHeader(model = model)
        }

        // Stats row
        item {
            StatsRow(model = model)
        }

        // Tags
        if (model.tags.isNotEmpty()) {
            item {
                TagsSection(tags = model.tags)
            }
        }

        // Description
        if (!model.description.isNullOrBlank()) {
            item {
                DescriptionSection(description = model.description!!)
            }
        }

        // Version selector
        if (model.modelVersions.size > 1) {
            item {
                VersionSelector(
                    versions = model.modelVersions,
                    selectedIndex = selectedVersionIndex,
                    onVersionSelected = onVersionSelected,
                )
            }
        }

        // Version detail
        if (selectedVersion != null) {
            item {
                VersionDetail(version = selectedVersion)
            }
        }
    }
}

@Composable
private fun ImageCarousel(images: List<ModelImage>) {
    if (images.isEmpty()) return

    val pagerState = rememberPagerState { images.size }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            AsyncImage(
                model = images[page].url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium),
            )
        }

        if (images.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1} / ${images.size}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ModelHeader(model: Model) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = model.name,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = model.type.name,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
            )
            if (model.creator != null) {
                Text(
                    text = "by ${model.creator!!.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatsRow(model: Model) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatColumn(
            value = FormatUtils.formatCount(model.stats.downloadCount),
            label = "Downloads",
        )
        StatColumn(
            value = FormatUtils.formatCount(model.stats.favoriteCount),
            label = "Favorites",
        )
        StatColumn(
            value = FormatUtils.formatRating(model.stats.rating),
            label = "Rating",
        )
        StatColumn(
            value = FormatUtils.formatCount(model.stats.commentCount),
            label = "Comments",
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(tags: List<String>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Tags",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tags.forEach { tag ->
                SuggestionChip(
                    onClick = {},
                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

@Composable
private fun DescriptionSection(description: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
        Text(
            text = "Description",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        val plainText = Html.fromHtml(description, Html.FROM_HTML_MODE_COMPACT).toString()
        Text(
            text = AnnotatedString(plainText),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun VersionSelector(
    versions: List<ModelVersion>,
    selectedIndex: Int,
    onVersionSelected: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        Text(
            text = "Versions",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(versions) { index, version ->
                FilterChip(
                    selected = index == selectedIndex,
                    onClick = { onVersionSelected(index) },
                    label = { Text(version.name) },
                )
            }
        }
    }
}

@Composable
private fun VersionDetail(version: ModelVersion) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (version.baseModel != null) {
            DetailRow(label = "Base Model", value = version.baseModel!!)
        }

        if (version.trainedWords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Trained Words",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = version.trainedWords.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (version.files.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Files",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            version.files.forEach { file ->
                FileInfoRow(file = file)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun FileInfoRow(file: ModelFile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = FormatUtils.formatFileSize(file.sizeKB),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                file.format?.let { format ->
                    Text(
                        text = format,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                file.fp?.let { fp ->
                    Text(
                        text = fp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
