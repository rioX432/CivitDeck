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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import com.riox432.civitdeck.domain.model.NavShortcut
import com.riox432.civitdeck.feature.collections.presentation.CollectionDetailViewModel
import com.riox432.civitdeck.feature.collections.presentation.CollectionsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.CivitaiLinkSettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIQueueViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ModelFileBrowserViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.WorkflowTemplateViewModel
import com.riox432.civitdeck.feature.creator.presentation.CreatorProfileViewModel
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.feature.gallery.presentation.ImageGalleryViewModel
import com.riox432.civitdeck.feature.prompts.presentation.SavedPromptsViewModel
import com.riox432.civitdeck.feature.search.presentation.ModelSearchViewModel
import com.riox432.civitdeck.feature.search.presentation.SwipeDiscoveryViewModel
import com.riox432.civitdeck.feature.settings.presentation.SettingsViewModel
import com.riox432.civitdeck.ui.collections.CollectionDetailScreen
import com.riox432.civitdeck.ui.collections.CollectionsScreen
import com.riox432.civitdeck.ui.comfyui.CivitaiLinkSettingsScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUIGenerationScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUIHistoryScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUIOutputDetailScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUIQueueScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUISettingsScreen
import com.riox432.civitdeck.ui.comfyui.SDWebUIGenerationScreen
import com.riox432.civitdeck.ui.comfyui.SDWebUISettingsScreen
import com.riox432.civitdeck.ui.comfyui.WorkflowTemplateEditorScreen
import com.riox432.civitdeck.ui.comfyui.WorkflowTemplateScreen
import com.riox432.civitdeck.ui.compare.ModelCompareScreen
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.creator.CreatorProfileScreen
import com.riox432.civitdeck.ui.dataset.DatasetDetailScreen
import com.riox432.civitdeck.ui.dataset.DatasetDetailViewModel
import com.riox432.civitdeck.ui.dataset.DatasetListScreen
import com.riox432.civitdeck.ui.dataset.DatasetListViewModel
import com.riox432.civitdeck.ui.detail.ModelDetailScreen
import com.riox432.civitdeck.ui.discovery.SwipeDiscoveryScreen
import com.riox432.civitdeck.ui.gallery.ImageGalleryScreen
import com.riox432.civitdeck.ui.modelfiles.ModelFileBrowserScreen
import com.riox432.civitdeck.ui.search.ModelSearchScreen
import com.riox432.civitdeck.ui.settings.AdvancedSettingsScreen
import com.riox432.civitdeck.ui.settings.AppearanceSettingsScreen
import com.riox432.civitdeck.ui.settings.ContentFilterSettingsScreen
import com.riox432.civitdeck.ui.settings.LicensesScreen
import com.riox432.civitdeck.ui.settings.NavShortcutsSettingsScreen
import com.riox432.civitdeck.ui.settings.NotificationsSettingsScreen
import com.riox432.civitdeck.ui.settings.SettingsScreen
import com.riox432.civitdeck.ui.settings.StorageSettingsScreen
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Easing
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.collections.buildList

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

data object ModelFileBrowserRoute

data object DiscoveryRoute

data class CompareRoute(val leftModelId: Long, val rightModelId: Long)

data object ComfyUISettingsRoute

data object ComfyUIGenerationRoute

data object ComfyUIQueueRoute

data class ComfyUIBridgeRoute(
    val modelId: Long,
    val versionId: Long,
    val sha256Hash: String,
    val modelName: String,
    val prompt: String?,
    val negativePrompt: String?,
    val steps: Int?,
    val cfgScale: Double?,
    val seed: Long?,
    val sampler: String?,
)

data object WorkflowTemplateLibraryRoute

data class WorkflowTemplateEditorRoute(val templateId: Long)

data object WorkflowTemplatePickerRoute

data object SDWebUISettingsRoute

data object SDWebUIGenerationRoute

data object AppearanceSettingsRoute

data object ContentFilterSettingsRoute

data object NotificationsSettingsRoute

data object StorageSettingsRoute

data object AdvancedSettingsRoute

data object CivitaiLinkSettingsRoute

data object ComfyUIHistoryRoute

data class ComfyUIOutputDetailRoute(val imageId: String)

data object BrowseImagesRoute

data object NavShortcutsSettingsRoute

data object DatasetListRoute

data class DatasetDetailRoute(val datasetId: Long, val datasetName: String)

internal enum class Tab(
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
) {
    Search("Search", Icons.Filled.Explore, Icons.Outlined.Explore),
    Collections("Saved", Icons.Filled.FolderCopy, Icons.Outlined.FolderCopy),
    Settings("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}

private val NavShortcut.activeIcon: ImageVector
    get() = when (this) {
        NavShortcut.OutputGallery -> Icons.Filled.PhotoLibrary
        NavShortcut.Generate -> Icons.Filled.AutoAwesome
        NavShortcut.ImageGallery -> Icons.Filled.Image
    }

private val NavShortcut.inactiveIcon: ImageVector
    get() = when (this) {
        NavShortcut.OutputGallery -> Icons.Outlined.PhotoLibrary
        NavShortcut.Generate -> Icons.Outlined.AutoAwesome
        NavShortcut.ImageGallery -> Icons.Outlined.Image
    }

private val NavShortcut.navLabel: String
    get() = when (this) {
        NavShortcut.OutputGallery -> "Output"
        NavShortcut.Generate -> "Generate"
        NavShortcut.ImageGallery -> "Images"
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
internal fun CivitDeckNavGraph(initialTab: Tab = Tab.Search) {
    val searchViewModel: ModelSearchViewModel = koinViewModel()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    var selectedTabId by rememberSaveable { mutableStateOf(initialTab.name) }

    val fixedTabStates = remember {
        mapOf(
            Tab.Search.name to TabState(mutableStateListOf<Any>(SearchRoute)),
            Tab.Collections.name to TabState(mutableStateListOf<Any>(CollectionsRoute)),
            Tab.Settings.name to TabState(mutableStateListOf<Any>(SettingsRoute)),
        )
    }

    val shortcutTabStates = remember {
        mapOf(
            NavShortcut.OutputGallery.name to TabState(mutableStateListOf<Any>(ComfyUIHistoryRoute)),
            NavShortcut.Generate.name to TabState(mutableStateListOf<Any>(ComfyUIGenerationRoute)),
            NavShortcut.ImageGallery.name to TabState(mutableStateListOf<Any>(BrowseImagesRoute)),
        )
    }

    val activeShortcuts = if (settingsState.powerUserMode) settingsState.customNavShortcuts else emptyList()

    val navItems = buildList {
        add(Tab.Search)
        add(Tab.Collections)
        activeShortcuts.forEach { shortcut ->
            add(shortcut)
        }
        add(Tab.Settings)
    }

    val validTabIds = navItems.mapNotNull { navItemInfoFor(it)?.id }.toSet()
    if (selectedTabId !in validTabIds) selectedTabId = Tab.Search.name

    val activeBackStack = fixedTabStates[selectedTabId]?.backStack
        ?: shortcutTabStates[selectedTabId]?.backStack
        ?: fixedTabStates.getValue(Tab.Search.name).backStack

    var compareModelId by rememberSaveable { mutableStateOf<Long?>(null) }
    var compareModelName by rememberSaveable { mutableStateOf<String?>(null) }

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val navLayoutType = if (
        adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
    ) {
        NavigationSuiteType.NavigationDrawer
    } else {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
    }

    NavigationSuiteScaffold(
        layoutType = navLayoutType,
        navigationSuiteItems = {
            navItems.forEach { navItem ->
                val info = navItemInfoFor(navItem) ?: return@forEach
                val selected = info.id == selectedTabId
                item(
                    selected = selected,
                    onClick = {
                        if (info.id == selectedTabId) {
                            (fixedTabStates[info.id] ?: shortcutTabStates[info.id])?.onReselected()
                        } else {
                            selectedTabId = info.id
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (selected) info.activeIcon else info.inactiveIcon,
                            contentDescription = info.label,
                        )
                    },
                    label = { Text(info.label) },
                )
            }
        },
    ) {
        Scaffold { padding ->
            SharedTransitionLayout(modifier = Modifier.padding(padding)) {
                CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                    CivitDeckNavDisplay(
                        backStack = activeBackStack,
                        searchViewModel = searchViewModel,
                        searchScrollTrigger = fixedTabStates.getValue(Tab.Search.name).scrollTrigger,
                        settingsScrollTrigger = fixedTabStates.getValue(Tab.Settings.name).scrollTrigger,
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

private data class NavItemInfo(
    val id: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
)

private fun navItemInfoFor(navItem: Any): NavItemInfo? = when (navItem) {
    is Tab -> NavItemInfo(navItem.name, navItem.label, navItem.activeIcon, navItem.inactiveIcon)
    is NavShortcut -> NavItemInfo(navItem.name, navItem.navLabel, navItem.activeIcon, navItem.inactiveIcon)
    else -> null
}

private fun slideTransition(enterOffset: (Int) -> Int, exitOffset: (Int) -> Int) =
    ContentTransform(
        slideInHorizontally(tween(Duration.normal, easing = Easing.standard), enterOffset) +
            fadeIn(tween(Duration.normal, easing = Easing.standard)),
        slideOutHorizontally(tween(Duration.normal, easing = Easing.standard), exitOffset) +
            fadeOut(tween(Duration.normal, easing = Easing.standard)),
    )

@Suppress("LongParameterList", "LongMethod", "UnusedParameter")
@Composable
private fun CivitDeckNavDisplay(
    backStack: MutableList<Any>,
    searchViewModel: ModelSearchViewModel,
    searchScrollTrigger: Int = 0,
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
                ModelSearchScreen(
                    viewModel = searchViewModel,
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
                    compareModelName = compareModelName,
                    onCancelCompare = onCancelCompare,
                    onDiscoverClick = { backStack.add(DiscoveryRoute) },
                    onCompareModel = onCompareModel,
                )
            }
            collectionsEntry(backStack)
            collectionDetailEntry(backStack, compareModelId, onCancelCompare)
            datasetListEntry(backStack)
            datasetDetailEntry(backStack)
            detailEntry(backStack)
            creatorEntry(backStack)
            galleryEntry(backStack)
            compareEntry(backStack)
            discoveryEntry(backStack)
            browseImagesEntry(backStack)
            entry<SettingsRoute> {
                val viewModel: SettingsViewModel = koinViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToAppearance = { backStack.add(AppearanceSettingsRoute) },
                    onNavigateToContentFilter = { backStack.add(ContentFilterSettingsRoute) },
                    onNavigateToNotifications = { backStack.add(NotificationsSettingsRoute) },
                    onNavigateToStorage = { backStack.add(StorageSettingsRoute) },
                    onNavigateToAdvanced = { backStack.add(AdvancedSettingsRoute) },
                    onNavigateToNavShortcuts = { backStack.add(NavShortcutsSettingsRoute) },
                    onNavigateToLicenses = { backStack.add(LicensesRoute) },
                    scrollToTopTrigger = settingsScrollTrigger,
                )
            }
            settingsSubScreenEntries(backStack)
            entry<LicensesRoute> {
                LicensesScreen(onBack = { backStack.removeLastOrNull() })
            }
            entry<ModelFileBrowserRoute> {
                val viewModel: ModelFileBrowserViewModel = koinViewModel()
                ModelFileBrowserScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
            comfyUIEntries(backStack)
        },
    )
}

private fun EntryProviderScope<Any>.collectionsEntry(backStack: MutableList<Any>) {
    entry<CollectionsRoute> {
        val viewModel: CollectionsViewModel = koinViewModel()
        val promptsViewModel: SavedPromptsViewModel = koinViewModel()
        val collections by viewModel.collections.collectAsStateWithLifecycle()
        CollectionsScreen(
            collections = collections,
            onCollectionClick = { id, name ->
                backStack.add(CollectionDetailRoute(id, name))
            },
            onCreateCollection = viewModel::createCollection,
            onRenameCollection = viewModel::renameCollection,
            onDeleteCollection = viewModel::deleteCollection,
            promptsViewModel = promptsViewModel,
            onNavigateToDatasets = { backStack.add(DatasetListRoute) },
        )
    }
}

private fun EntryProviderScope<Any>.datasetListEntry(backStack: MutableList<Any>) {
    entry<DatasetListRoute> {
        val viewModel: DatasetListViewModel = koinViewModel()
        DatasetListScreen(
            viewModel = viewModel,
            onDatasetClick = { id, name -> backStack.add(DatasetDetailRoute(id, name)) },
            onBack = { backStack.removeLastOrNull() },
        )
    }
}

private fun EntryProviderScope<Any>.datasetDetailEntry(backStack: MutableList<Any>) {
    entry<DatasetDetailRoute> { key ->
        val viewModel: DatasetDetailViewModel = koinViewModel(
            key = "dataset_${key.datasetId}",
        ) { parametersOf(key.datasetId) }
        DatasetDetailScreen(
            datasetName = key.datasetName,
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
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
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
            onTryInComfyUI = if (uiState.powerUserMode) {
                { sha256, modelName, meta ->
                    backStack.add(
                        ComfyUIBridgeRoute(
                            modelId = key.modelId,
                            versionId = uiState.model?.modelVersions
                                ?.getOrNull(uiState.selectedVersionIndex)?.id ?: 0L,
                            sha256Hash = sha256,
                            modelName = modelName,
                            prompt = meta?.prompt,
                            negativePrompt = meta?.negativePrompt,
                            steps = meta?.steps,
                            cfgScale = meta?.cfgScale,
                            seed = meta?.seed,
                            sampler = meta?.sampler,
                        )
                    )
                }
            } else {
                null
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

private fun EntryProviderScope<Any>.browseImagesEntry(backStack: MutableList<Any>) {
    entry<BrowseImagesRoute> {
        val viewModel: ImageGalleryViewModel = koinViewModel(key = "browse_images") { parametersOf(0L) }
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

private fun EntryProviderScope<Any>.discoveryEntry(backStack: MutableList<Any>) {
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

private fun EntryProviderScope<Any>.settingsSubScreenEntries(backStack: MutableList<Any>) {
    entry<AppearanceSettingsRoute> {
        val viewModel: SettingsViewModel = koinViewModel()
        AppearanceSettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
    entry<ContentFilterSettingsRoute> {
        val viewModel: SettingsViewModel = koinViewModel()
        ContentFilterSettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
    entry<NotificationsSettingsRoute> {
        val viewModel: SettingsViewModel = koinViewModel()
        NotificationsSettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
    entry<StorageSettingsRoute> {
        val viewModel: SettingsViewModel = koinViewModel()
        StorageSettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
    entry<AdvancedSettingsRoute> {
        val viewModel: SettingsViewModel = koinViewModel()
        AdvancedSettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onNavigateToComfyUI = { backStack.add(ComfyUISettingsRoute) },
            onNavigateToModelFiles = { backStack.add(ModelFileBrowserRoute) },
            onNavigateToTemplates = { backStack.add(WorkflowTemplateLibraryRoute) },
            onNavigateToSDWebUI = { backStack.add(SDWebUISettingsRoute) },
            onNavigateToCivitaiLink = { backStack.add(CivitaiLinkSettingsRoute) },
        )
    }
    entry<NavShortcutsSettingsRoute> {
        val viewModel: SettingsViewModel = koinViewModel()
        NavShortcutsSettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
}

private fun EntryProviderScope<Any>.comfyUIEntries(backStack: MutableList<Any>) {
    entry<CivitaiLinkSettingsRoute> {
        val viewModel: CivitaiLinkSettingsViewModel = koinViewModel()
        CivitaiLinkSettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
    entry<ComfyUISettingsRoute> {
        val viewModel: ComfyUISettingsViewModel = koinViewModel()
        ComfyUISettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onNavigateToGeneration = { backStack.add(ComfyUIGenerationRoute) },
            onNavigateToHistory = { backStack.add(ComfyUIHistoryRoute) },
        )
    }
    entry<SDWebUISettingsRoute> {
        val viewModel: SDWebUISettingsViewModel = koinViewModel()
        SDWebUISettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onNavigateToGeneration = { backStack.add(SDWebUIGenerationRoute) },
        )
    }
    entry<SDWebUIGenerationRoute> {
        val viewModel: SDWebUIGenerationViewModel = koinViewModel()
        SDWebUIGenerationScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
    entry<ComfyUIGenerationRoute> {
        val viewModel: ComfyUIGenerationViewModel = koinViewModel()
        ComfyUIGenerationScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onLoadTemplate = { backStack.add(WorkflowTemplatePickerRoute) },
        )
    }
    entry<ComfyUIQueueRoute> {
        val viewModel: ComfyUIQueueViewModel = koinViewModel()
        ComfyUIQueueScreen(viewModel = viewModel, onBack = { backStack.removeLastOrNull() })
    }
    entry<ComfyUIBridgeRoute> { key ->
        val viewModel: ComfyUIGenerationViewModel = koinViewModel(
            key = "bridge_${key.modelId}_${key.versionId}",
        )
        ComfyUIGenerationScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onLoadTemplate = { backStack.add(WorkflowTemplatePickerRoute) },
        )
    }
    workflowTemplateEntries(backStack)
    comfyUIHistoryEntries(backStack)
}

private fun EntryProviderScope<Any>.workflowTemplateEntries(backStack: MutableList<Any>) {
    entry<WorkflowTemplateLibraryRoute> {
        val viewModel: WorkflowTemplateViewModel = koinViewModel()
        WorkflowTemplateScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onCreateTemplate = { backStack.add(WorkflowTemplateEditorRoute(templateId = 0L)) },
            onEditTemplate = { template -> backStack.add(WorkflowTemplateEditorRoute(templateId = template.id)) },
        )
    }
    entry<WorkflowTemplatePickerRoute> {
        val viewModel: WorkflowTemplateViewModel = koinViewModel()
        WorkflowTemplateScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onCreateTemplate = {},
            onEditTemplate = {},
            onSelectTemplate = { backStack.removeLastOrNull() },
        )
    }
    entry<WorkflowTemplateEditorRoute> { key ->
        val viewModel: WorkflowTemplateViewModel = koinViewModel()
        val template = if (key.templateId == 0L) {
            WorkflowTemplateViewModel.emptyTemplate()
        } else {
            viewModel.uiState.value.templates.find { it.id == key.templateId }
                ?: WorkflowTemplateViewModel.emptyTemplate()
        }
        WorkflowTemplateEditorScreen(
            initialTemplate = template,
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
}

private fun EntryProviderScope<Any>.comfyUIHistoryEntries(backStack: MutableList<Any>) {
    entry<ComfyUIHistoryRoute> {
        val viewModel: ComfyUIHistoryViewModel = koinViewModel()
        ComfyUIHistoryScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onImageClick = { image ->
                backStack.add(ComfyUIOutputDetailRoute(image.id))
            },
        )
    }
    entry<ComfyUIOutputDetailRoute> { key ->
        val historyViewModel: ComfyUIHistoryViewModel = koinViewModel()
        val state by historyViewModel.uiState.collectAsStateWithLifecycle()
        val image = state.images.find { it.id == key.imageId }
        when {
            state.isLoading && image == null -> LoadingStateOverlay()
            image != null -> ComfyUIOutputDetailScreen(
                image = image,
                viewModel = historyViewModel,
                onBack = { backStack.removeLastOrNull() },
            )
        }
    }
}
