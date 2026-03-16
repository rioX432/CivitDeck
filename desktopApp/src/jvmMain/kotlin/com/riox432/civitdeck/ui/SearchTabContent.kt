package com.riox432.civitdeck

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.riox432.civitdeck.feature.creator.presentation.CreatorProfileViewModel
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.ui.DesktopRoute
import com.riox432.civitdeck.ui.compare.DesktopCompareScreen
import com.riox432.civitdeck.ui.creator.DesktopCreatorScreen
import com.riox432.civitdeck.ui.detail.DesktopDetailScreen
import com.riox432.civitdeck.ui.qrcode.DesktopQRCodeScreen
import com.riox432.civitdeck.ui.search.DesktopSearchScreen
import com.riox432.civitdeck.ui.search.DesktopSearchViewModel
import com.riox432.civitdeck.ui.search.DesktopUrlImportDialog
import com.riox432.civitdeck.ui.viewer.DesktopImageViewer
import com.riox432.civitdeck.util.removeLastOrNull
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SearchTabContent(
    backstack: SnapshotStateList<DesktopRoute>,
    searchFocusRequester: FocusRequester = FocusRequester(),
    modifier: Modifier = Modifier,
) {
    val currentRoute = backstack.lastOrNull()
    var showUrlImport by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Always keep search screen alive underneath
        val searchViewModel: DesktopSearchViewModel = koinViewModel()
        DesktopSearchScreen(
            viewModel = searchViewModel,
            onModelClick = { modelId ->
                backstack.add(DesktopRoute.ModelDetail(modelId))
            },
            onCreatorClick = { username ->
                backstack.add(DesktopRoute.CreatorProfile(username))
            },
            onUrlImportClick = { showUrlImport = true },
            onQRCodeClick = { backstack.add(DesktopRoute.QRCode) },
            searchFocusRequester = searchFocusRequester,
        )

        // Overlay detail/viewer/creator/compare on top
        when (currentRoute) {
            is DesktopRoute.ModelDetail -> {
                val detailVm: ModelDetailViewModel = koinViewModel(
                    key = "detail_${currentRoute.modelId}",
                ) { parametersOf(currentRoute.modelId) }
                DisposableEffect(detailVm) {
                    onDispose { detailVm.onCleared() }
                }
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
                DisposableEffect(leftVm, rightVm) {
                    onDispose {
                        leftVm.onCleared()
                        rightVm.onCleared()
                    }
                }
                DesktopCompareScreen(
                    leftViewModel = leftVm,
                    rightViewModel = rightVm,
                    onBack = { backstack.removeLastOrNull() },
                )
            }
            else -> { /* Search screen is always shown */ }
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
