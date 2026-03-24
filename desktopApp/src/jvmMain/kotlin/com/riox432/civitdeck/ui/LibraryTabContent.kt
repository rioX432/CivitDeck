package com.riox432.civitdeck

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.feature.collections.presentation.CollectionDetailViewModel
import com.riox432.civitdeck.feature.collections.presentation.CollectionsViewModel
import com.riox432.civitdeck.feature.creator.presentation.CreatorProfileViewModel
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.feature.prompts.presentation.SavedPromptsViewModel
import com.riox432.civitdeck.ui.DesktopRoute
import com.riox432.civitdeck.ui.collections.DesktopCollectionDetailScreen
import com.riox432.civitdeck.ui.collections.DesktopCollectionsScreen
import com.riox432.civitdeck.ui.creator.DesktopCreatorScreen
import com.riox432.civitdeck.ui.dataset.DesktopDatasetDetailScreen
import com.riox432.civitdeck.ui.dataset.DesktopDatasetDetailViewModel
import com.riox432.civitdeck.ui.dataset.DesktopDatasetListScreen
import com.riox432.civitdeck.ui.dataset.DesktopDatasetListViewModel
import com.riox432.civitdeck.ui.detail.DesktopDetailScreen
import com.riox432.civitdeck.ui.prompts.DesktopPromptsScreen
import com.riox432.civitdeck.ui.theme.Elevation
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.viewer.DesktopImageViewer
import com.riox432.civitdeck.util.removeLastOrNull
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private enum class LibrarySection(val label: String) {
    Collections("Collections"),
    Datasets("Datasets"),
    Prompts("Prompts"),
}

@Composable
fun LibraryTabContent(
    backstack: SnapshotStateList<DesktopRoute>,
    modifier: Modifier = Modifier,
) {
    var selectedSection by remember { mutableStateOf(LibrarySection.Collections) }
    val currentRoute = backstack.lastOrNull()

    Column(modifier = modifier.fillMaxSize()) {
        LibrarySectionTabs(
            selected = selectedSection,
            onSelected = {
                selectedSection = it
                backstack.clear()
            },
        )

        Box(modifier = Modifier.fillMaxSize()) {
            LibraryBaseContent(
                section = selectedSection,
                backstack = backstack,
            )

            LibraryOverlayContent(backstack, currentRoute)
        }
    }
}

@Composable
private fun LibrarySectionTabs(
    selected: LibrarySection,
    onSelected: (LibrarySection) -> Unit,
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
                text = "Library",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = Spacing.md),
            )
            LibrarySection.entries.forEach { section ->
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
private fun LibraryBaseContent(
    section: LibrarySection,
    backstack: SnapshotStateList<DesktopRoute>,
) {
    when (section) {
        LibrarySection.Collections -> {
            val vm: CollectionsViewModel = koinViewModel()
            DesktopCollectionsScreen(
                viewModel = vm,
                onCollectionClick = { id, name ->
                    backstack.add(DesktopRoute.CollectionDetail(id, name))
                },
            )
        }
        LibrarySection.Datasets -> {
            val vm: DesktopDatasetListViewModel = koinViewModel()
            DisposableEffect(vm) { onDispose { vm.onCleared() } }
            DesktopDatasetListScreen(
                viewModel = vm,
                onDatasetClick = { id, name ->
                    backstack.add(DesktopRoute.DatasetDetail(id, name))
                },
                onBack = { /* At top level, no back action */ },
            )
        }
        LibrarySection.Prompts -> {
            val vm: SavedPromptsViewModel = koinViewModel()
            DesktopPromptsScreen(viewModel = vm)
        }
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun LibraryOverlayContent(
    backstack: SnapshotStateList<DesktopRoute>,
    currentRoute: DesktopRoute?,
) {
    when (currentRoute) {
        is DesktopRoute.CollectionDetail -> {
            val detailVm: CollectionDetailViewModel = koinViewModel(
                key = "collection_${currentRoute.collectionId}",
            ) { parametersOf(currentRoute.collectionId) }
            DesktopCollectionDetailScreen(
                viewModel = detailVm,
                collectionName = currentRoute.collectionName,
                onBack = { backstack.removeLastOrNull() },
                onModelClick = { modelId ->
                    backstack.add(DesktopRoute.ModelDetail(modelId))
                },
            )
        }
        is DesktopRoute.DatasetDetail -> {
            val vm: DesktopDatasetDetailViewModel = koinViewModel(
                key = "dataset_detail_${currentRoute.datasetId}",
            ) { parametersOf(currentRoute.datasetId) }
            DisposableEffect(vm) { onDispose { vm.onCleared() } }
            DesktopDatasetDetailScreen(
                datasetName = currentRoute.datasetName,
                viewModel = vm,
                onBack = { backstack.removeLastOrNull() },
            )
        }
        is DesktopRoute.ModelDetail -> {
            val modelVm: ModelDetailViewModel = koinViewModel(
                key = "detail_${currentRoute.modelId}",
            ) { parametersOf(currentRoute.modelId) }
            DesktopDetailScreen(
                viewModel = modelVm,
                onBack = { backstack.removeLastOrNull() },
                onImageClick = { urls, index ->
                    backstack.add(DesktopRoute.ImageViewer(urls, index))
                },
                onCreatorClick = { username ->
                    backstack.add(DesktopRoute.CreatorProfile(username))
                },
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
        is DesktopRoute.ImageViewer -> {
            DesktopImageViewer(
                imageUrls = currentRoute.imageUrls,
                initialIndex = currentRoute.initialIndex,
                onClose = { backstack.removeLastOrNull() },
            )
        }
        else -> { /* Base section is shown */ }
    }
}
