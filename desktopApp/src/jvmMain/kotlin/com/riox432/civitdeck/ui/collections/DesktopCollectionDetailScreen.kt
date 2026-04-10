package com.riox432.civitdeck.ui.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.PlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.riox432.civitdeck.domain.model.FavoriteModelSummary
import com.riox432.civitdeck.feature.collections.presentation.CollectionDetailViewModel
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.shimmer
import com.riox432.civitdeck.ui.theme.Elevation
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.clickable

@Composable
fun DesktopCollectionDetailScreen(
    viewModel: CollectionDetailViewModel,
    collectionName: String,
    onBack: () -> Unit,
    onModelClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val models by viewModel.displayModels.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        DetailTopBar(title = collectionName, onBack = onBack)
        if (models.isEmpty()) {
            EmptyCollectionView()
        } else {
            ModelGrid(models = models, onModelClick = onModelClick)
        }
    }
}

@Composable
private fun DetailTopBar(title: String, onBack: () -> Unit) {
    Surface(tonalElevation = Elevation.xs) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EmptyCollectionView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No models in this collection",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModelGrid(
    models: List<FavoriteModelSummary>,
    onModelClick: (Long) -> Unit,
) {
    val displayViewModel: DisplaySettingsViewModel = koinViewModel()
    val displayState by displayViewModel.uiState.collectAsState()
    val columns = displayState.gridColumns

    LazyVerticalGrid(
        columns = if (columns > 0) GridCells.Fixed(columns) else GridCells.Adaptive(minSize = CARD_MIN_WIDTH),
        contentPadding = PaddingValues(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items = models, key = { it.id }) { model ->
            CollectionModelCard(
                model = model,
                onClick = { onModelClick(model.id) },
            )
        }
    }
}

@Composable
private fun CollectionModelCard(
    model: FavoriteModelSummary,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(CornerRadius.card),
        tonalElevation = Elevation.xs,
    ) {
        Column {
            if (model.thumbnailUrl != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(PlatformContext.INSTANCE)
                        .data(model.thumbnailUrl)
                        .size(Size(COLLECTION_CARD_IMAGE_SIZE, COLLECTION_CARD_IMAGE_SIZE))
                        .build(),
                    contentDescription = model.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = CornerRadius.card, topEnd = CornerRadius.card)),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(modifier = Modifier.fillMaxWidth().shimmer())
                    },
                )
            }
            Column(modifier = Modifier.padding(Spacing.sm)) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = model.type.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val CARD_MIN_WIDTH = 200.dp
private const val COLLECTION_CARD_IMAGE_SIZE = 400
