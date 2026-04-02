package com.riox432.civitdeck.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.filterByNsfwLevel
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailUiState
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.ui.components.ErrorStateView
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.theme.Elevation
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun DesktopDetailScreen(
    viewModel: ModelDetailViewModel,
    onBack: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onCreatorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        DetailTopBar(onBack = onBack, title = uiState.model?.name ?: "Loading...")

        when {
            uiState.isLoading -> LoadingStateOverlay()
            uiState.error != null -> ErrorStateView(
                message = uiState.error ?: "Unknown error",
                onRetry = viewModel::retry,
            )
            uiState.model != null -> DetailBody(
                model = uiState.model!!,
                uiState = uiState,
                onVersionSelected = viewModel::onVersionSelected,
                onImageClick = onImageClick,
                onCreatorClick = onCreatorClick,
            )
        }
    }
}

@Composable
private fun DetailTopBar(
    onBack: () -> Unit,
    title: String,
) {
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
private fun DetailBody(
    model: Model,
    uiState: ModelDetailUiState,
    onVersionSelected: (Int) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onCreatorClick: (String) -> Unit,
) {
    val selectedVersion = model.modelVersions.getOrNull(uiState.selectedVersionIndex)
    val images = (selectedVersion?.images ?: emptyList())
        .filterByNsfwLevel(uiState.nsfwFilterLevel)
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val panelWidth = (maxWidth * INFO_PANEL_FRACTION)
            .coerceIn(INFO_PANEL_MIN_WIDTH, INFO_PANEL_MAX_WIDTH)

        Row(modifier = Modifier.fillMaxSize()) {
            // Left: image grid + metadata panel
            Column(modifier = Modifier.weight(1f)) {
                ImageGridPanel(
                    images = images,
                    selectedIndex = selectedImageIndex,
                    onImageSelect = { index -> selectedImageIndex = index },
                    onImageFullscreen = onImageClick,
                    modifier = Modifier.weight(1f),
                )
                selectedImageIndex?.let { index ->
                    val image = images.getOrNull(index)
                    if (image != null) {
                        ImageMetadataPanel(
                            image = image,
                            onClose = { selectedImageIndex = null },
                            onOpenFullscreen = {
                                onImageClick(images.map { it.url }, index)
                            },
                        )
                    }
                }
            }

            // Right: info panel
            InfoPanel(
                model = model,
                uiState = uiState,
                selectedVersion = selectedVersion,
                onVersionSelected = onVersionSelected,
                onCreatorClick = onCreatorClick,
                modifier = Modifier.width(panelWidth),
            )
        }
    }
}

internal const val INFO_PANEL_FRACTION = 0.35f
internal val INFO_PANEL_MIN_WIDTH = 280.dp
internal val INFO_PANEL_MAX_WIDTH = 400.dp
internal val IMAGE_GRID_MIN_SIZE = 180.dp
internal val AVATAR_SIZE = 24.dp
internal val METADATA_PANEL_HEIGHT = 200.dp
internal val ICON_SIZE = 20.dp
internal const val DETAIL_GRID_IMAGE_SIZE = 360
internal const val AVATAR_IMAGE_SIZE = 48
internal const val MAX_MONOSPACE_LINES = 5
internal const val MAX_NORMAL_LINES = 2
