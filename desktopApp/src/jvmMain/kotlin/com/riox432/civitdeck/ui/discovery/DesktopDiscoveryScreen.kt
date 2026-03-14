package com.riox432.civitdeck.ui.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.feature.settings.presentation.ContentFilterSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.ui.search.DesktopModelCard
import com.riox432.civitdeck.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DesktopDiscoveryScreen(
    viewModel: DesktopDiscoveryViewModel,
    onModelClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val displayViewModel: DisplaySettingsViewModel = koinViewModel()
    val displayState by displayViewModel.uiState.collectAsState()
    val contentFilterVm: ContentFilterSettingsViewModel = koinViewModel()
    val contentFilterState by contentFilterVm.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        DiscoveryTopBar(onRefresh = viewModel::refresh)

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = viewModel::refresh) {
                            Text("Retry")
                        }
                    }
                }
            }
            uiState.sections.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No recommendations available yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                val columns = displayState.gridColumns
                val nsfwFilterLevel = contentFilterState.nsfwFilterLevel
                val nsfwBlurSettings = contentFilterState.nsfwBlurSettings

                LazyVerticalGrid(
                    columns = if (columns > 0) {
                        GridCells.Fixed(columns)
                    } else {
                        GridCells.Adaptive(minSize = CARD_MIN_WIDTH)
                    },
                    contentPadding = PaddingValues(Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    uiState.sections.forEach { section ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader(
                                title = section.title,
                                subtitle = section.reason,
                            )
                        }

                        val models = if (nsfwFilterLevel == NsfwFilterLevel.All) {
                            section.models.filter { !it.nsfw }
                        } else {
                            section.models
                        }

                        items(
                            items = models,
                            key = { model -> "${section.title}_${model.id}" },
                        ) { model ->
                            DesktopModelCard(
                                model = model,
                                onClick = { onModelClick(model.id) },
                                nsfwBlurSettings = nsfwBlurSettings,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveryTopBar(onRefresh: () -> Unit) {
    Surface(tonalElevation = 1.dp) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
        ) {
            Text(
                text = "Discover",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterStart)
                    .padding(start = Spacing.sm),
            )
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier.padding(
            top = Spacing.md,
            bottom = Spacing.xs,
        ),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val CARD_MIN_WIDTH = 256.dp
