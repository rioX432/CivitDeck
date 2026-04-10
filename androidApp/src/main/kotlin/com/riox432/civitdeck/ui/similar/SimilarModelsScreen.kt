package com.riox432.civitdeck.ui.similar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.feature.search.presentation.SimilarModelsUiState
import com.riox432.civitdeck.feature.search.presentation.SimilarModelsViewModel
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.components.ErrorStateView
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.components.ModelCard
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimilarModelsScreen(
    viewModel: SimilarModelsViewModel,
    onBack: () -> Unit,
    onModelClick: (Long) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = buildTitle(state.sourceModel?.name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        SimilarModelsContent(state, onModelClick, viewModel::retry, Modifier.padding(padding))
    }
}

private fun buildTitle(modelName: String?): String =
    if (modelName != null) "Similar to $modelName" else "Similar Models"

@Composable
private fun SimilarModelsContent(
    state: SimilarModelsUiState,
    onModelClick: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorMessage = state.error
    when {
        state.isLoading -> LoadingStateOverlay(modifier = modifier)
        errorMessage != null -> ErrorStateView(
            message = errorMessage,
            onRetry = onRetry,
            modifier = modifier,
        )
        state.similarModels.isEmpty() -> EmptyStateMessage(
            icon = Icons.Outlined.SearchOff,
            title = "No similar models found",
            subtitle = "Try a different model to find related content.",
            modifier = modifier,
        )
        else -> SimilarModelsGrid(state, onModelClick, modifier)
    }
}

@Composable
private fun SimilarModelsGrid(
    state: SimilarModelsUiState,
    onModelClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        contentPadding = PaddingValues(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = modifier,
    ) {
        items(state.similarModels, key = { it.id }) { model ->
            ModelCard(
                model = model,
                onClick = { onModelClick(model.id) },
            )
        }
    }
}

private const val GRID_COLUMNS = 2
