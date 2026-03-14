package com.riox432.civitdeck.ui.settings

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.riox432.civitdeck.feature.settings.presentation.AuthSettingsViewModel
import com.riox432.civitdeck.ui.analytics.DesktopAnalyticsViewModel
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun DesktopSettingsScreen(
    authSettingsViewModel: AuthSettingsViewModel,
    analyticsViewModel: DesktopAnalyticsViewModel,
    modifier: Modifier = Modifier,
) {
    val analyticsState by analyticsViewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        SettingsTopBar()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AnalyticsCard(
                totalViews = analyticsState.totalViews,
                totalFavorites = analyticsState.totalFavorites,
                totalSearches = analyticsState.totalSearches,
                isLoading = analyticsState.isLoading,
                onRefresh = analyticsViewModel::refresh,
            )
            TopStatsCard(
                title = "Top Model Types",
                items = analyticsState.topModelTypes.map { "${it.name}: ${it.count}" },
            )
            TopStatsCard(
                title = "Top Creators",
                items = analyticsState.topCreators.map { "${it.name}: ${it.count}" },
            )
        }
    }
}

@Composable
private fun SettingsTopBar() {
    Surface(tonalElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Settings & Analytics",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
    }
}

@Composable
private fun AnalyticsCard(
    totalViews: Int,
    totalFavorites: Int,
    totalSearches: Int,
    isLoading: Boolean,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Analytics", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onRefresh, enabled = !isLoading) {
                    Text("Refresh")
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem(label = "Views", value = totalViews)
                StatItem(label = "Favorites", value = totalFavorites)
                StatItem(label = "Searches", value = totalSearches)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TopStatsCard(title: String, items: List<String>) {
    if (items.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(Spacing.sm))
            items.take(MAX_STATS_ITEMS).forEachIndexed { index, item ->
                Text(
                    text = "${index + 1}. $item",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = Spacing.xs),
                )
            }
        }
    }
}

private const val MAX_STATS_ITEMS = 10
