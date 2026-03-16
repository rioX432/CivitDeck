package com.riox432.civitdeck

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.feature.collections.presentation.CollectionDetailViewModel
import com.riox432.civitdeck.feature.collections.presentation.CollectionsViewModel
import com.riox432.civitdeck.feature.creator.presentation.CreatorProfileViewModel
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.ui.DesktopRoute
import com.riox432.civitdeck.ui.collections.DesktopCollectionDetailScreen
import com.riox432.civitdeck.ui.collections.DesktopCollectionsScreen
import com.riox432.civitdeck.ui.creator.DesktopCreatorScreen
import com.riox432.civitdeck.ui.detail.DesktopDetailScreen
import com.riox432.civitdeck.ui.viewer.DesktopImageViewer
import com.riox432.civitdeck.util.removeLastOrNull
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CollectionsTabContent(
    backstack: SnapshotStateList<DesktopRoute>,
    modifier: Modifier = Modifier,
) {
    val currentRoute = backstack.lastOrNull()

    Box(modifier = modifier.fillMaxSize()) {
        val collectionsVm: CollectionsViewModel = koinViewModel()
        DesktopCollectionsScreen(
            viewModel = collectionsVm,
            onCollectionClick = { id, name ->
                backstack.add(DesktopRoute.CollectionDetail(id, name))
            },
        )

        when (currentRoute) {
            is DesktopRoute.CollectionDetail -> {
                val detailVm: CollectionDetailViewModel = koinViewModel(
                    key = "collection_${currentRoute.collectionId}",
                ) { parametersOf(currentRoute.collectionId) }
                DisposableEffect(detailVm) {
                    onDispose { detailVm.onCleared() }
                }
                DesktopCollectionDetailScreen(
                    viewModel = detailVm,
                    collectionName = currentRoute.collectionName,
                    onBack = { backstack.removeLastOrNull() },
                    onModelClick = { modelId ->
                        backstack.add(DesktopRoute.ModelDetail(modelId))
                    },
                )
            }
            is DesktopRoute.ModelDetail -> {
                val modelVm: ModelDetailViewModel = koinViewModel(
                    key = "detail_${currentRoute.modelId}",
                ) { parametersOf(currentRoute.modelId) }
                DisposableEffect(modelVm) {
                    onDispose { modelVm.onCleared() }
                }
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
                DisposableEffect(creatorVm) {
                    onDispose { creatorVm.onCleared() }
                }
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
            else -> { /* Collections list is always shown */ }
        }
    }
}
