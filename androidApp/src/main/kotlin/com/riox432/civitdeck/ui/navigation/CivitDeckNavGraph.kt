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
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import com.riox432.civitdeck.domain.model.NavShortcut
import com.riox432.civitdeck.feature.search.presentation.ModelSearchViewModel
import com.riox432.civitdeck.feature.settings.presentation.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.AuthSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.StorageSettingsViewModel
import com.riox432.civitdeck.ui.search.ModelSearchScreen
import com.riox432.civitdeck.ui.settings.SettingsScreen
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Easing
import com.riox432.civitdeck.ui.update.UpdateViewModel
import org.koin.compose.viewmodel.koinViewModel

internal enum class Tab(
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
) {
    Discover("Discover", Icons.Filled.Explore, Icons.Outlined.Explore),
    Create("Create", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    Library("Library", Icons.Filled.FolderCopy, Icons.Outlined.FolderCopy),
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
internal fun CivitDeckNavGraph(initialTab: Tab = Tab.Discover) {
    val searchViewModel: ModelSearchViewModel = koinViewModel()
    val displayViewModel: DisplaySettingsViewModel = koinViewModel()
    val behaviorViewModel: AppBehaviorSettingsViewModel = koinViewModel()
    val displayState by displayViewModel.uiState.collectAsStateWithLifecycle()
    val behaviorState by behaviorViewModel.uiState.collectAsStateWithLifecycle()

    var selectedTabId by rememberSaveable { mutableStateOf(initialTab.name) }

    val fixedTabStates = remember {
        mapOf(
            Tab.Discover.name to TabState(mutableStateListOf<Any>(SearchRoute)),
            Tab.Create.name to TabState(mutableStateListOf<Any>(CreateHubRoute)),
            Tab.Library.name to TabState(mutableStateListOf<Any>(CollectionsRoute)),
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

    val activeShortcuts = if (behaviorState.powerUserMode) displayState.customNavShortcuts else emptyList()

    val navItems = buildList<Any> {
        add(Tab.Discover)
        add(Tab.Create)
        add(Tab.Library)
        activeShortcuts.forEach { add(it) }
        add(Tab.Settings)
    }

    val validTabIds = navItems.mapNotNull { navItemInfoFor(it)?.id }.toSet()
    if (selectedTabId !in validTabIds) selectedTabId = Tab.Discover.name

    val activeBackStack = fixedTabStates[selectedTabId]?.backStack
        ?: shortcutTabStates[selectedTabId]?.backStack
        ?: fixedTabStates.getValue(Tab.Discover.name).backStack

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
                        searchScrollTrigger = fixedTabStates.getValue(Tab.Discover.name).scrollTrigger,
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
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
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
                    onScanQRCode = { backStack.add(QRScannerRoute) },
                )
            }
            createHubEntry(backStack)
            collectionsEntry(backStack)
            collectionDetailEntry(backStack, compareModelId, onCancelCompare)
            datasetListEntry(backStack)
            datasetDetailEntry(backStack)
            batchTagEditorEntry(backStack)
            duplicateReviewEntry(backStack)
            detailEntry(backStack)
            similarModelsEntry(backStack)
            qrScannerEntry(backStack)
            analyticsEntry(backStack)
            notificationCenterEntry(backStack)
            browsingHistoryEntry(backStack)
            feedEntry(backStack)
            creatorEntry(backStack)
            galleryEntry(backStack)
            compareEntry(backStack)
            discoveryEntry(backStack)
            browseImagesEntry(backStack)
            entry<SettingsRoute> {
                val authVm: AuthSettingsViewModel = koinViewModel()
                val storageVm: StorageSettingsViewModel = koinViewModel()
                val behaviorVm: AppBehaviorSettingsViewModel = koinViewModel()
                val updateVm: UpdateViewModel = koinViewModel()
                val context = androidx.compose.ui.platform.LocalContext.current
                SettingsScreen(
                    authViewModel = authVm,
                    storageViewModel = storageVm,
                    appBehaviorViewModel = behaviorVm,
                    updateViewModel = updateVm,
                    onNavigateToAppearance = { backStack.add(AppearanceSettingsRoute) },
                    onNavigateToContentFilter = { backStack.add(ContentFilterSettingsRoute) },
                    onNavigateToStorage = { backStack.add(StorageSettingsRoute) },
                    onNavigateToAdvanced = { backStack.add(AdvancedSettingsRoute) },
                    onNavigateToAnalytics = { backStack.add(AnalyticsRoute) },
                    onNavigateToNotificationCenter = { backStack.add(NotificationCenterRoute) },
                    onNavigateToBrowsingHistory = { backStack.add(BrowsingHistoryRoute) },
                    onNavigateToLicenses = { backStack.add(LicensesRoute) },
                    onOpenUrl = { url ->
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(url),
                        )
                        context.startActivity(intent)
                    },
                    scrollToTopTrigger = settingsScrollTrigger,
                )
            }
            settingsSubScreenEntries(backStack)
            comfyUIEntries(backStack)
            externalServerEntries(backStack)
        },
    )
}
