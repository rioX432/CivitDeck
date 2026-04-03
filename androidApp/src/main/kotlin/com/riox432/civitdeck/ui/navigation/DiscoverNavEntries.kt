package com.riox432.civitdeck.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import com.riox432.civitdeck.ui.discovery.SwipeDiscoveryScreen
import com.riox432.civitdeck.ui.discovery.SwipeDiscoveryViewModel
import com.riox432.civitdeck.ui.gallery.ImageGalleryScreen
import com.riox432.civitdeck.ui.gallery.ImageGalleryViewModel
import com.riox432.civitdeck.ui.qrcode.QRScannerScreen
import com.riox432.civitdeck.ui.share.ShareViewModel
import com.riox432.civitdeck.ui.similar.SimilarModelsScreen
import com.riox432.civitdeck.ui.similar.SimilarModelsViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

internal fun EntryProviderScope<Any>.discoveryEntry(backStack: MutableList<Any>) {
    entry<DiscoveryRoute> {
        val viewModel: SwipeDiscoveryViewModel = koinViewModel()
        SwipeDiscoveryScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onModelDetail = { modelId ->
                backStack.add(DetailRoute(modelId))
            },
        )
    }
}

internal fun EntryProviderScope<Any>.browseImagesEntry(backStack: MutableList<Any>) {
    entry<BrowseImagesRoute> {
        val viewModel: ImageGalleryViewModel = koinViewModel(key = "browse_images") { parametersOf(0L) }
        val shareVm: ShareViewModel = koinViewModel()
        val shareHashtags by shareVm.hashtags.collectAsStateWithLifecycle()
        ImageGalleryScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            shareHashtags = shareHashtags,
            onToggleShareHashtag = shareVm::onToggle,
            onAddShareHashtag = shareVm::onAdd,
            onRemoveShareHashtag = shareVm::onRemove,
        )
    }
}

internal fun EntryProviderScope<Any>.qrScannerEntry(backStack: MutableList<Any>) {
    entry<QRScannerRoute> {
        QRScannerScreen(
            onBack = { backStack.removeLastOrNull() },
            onModelScanned = { modelId ->
                backStack.removeLastOrNull()
                backStack.add(DetailRoute(modelId))
            },
        )
    }
}

internal fun EntryProviderScope<Any>.similarModelsEntry(backStack: MutableList<Any>) {
    entry<SimilarModelsRoute> { key ->
        val viewModel: SimilarModelsViewModel = koinViewModel(
            key = "similar_${key.modelId}",
        ) { parametersOf(key.modelId) }
        SimilarModelsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onModelClick = { modelId -> backStack.add(DetailRoute(modelId)) },
        )
    }
}
