package com.omooooori.civitdeck.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.omooooori.civitdeck.ui.detail.ModelDetailScreen
import com.omooooori.civitdeck.ui.detail.ModelDetailViewModel
import com.omooooori.civitdeck.ui.gallery.ImageGalleryScreen
import com.omooooori.civitdeck.ui.gallery.ImageGalleryViewModel
import com.omooooori.civitdeck.ui.search.ModelSearchScreen
import com.omooooori.civitdeck.ui.search.ModelSearchViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data object SearchRoute

data class DetailRoute(val modelId: Long)

data class ImageGalleryRoute(val modelId: Long)

@Composable
fun CivitDeckNavGraph() {
    val backStack = remember { mutableStateListOf<Any>(SearchRoute) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<SearchRoute> {
                val viewModel: ModelSearchViewModel = koinViewModel()
                ModelSearchScreen(
                    viewModel = viewModel,
                    onModelClick = { modelId -> backStack.add(DetailRoute(modelId)) },
                )
            }
            entry<DetailRoute> { key ->
                val viewModel: ModelDetailViewModel = koinViewModel(
                    key = key.modelId.toString(),
                ) { parametersOf(key.modelId) }
                ModelDetailScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() },
                    onViewImages = { backStack.add(ImageGalleryRoute(key.modelId)) },
                )
            }
            entry<ImageGalleryRoute> { key ->
                val viewModel: ImageGalleryViewModel = koinViewModel(
                    key = "gallery_${key.modelId}",
                ) { parametersOf(key.modelId) }
                ImageGalleryScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
