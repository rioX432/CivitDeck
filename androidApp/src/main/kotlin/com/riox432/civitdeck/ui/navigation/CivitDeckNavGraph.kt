package com.riox432.civitdeck.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.riox432.civitdeck.ui.collections.CollectionDetailScreen
import com.riox432.civitdeck.ui.collections.CollectionDetailViewModel
import com.riox432.civitdeck.ui.collections.CollectionsScreen
import com.riox432.civitdeck.ui.collections.CollectionsViewModel
import com.riox432.civitdeck.ui.compare.ModelCompareScreen
import com.riox432.civitdeck.ui.creator.CreatorProfileScreen
import com.riox432.civitdeck.ui.creator.CreatorProfileViewModel
import com.riox432.civitdeck.ui.detail.ModelDetailScreen
import com.riox432.civitdeck.ui.detail.ModelDetailViewModel
import com.riox432.civitdeck.ui.gallery.ImageGalleryScreen
import com.riox432.civitdeck.ui.gallery.ImageGalleryViewModel
import com.riox432.civitdeck.ui.prompts.SavedPromptsScreen
import com.riox432.civitdeck.ui.prompts.SavedPromptsViewModel
import com.riox432.civitdeck.ui.search.ModelSearchScreen
import com.riox432.civitdeck.ui.search.ModelSearchViewModel
import com.riox432.civitdeck.ui.settings.LicensesScreen
import com.riox432.civitdeck.ui.settings.SettingsScreen
import com.riox432.civitdeck.ui.settings.SettingsViewModel
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Easing
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data object SearchRoute

data object CollectionsRoute

data class CollectionDetailRoute(val collectionId: Long, val collectionName: String)

data class DetailRoute(
    val modelId: Long,
    val thumbnailUrl: String? = null,
    val sharedElementSuffix: String = "",
)

data class ImageGalleryRoute(val modelVersionId: Long)

data class CreatorRoute(val username: String)

data object SavedPromptsRoute

data object SettingsRoute

data object LicensesRoute

data class CompareRoute(val leftModelId: Long, val rightModelId: Long)

private enum class Tab(
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
) {
    Search("Search", Icons.Filled.Explore, Icons.Outlined.Explore),
    Collections("Collections", Icons.Filled.FolderCopy, Icons.Outlined.FolderCopy),
    Prompts("Prompts", Icons.Outlined.Bookmarks, Icons.Outlined.BookmarkBorder),
    Settings("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}

private class TabState(
    val backStack: MutableList<Any>,
    scrollTrigger: Int = 0,
) {
    var scrollTrigger by mutableIntStateOf(scrollTrigger)

    fun onReselected() {
        if (backStack.size > 1) {
            while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        } else {
            scrollTrigger++
        }
    }
}

@Suppress("LongMethod")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CivitDeckNavGraph() {
    var selectedTab by rememberSaveable(
        stateSaver = mapSaver(
            save = { mapOf("tab" to it.name) },
            restore = { Tab.valueOf(it["tab"] as String) },
        ),
    ) { mutableStateOf(Tab.Search) }

    val tabs = remember {
        mapOf(
            Tab.Search to TabState(mutableStateListOf<Any>(SearchRoute)),
            Tab.Collections to TabState(mutableStateListOf<Any>(CollectionsRoute)),
            Tab.Prompts to TabState(mutableStateListOf<Any>(SavedPromptsRoute)),
            Tab.Settings to TabState(mutableStateListOf<Any>(SettingsRoute)),
        )
    }

    val activeTab = tabs.getValue(selectedTab)

    var compareModelId by rememberSaveable { mutableStateOf<Long?>(null) }
    var compareModelName by rememberSaveable { mutableStateOf<String?>(null) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            Tab.entries.forEach { tab ->
                val selected = tab == selectedTab
                item(
                    selected = selected,
                    onClick = {
                        if (tab == selectedTab) {
                            tabs.getValue(tab).onReselected()
                        } else {
                            selectedTab = tab
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (selected) tab.activeIcon else tab.inactiveIcon,
                            contentDescription = tab.label,
                        )
                    },
                    label = { Text(tab.label) },
                )
            }
        },
    ) {
        Scaffold { padding ->
            SharedTransitionLayout(modifier = Modifier.padding(padding)) {
                CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                    CivitDeckNavDisplay(
                        backStack = activeTab.backStack,
                        searchScrollTrigger = tabs.getValue(Tab.Search).scrollTrigger,
                        promptsScrollTrigger = tabs.getValue(Tab.Prompts).scrollTrigger,
                        settingsScrollTrigger = tabs.getValue(Tab.Settings).scrollTrigger,
                        compareModelId = compareModelId,
                        compareModelName = compareModelName,
                        onCompareModel = { id, name ->
                            compareModelId = id
                            compareModelName = name
                        },
                        onCancelCompare = {
                            compareModelId = null
                            compareModelName = null
                        },
                    )
                }
            }
        }
    }
}

private fun slideTransition(enterOffset: (Int) -> Int, exitOffset: (Int) -> Int) =
    ContentTransform(
        slideInHorizontally(tween(Duration.normal, easing = Easing.standard), enterOffset) +
            fadeIn(tween(Duration.normal, easing = Easing.standard)),
        slideOutHorizontally(tween(Duration.normal, easing = Easing.standard), exitOffset) +
            fadeOut(tween(Duration.normal, easing = Easing.standard)),
    )

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun CivitDeckNavDisplay(
    backStack: MutableList<Any>,
    searchScrollTrigger: Int = 0,
    promptsScrollTrigger: Int = 0,
    settingsScrollTrigger: Int = 0,
    compareModelId: Long? = null,
    compareModelName: String? = null,
    onCompareModel: (Long, String) -> Unit = { _, _ -> },
    onCancelCompare: () -> Unit = {},
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = { slideTransition(enterOffset = { it / 4 }, exitOffset = { -it / 4 }) },
        popTransitionSpec = { slideTransition(enterOffset = { -it / 4 }, exitOffset = { it / 4 }) },
        entryProvider = entryProvider {
            entry<SearchRoute> {
                val viewModel: ModelSearchViewModel = koinViewModel()
                ModelSearchScreen(
                    viewModel = viewModel,
                    onModelClick = { modelId, thumbnailUrl, suffix ->
                        val cmpId = compareModelId
                        if (cmpId != null) {
                            backStack.add(CompareRoute(cmpId, modelId))
                            onCancelCompare()
                        } else {
                            backStack.add(DetailRoute(modelId, thumbnailUrl, suffix))
                        }
                    },
                    scrollToTopTrigger = searchScrollTrigger,
                    onCompareModel = onCompareModel,
                    compareModelName = compareModelName,
                    onCancelCompare = onCancelCompare,
                )
            }
            collectionsEntry(backStack)
            collectionDetailEntry(backStack, compareModelId, onCancelCompare)
            detailEntry(backStack)
            creatorEntry(backStack)
            galleryEntry(backStack)
            compareEntry(backStack)
            entry<SavedPromptsRoute> {
                val viewModel: SavedPromptsViewModel = koinViewModel()
                SavedPromptsScreen(
                    viewModel = viewModel,
                    scrollToTopTrigger = promptsScrollTrigger,
                )
            }
            entry<SettingsRoute> {
                val viewModel: SettingsViewModel = koinViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToLicenses = { backStack.add(LicensesRoute) },
                    scrollToTopTrigger = settingsScrollTrigger,
                )
            }
            entry<LicensesRoute> {
                LicensesScreen(onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}

private fun EntryProviderScope<Any>.collectionsEntry(backStack: MutableList<Any>) {
    entry<CollectionsRoute> {
        val viewModel: CollectionsViewModel = koinViewModel()
        val collections by viewModel.collections.collectAsStateWithLifecycle()
        CollectionsScreen(
            collections = collections,
            onCollectionClick = { id, name ->
                backStack.add(CollectionDetailRoute(id, name))
            },
            onCreateCollection = viewModel::createCollection,
            onRenameCollection = viewModel::renameCollection,
            onDeleteCollection = viewModel::deleteCollection,
        )
    }
}

private fun EntryProviderScope<Any>.collectionDetailEntry(
    backStack: MutableList<Any>,
    compareModelId: Long?,
    onCancelCompare: () -> Unit,
) {
    entry<CollectionDetailRoute> { key ->
        val viewModel: CollectionDetailViewModel = koinViewModel(
            key = "collection_${key.collectionId}",
        ) { parametersOf(key.collectionId) }
        val models by viewModel.displayModels.collectAsStateWithLifecycle()
        val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
        val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
        val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
        val selectedIds by viewModel.selectedModelIds.collectAsStateWithLifecycle()
        val collections by viewModel.collections.collectAsStateWithLifecycle()
        CollectionDetailScreen(
            collectionName = key.collectionName,
            models = models,
            sortOrder = sortOrder,
            typeFilter = typeFilter,
            isSelectionMode = isSelectionMode,
            selectedIds = selectedIds,
            collections = collections,
            collectionId = key.collectionId,
            onBack = { backStack.removeLastOrNull() },
            onModelClick = { modelId ->
                val cmpId = compareModelId
                if (cmpId != null) {
                    backStack.add(CompareRoute(cmpId, modelId))
                    onCancelCompare()
                } else {
                    backStack.add(DetailRoute(modelId))
                }
            },
            onSortChange = { viewModel.sortOrder.value = it },
            onTypeFilterChange = { viewModel.typeFilter.value = it },
            onToggleSelection = viewModel::toggleSelection,
            onEnterSelectionMode = viewModel::enterSelectionMode,
            onSelectAll = viewModel::selectAll,
            onClearSelection = viewModel::clearSelection,
            onRemoveSelected = viewModel::removeSelected,
            onMoveSelectedTo = viewModel::moveSelectedTo,
        )
    }
}

private fun EntryProviderScope<Any>.detailEntry(backStack: MutableList<Any>) {
    entry<DetailRoute> { key ->
        val viewModel: ModelDetailViewModel = koinViewModel(
            key = key.modelId.toString(),
        ) { parametersOf(key.modelId) }
        ModelDetailScreen(
            viewModel = viewModel,
            modelId = key.modelId,
            initialThumbnailUrl = key.thumbnailUrl,
            sharedElementSuffix = key.sharedElementSuffix,
            onBack = { backStack.removeLastOrNull() },
            onViewImages = { modelVersionId ->
                backStack.add(ImageGalleryRoute(modelVersionId))
            },
            onCreatorClick = { username ->
                backStack.add(CreatorRoute(username))
            },
        )
    }
}

private fun EntryProviderScope<Any>.creatorEntry(backStack: MutableList<Any>) {
    entry<CreatorRoute> { key ->
        val viewModel: CreatorProfileViewModel = koinViewModel(
            key = "creator_${key.username}",
        ) { parametersOf(key.username) }
        CreatorProfileScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onModelClick = { modelId, thumbnailUrl ->
                backStack.add(DetailRoute(modelId, thumbnailUrl))
            },
        )
    }
}

private fun EntryProviderScope<Any>.galleryEntry(backStack: MutableList<Any>) {
    entry<ImageGalleryRoute> { key ->
        val viewModel: ImageGalleryViewModel = koinViewModel(
            key = "gallery_${key.modelVersionId}",
        ) { parametersOf(key.modelVersionId) }
        ImageGalleryScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
}

private fun EntryProviderScope<Any>.compareEntry(backStack: MutableList<Any>) {
    entry<CompareRoute> { key ->
        val leftVm: ModelDetailViewModel = koinViewModel(
            key = "compare_left_${key.leftModelId}",
        ) { parametersOf(key.leftModelId) }
        val rightVm: ModelDetailViewModel = koinViewModel(
            key = "compare_right_${key.rightModelId}",
        ) { parametersOf(key.rightModelId) }
        ModelCompareScreen(
            leftViewModel = leftVm,
            rightViewModel = rightVm,
            onBack = { backStack.removeLastOrNull() },
        )
    }
}
