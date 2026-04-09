package com.riox432.civitdeck.ui.textsearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.presentation.textsearch.TextSearchUiState
import com.riox432.civitdeck.presentation.textsearch.TextSearchViewModel
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.components.ErrorStateView
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.components.ModelCard
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSearchScreen(
    viewModel: TextSearchViewModel,
    onBack: () -> Unit,
    onModelClick: (Long) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        TextSearchContent(
            state = state,
            onQueryChanged = viewModel::onQueryChanged,
            onSearch = viewModel::search,
            onRetry = viewModel::retry,
            onModelClick = onModelClick,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun TextSearchContent(
    state: TextSearchUiState,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onModelClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (state.isModelAvailable) {
            SearchInput(state.query, onQueryChanged, onSearch)
        }
        TextSearchBody(state, onRetry, onModelClick)
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        placeholder = { Text("Describe what you're looking for...") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
    )
}

@Composable
private fun TextSearchBody(
    state: TextSearchUiState,
    onRetry: () -> Unit,
    onModelClick: (Long) -> Unit,
) {
    val errorMessage = state.error
    when {
        !state.isModelAvailable -> UnavailableState()
        state.isLoading -> LoadingStateOverlay()
        errorMessage != null -> ErrorStateView(message = errorMessage, onRetry = onRetry)
        state.hasSearched && state.results.isEmpty() -> EmptyResultsState()
        state.results.isNotEmpty() -> ResultsGrid(state, onModelClick)
        else -> IdleState()
    }
}

@Composable
private fun UnavailableState() {
    EmptyStateMessage(
        icon = Icons.Outlined.AutoAwesome,
        title = "AI Search coming soon",
        subtitle = "Text-to-image search using SigLIP-2 is under development. " +
            "The text encoder and tokenizer are being integrated.",
    )
}

@Composable
private fun EmptyResultsState() {
    EmptyStateMessage(
        icon = Icons.Outlined.SearchOff,
        title = "No matching models found",
        subtitle = "Try a different description or wait for more models to be indexed.",
    )
}

@Composable
private fun IdleState() {
    EmptyStateMessage(
        icon = Icons.Outlined.AutoAwesome,
        title = "Search by description",
        subtitle = "Describe the kind of model you're looking for and AI will find matches.",
    )
}

@Composable
private fun ResultsGrid(
    state: TextSearchUiState,
    onModelClick: (Long) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        contentPadding = PaddingValues(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(state.results, key = { it.id }) { model ->
            ModelCard(
                model = model,
                onClick = { onModelClick(model.id) },
            )
        }
    }
}

private const val GRID_COLUMNS = 2
