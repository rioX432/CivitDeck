package com.riox432.civitdeck

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.riox432.civitdeck.feature.creator.presentation.CreatorProfileViewModel
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.ui.DesktopRoute
import com.riox432.civitdeck.ui.compare.DesktopCompareScreen
import com.riox432.civitdeck.ui.creator.DesktopCreatorScreen
import com.riox432.civitdeck.ui.detail.DesktopDetailScreen
import com.riox432.civitdeck.ui.discovery.DesktopDiscoveryScreen
import com.riox432.civitdeck.ui.discovery.DesktopDiscoveryViewModel
import com.riox432.civitdeck.presentation.feed.FeedViewModel
import com.riox432.civitdeck.ui.feed.DesktopFeedScreen
import com.riox432.civitdeck.ui.qrcode.DesktopQRCodeScreen
import com.riox432.civitdeck.ui.search.DesktopSearchScreen
import com.riox432.civitdeck.feature.search.presentation.ModelSearchViewModel
import com.riox432.civitdeck.ui.search.DesktopUrlImportDialog
import com.riox432.civitdeck.ui.theme.Elevation
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.viewer.DesktopImageViewer
import com.riox432.civitdeck.util.removeLastOrNull
import androidx.compose.foundation.layout.Arrangement
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private enum class DiscoverSection(val label: String) {
    Search("Search"),
    Trending("Trending"),
    Feed("Feed"),
}

@Composable
fun DiscoverTabContent(
    backstack: SnapshotStateList<DesktopRoute>,
    searchFocusRequester: FocusRequester = FocusRequester(),
    modifier: Modifier = Modifier,
) {
    var selectedSection by remember { mutableStateOf(DiscoverSection.Search) }
    var showUrlImport by remember { mutableStateOf(false) }
    val currentRoute = backstack.lastOrNull()

    Column(modifier = modifier.fillMaxSize()) {
        DiscoverSectionTabs(
            selected = selectedSection,
            onSelected = { selectedSection = it },
        )

        Box(modifier = Modifier.fillMaxSize()) {
            DiscoverBaseContent(
                section = selectedSection,
                backstack = backstack,
                searchFocusRequester = searchFocusRequester,
                onShowUrlImport = { showUrlImport = true },
            )

            // Overlay routes on top
            DiscoverOverlayContent(backstack, currentRoute)
        }
    }

    if (showUrlImport) {
        DesktopUrlImportDialog(
            onModelFound = { modelId ->
                showUrlImport = false
                backstack.add(DesktopRoute.ModelDetail(modelId))
            },
            onDismiss = { showUrlImport = false },
        )
    }
}

@Composable
private fun DiscoverSectionTabs(
    selected: DiscoverSection,
    onSelected: (DiscoverSection) -> Unit,
) {
    Surface(tonalElevation = Elevation.xs) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Discover",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = Spacing.md),
            )
            DiscoverSection.entries.forEach { section ->
                FilterChip(
                    selected = selected == section,
                    onClick = { onSelected(section) },
                    label = { Text(section.label) },
                )
            }
        }
    }
}

@Composable
private fun DiscoverBaseContent(
    section: DiscoverSection,
    backstack: SnapshotStateList<DesktopRoute>,
    searchFocusRequester: FocusRequester,
    onShowUrlImport: () -> Unit,
) {
    when (section) {
        DiscoverSection.Search -> {
            val searchVm: ModelSearchViewModel = koinViewModel()
            DesktopSearchScreen(
                viewModel = searchVm,
                onModelClick = { backstack.add(DesktopRoute.ModelDetail(it)) },
                onCreatorClick = { backstack.add(DesktopRoute.CreatorProfile(it)) },
                onUrlImportClick = onShowUrlImport,
                onQRCodeClick = { backstack.add(DesktopRoute.QRCode) },
                searchFocusRequester = searchFocusRequester,
            )
        }
        DiscoverSection.Trending -> {
            val discoveryVm: DesktopDiscoveryViewModel = koinViewModel()
            DesktopDiscoveryScreen(
                viewModel = discoveryVm,
                onModelClick = { backstack.add(DesktopRoute.ModelDetail(it)) },
            )
        }
        DiscoverSection.Feed -> {
            val feedVm: FeedViewModel = koinViewModel()
            DesktopFeedScreen(
                viewModel = feedVm,
                onModelClick = { backstack.add(DesktopRoute.ModelDetail(it)) },
                onCreatorClick = { backstack.add(DesktopRoute.CreatorProfile(it)) },
            )
        }
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun DiscoverOverlayContent(
    backstack: SnapshotStateList<DesktopRoute>,
    currentRoute: DesktopRoute?,
) {
    when (currentRoute) {
        is DesktopRoute.ModelDetail -> {
            val detailVm: ModelDetailViewModel = koinViewModel(
                key = "detail_${currentRoute.modelId}",
            ) { parametersOf(currentRoute.modelId) }
            DesktopDetailScreen(
                viewModel = detailVm,
                onBack = { backstack.removeLastOrNull() },
                onImageClick = { urls, index ->
                    backstack.add(DesktopRoute.ImageViewer(urls, index))
                },
                onCreatorClick = { username ->
                    backstack.add(DesktopRoute.CreatorProfile(username))
                },
            )
        }
        is DesktopRoute.ImageViewer -> {
            DesktopImageViewer(
                imageUrls = currentRoute.imageUrls,
                initialIndex = currentRoute.initialIndex,
                onClose = { backstack.removeLastOrNull() },
            )
        }
        is DesktopRoute.CreatorProfile -> {
            val creatorVm: CreatorProfileViewModel = koinViewModel(
                key = "creator_${currentRoute.username}",
            ) { parametersOf(currentRoute.username) }
            DesktopCreatorScreen(
                viewModel = creatorVm,
                onBack = { backstack.removeLastOrNull() },
                onModelClick = { modelId ->
                    backstack.add(DesktopRoute.ModelDetail(modelId))
                },
            )
        }
        is DesktopRoute.QRCode -> {
            DesktopQRCodeScreen(
                onBack = { backstack.removeLastOrNull() },
                onModelIdFound = { modelId ->
                    backstack.removeLastOrNull()
                    backstack.add(DesktopRoute.ModelDetail(modelId))
                },
            )
        }
        is DesktopRoute.ModelCompare -> {
            val leftVm: ModelDetailViewModel = koinViewModel(
                key = "compare_left_${currentRoute.leftModelId}",
            ) { parametersOf(currentRoute.leftModelId) }
            val rightVm: ModelDetailViewModel = koinViewModel(
                key = "compare_right_${currentRoute.rightModelId}",
            ) { parametersOf(currentRoute.rightModelId) }
            DesktopCompareScreen(
                leftViewModel = leftVm,
                rightViewModel = rightVm,
                onBack = { backstack.removeLastOrNull() },
            )
        }
        else -> { /* Base content is shown */ }
    }
}
