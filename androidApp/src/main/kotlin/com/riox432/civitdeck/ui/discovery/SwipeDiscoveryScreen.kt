package com.riox432.civitdeck.ui.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.theme.IconSize
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeDiscoveryScreen(
    viewModel: SwipeDiscoveryViewModel,
    onBack: () -> Unit,
    onModelDetail: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discover") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                DiscoveryContent(
                    state = state,
                    viewModel = viewModel,
                    onModelDetail = onModelDetail,
                )
            }

            ActionButtons(
                canUndo = state.lastDismissed != null,
                hasCards = state.cards.isNotEmpty(),
                onSkip = {
                    state.cards.firstOrNull()?.let {
                        viewModel.onSwipeLeft(it)
                    }
                },
                onFavorite = {
                    state.cards.firstOrNull()?.let {
                        viewModel.onSwipeRight(it)
                    }
                },
                onUndo = viewModel::undoLastSwipe,
            )

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun DiscoveryContent(
    state: SwipeDiscoveryState,
    viewModel: SwipeDiscoveryViewModel,
    onModelDetail: (Long) -> Unit,
) {
    when {
        state.isLoading && state.cards.isEmpty() -> {
            LoadingStateOverlay()
        }
        state.error != null && state.cards.isEmpty() -> {
            Text(
                text = state.error ?: "Unknown error",
                color = MaterialTheme.colorScheme.error,
            )
        }
        state.cards.isEmpty() -> {
            Text(
                text = "No more models to discover",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        else -> {
            CardStack(
                cards = state.cards,
                onSwiped = { model, direction ->
                    when (direction) {
                        SwipeDirection.Right -> viewModel.onSwipeRight(model)
                        SwipeDirection.Left -> viewModel.onSwipeLeft(model)
                        SwipeDirection.Up -> {
                            val id = viewModel.onSwipeUp(model)
                            onModelDetail(id)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ActionButtons(
    canUndo: Boolean,
    hasCards: Boolean,
    onSkip: () -> Unit,
    onFavorite: () -> Unit,
    onUndo: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = onUndo,
            enabled = canUndo,
            modifier = Modifier.size(IconSize.large),
        ) {
            Icon(Icons.Filled.Undo, contentDescription = stringResource(R.string.cd_undo))
        }

        FilledTonalIconButton(
            onClick = onSkip,
            enabled = hasCards,
            modifier = Modifier.size(IconSize.xlarge),
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.cd_skip),
                tint = MaterialTheme.colorScheme.error,
            )
        }

        FilledTonalIconButton(
            onClick = onFavorite,
            enabled = hasCards,
            modifier = Modifier.size(IconSize.xlarge),
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = stringResource(R.string.cd_favorite),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
