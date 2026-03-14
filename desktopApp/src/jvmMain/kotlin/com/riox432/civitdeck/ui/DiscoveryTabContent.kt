package com.riox432.civitdeck

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.feature.creator.presentation.CreatorProfileViewModel
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.ui.DesktopRoute
import com.riox432.civitdeck.ui.creator.DesktopCreatorScreen
import com.riox432.civitdeck.ui.detail.DesktopDetailScreen
import com.riox432.civitdeck.ui.discovery.DesktopDiscoveryScreen
import com.riox432.civitdeck.ui.discovery.DesktopDiscoveryViewModel
import com.riox432.civitdeck.ui.viewer.DesktopImageViewer
import com.riox432.civitdeck.util.removeLastOrNull
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DiscoveryTabContent(
    backstack: SnapshotStateList<DesktopRoute>,
    modifier: Modifier = Modifier,
) {
    val currentRoute = backstack.lastOrNull()

    Box(modifier = modifier.fillMaxSize()) {
        val discoveryVm: DesktopDiscoveryViewModel = koinViewModel()
        DesktopDiscoveryScreen(
            viewModel = discoveryVm,
            onModelClick = { modelId ->
                backstack.add(DesktopRoute.ModelDetail(modelId))
            },
        )

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
            else -> { /* Discovery screen is always shown */ }
        }
    }
}
