@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.CategoryStat
import com.riox432.civitdeck.domain.model.DailyViewCount
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usage Stats") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            AnalyticsContent(state, Modifier.padding(padding))
        }
    }
}

@Composable
private fun AnalyticsContent(state: AnalyticsUiState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item { Spacer(Modifier.height(Spacing.sm)) }
        item { SummaryCards(state.totalViews, state.totalFavorites, state.totalSearches) }
        if (state.dailyViewCounts.isNotEmpty()) {
            item { SectionTitle("Views (Last 30 Days)") }
            item { ViewTrendChart(state.dailyViewCounts) }
        }
        if (state.topModelTypes.isNotEmpty()) {
            item { SectionTitle("Top Model Types") }
            item { HorizontalBarChart(state.topModelTypes) }
        }
        if (state.topCreators.isNotEmpty()) {
            item { SectionTitle("Most Viewed Creators") }
            items(state.topCreators.take(TOP_N)) { stat ->
                RankedRow(stat)
            }
        }
        if (state.topSearchQueries.isNotEmpty()) {
            item { SectionTitle("Top Searches") }
            items(state.topSearchQueries.take(TOP_N)) { stat ->
                RankedRow(stat)
            }
        }
        item { Spacer(Modifier.height(Spacing.lg)) }
    }
}

@Composable
private fun SummaryCards(views: Int, favorites: Int, searches: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        StatCard("Views", views.toString(), Modifier.weight(1f))
        StatCard("Favorites", favorites.toString(), Modifier.weight(1f))
        StatCard("Searches", searches.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = Spacing.sm),
    )
}

@Composable
private fun ViewTrendChart(data: List<DailyViewCount>) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    Card(colors = CardDefaults.cardColors(containerColor = surfaceVariant)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT).padding(Spacing.md)) {
            drawViewTrend(data, primary)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawViewTrend(
    data: List<DailyViewCount>,
    color: Color,
) {
    if (data.size < 2) return
    val maxCount = data.maxOf { it.count }.coerceAtLeast(1).toFloat()
    val stepX = size.width / (data.size - 1).coerceAtLeast(1)
    val path = Path()
    data.forEachIndexed { i, point ->
        val x = i * stepX
        val y = size.height - (point.count / maxCount) * size.height
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        drawCircle(color, radius = DOT_RADIUS, center = Offset(x, y))
    }
    drawPath(path, color, style = Stroke(width = LINE_WIDTH))
}

@Composable
private fun HorizontalBarChart(data: List<CategoryStat>) {
    val maxCount = data.maxOfOrNull { it.count } ?: 1
    val primary = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        data.take(TOP_N).forEach { stat ->
            BarRow(stat, maxCount, primary)
        }
    }
}

@Composable
private fun BarRow(stat: CategoryStat, maxCount: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stat.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(BAR_LABEL_WIDTH),
            maxLines = 1,
        )
        Canvas(modifier = Modifier.weight(1f).height(BAR_HEIGHT)) {
            val barWidth = (stat.count.toFloat() / maxCount) * size.width
            drawRoundRect(color, size = size.copy(width = barWidth))
        }
        Spacer(Modifier.width(Spacing.xs))
        Text(stat.count.toString(), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RankedRow(stat: CategoryStat) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stat.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            stat.count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val CHART_HEIGHT = 160.dp
private val BAR_HEIGHT = 20.dp
private val BAR_LABEL_WIDTH = 100.dp
private const val DOT_RADIUS = 4f
private const val LINE_WIDTH = 2f
private const val TOP_N = 5
