package com.riox432.civitdeck.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import com.riox432.civitdeck.presentation.analytics.AnalyticsViewModel
import com.riox432.civitdeck.presentation.analytics.AnalyticsUiState
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.Elevation

@Composable
fun DesktopAnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        AnalyticsTopBar(onBack = onBack, onRefresh = viewModel::refresh)

        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        state.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(Spacing.lg),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SummaryCard(state)
            state.averageViewDurationMs?.let { avgMs ->
                AvgDurationCard(avgMs)
            }
            if (state.topModelTypes.isNotEmpty()) {
                RankingCard(title = "Top Model Types", items = state.topModelTypes)
            }
            if (state.topCreators.isNotEmpty()) {
                RankingCard(title = "Top Creators", items = state.topCreators)
            }
        }
    }
}

@Composable
private fun AnalyticsTopBar(onBack: () -> Unit, onRefresh: () -> Unit) {
    Surface(tonalElevation = Elevation.xs) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRefresh) {
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun SummaryCard(state: AnalyticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.xs),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text("Usage Summary", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem(label = "Views", value = state.totalViews)
                StatItem(label = "Favorites", value = state.totalFavorites)
                StatItem(label = "Searches", value = state.totalSearches)
            }
        }
    }
}

@Composable
private fun AvgDurationCard(avgMs: Long) {
    val seconds = avgMs / MS_PER_SECOND
    val label = if (seconds >= SECONDS_PER_MINUTE) {
        "${seconds / SECONDS_PER_MINUTE}m ${seconds % SECONDS_PER_MINUTE}s"
    } else {
        "${seconds}s"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.xs),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text("Avg. View Duration", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value.toString(), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RankingCard(
    title: String,
    items: List<com.riox432.civitdeck.domain.model.CategoryStat>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.xs),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(Spacing.md))
            items.take(MAX_RANKING_ITEMS).forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${index + 1}. ${item.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = item.count.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private const val MAX_RANKING_ITEMS = 10
private const val MS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
