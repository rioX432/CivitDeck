package com.riox432.civitdeck.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.riox432.civitdeck.ui.creator.CreatorProfileScreen
import com.riox432.civitdeck.ui.creator.CreatorProfileViewModel
import com.riox432.civitdeck.ui.detail.ModelDetailScreen
import com.riox432.civitdeck.ui.detail.ModelDetailViewModel
import com.riox432.civitdeck.ui.favorites.FavoritesScreen
import com.riox432.civitdeck.ui.favorites.FavoritesViewModel
import com.riox432.civitdeck.ui.gallery.ImageGalleryScreen
import com.riox432.civitdeck.ui.gallery.ImageGalleryViewModel
import com.riox432.civitdeck.ui.prompts.SavedPromptsScreen
import com.riox432.civitdeck.ui.prompts.SavedPromptsViewModel
import com.riox432.civitdeck.ui.search.ModelSearchScreen
import com.riox432.civitdeck.ui.search.ModelSearchViewModel
import com.riox432.civitdeck.ui.settings.SettingsScreen
import com.riox432.civitdeck.ui.settings.SettingsViewModel
import com.riox432.civitdeck.ui.theme.IconSize
import com.riox432.civitdeck.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data object SearchRoute

data object FavoritesRoute

data class DetailRoute(
    val modelId: Long,
    val thumbnailUrl: String? = null,
    val sharedElementSuffix: String = "",
)

data class ImageGalleryRoute(val modelVersionId: Long)

data class CreatorRoute(val username: String)

data object SavedPromptsRoute

data object SettingsRoute

private enum class Tab { Search, Favorites }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CivitDeckNavGraph() {
    var selectedTab by rememberSaveable(
        stateSaver = mapSaver(
            save = { mapOf("tab" to it.name) },
            restore = { Tab.valueOf(it["tab"] as String) },
        ),
    ) { mutableStateOf(Tab.Search) }
    val searchBackStack = remember { mutableStateListOf<Any>(SearchRoute) }
    val favoritesBackStack = remember { mutableStateListOf<Any>(FavoritesRoute) }

    val activeBackStack = when (selectedTab) {
        Tab.Search -> searchBackStack
        Tab.Favorites -> favoritesBackStack
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (tab == selectedTab) {
                        // Pop to root on re-select
                        val stack = if (tab == Tab.Search) searchBackStack else favoritesBackStack
                        while (stack.size > 1) stack.removeAt(stack.lastIndex)
                    } else {
                        selectedTab = tab
                    }
                },
            )
        },
    ) { padding ->
        SharedTransitionLayout(modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                CivitDeckNavDisplay(activeBackStack)
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit,
) {
    Surface(
        color = NavigationBarDefaults.containerColor,
        tonalElevation = NavigationBarDefaults.Elevation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            BottomNavItem(
                selected = selectedTab == Tab.Search,
                onClick = { onTabSelected(Tab.Search) },
                activeIcon = Icons.Filled.Explore,
                inactiveIcon = Icons.Outlined.Explore,
                label = "Search",
            )
            BottomNavItem(
                selected = selectedTab == Tab.Favorites,
                onClick = { onTabSelected(Tab.Favorites) },
                activeIcon = Icons.Filled.Favorite,
                inactiveIcon = Icons.Outlined.FavoriteBorder,
                label = "Favorites",
            )
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    label: String,
) {
    val color = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .semantics { role = Role.Tab }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .height(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier = Modifier.size(IconSize.navBar),
            imageVector = if (selected) activeIcon else inactiveIcon,
            contentDescription = label,
            tint = color,
        )
        Text(
            modifier = Modifier.padding(top = Spacing.xs),
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
private fun CivitDeckNavDisplay(backStack: MutableList<Any>) {
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
                    onModelClick = { modelId, thumbnailUrl, suffix ->
                        backStack.add(DetailRoute(modelId, thumbnailUrl, suffix))
                    },
                    onSavedPromptsClick = { backStack.add(SavedPromptsRoute) },
                    onSettingsClick = { backStack.add(SettingsRoute) },
                )
            }
            entry<FavoritesRoute> {
                val viewModel: FavoritesViewModel = koinViewModel()
                val favorites by viewModel.favorites.collectAsStateWithLifecycle()
                FavoritesScreen(
                    favorites = favorites,
                    onModelClick = { modelId ->
                        backStack.add(DetailRoute(modelId))
                    },
                )
            }
            detailEntry(backStack)
            creatorEntry(backStack)
            galleryEntry(backStack)
            entry<SavedPromptsRoute> {
                val viewModel: SavedPromptsViewModel = koinViewModel()
                SavedPromptsScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
            entry<SettingsRoute> {
                val viewModel: SettingsViewModel = koinViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
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
