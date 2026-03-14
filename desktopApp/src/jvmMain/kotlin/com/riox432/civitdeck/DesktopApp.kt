package com.riox432.civitdeck

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import com.riox432.civitdeck.util.removeLastOrNull
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.riox432.civitdeck.domain.model.ThemeMode
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.ui.DesktopRoute
import com.riox432.civitdeck.ui.theme.CivitDeckTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import org.koin.compose.viewmodel.koinViewModel

enum class DesktopTab(
    val label: String,
    val icon: ImageVector,
) {
    Search("Search", Icons.Default.Search),
    Collections("Collections", Icons.Default.FolderCopy),
    Prompts("Prompts", Icons.AutoMirrored.Filled.TextSnippet),
    Feed("Feed", Icons.Default.DynamicFeed),
    Settings("Settings", Icons.Default.Settings),
}

@Composable
fun DesktopApp() {
    val displayViewModel: DisplaySettingsViewModel = koinViewModel()
    val displayState by displayViewModel.uiState.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val isDark = when (displayState.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    CivitDeckTheme(
        darkTheme = isDark,
        accentColor = displayState.accentColor,
        amoledDarkMode = displayState.amoledDarkMode,
    ) {
        var selectedTab by remember { mutableStateOf(DesktopTab.Search) }
        val backstack = remember {
            androidx.compose.runtime.mutableStateListOf<DesktopRoute>()
        }
        val searchFocusRequester = remember { FocusRequester() }
        val isMac = remember {
            System.getProperty("os.name").lowercase().contains("mac")
        }

        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val hasModifier = if (isMac) event.isMetaPressed else event.isCtrlPressed
                handleKeyboardShortcut(
                    key = event.key,
                    hasModifier = hasModifier,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it; backstack.clear() },
                    onBack = { backstack.removeLastOrNull() },
                    onFocusSearch = {
                        selectedTab = DesktopTab.Search
                        backstack.clear()
                        searchFocusRequester.requestFocus()
                    },
                )
            },
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                DesktopNavigationRail(
                    selectedTab = selectedTab,
                    onTabSelected = {
                        selectedTab = it
                        backstack.clear()
                    },
                )
                DesktopContent(
                    selectedTab = selectedTab,
                    backstack = backstack,
                    searchFocusRequester = searchFocusRequester,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

@Suppress("CyclomaticComplexMethod")
private fun handleKeyboardShortcut(
    key: Key,
    hasModifier: Boolean,
    selectedTab: DesktopTab,
    onTabSelected: (DesktopTab) -> Unit,
    onBack: () -> Unit,
    onFocusSearch: () -> Unit,
): Boolean {
    // Escape → go back / close overlay
    if (key == Key.Escape) {
        onBack()
        return true
    }

    // F5 → refresh (switch to search tab and re-focus)
    if (key == Key.F5) {
        onFocusSearch()
        return true
    }

    if (!hasModifier) return false

    return when (key) {
        // Cmd/Ctrl+F → focus search bar
        Key.F -> { onFocusSearch(); true }
        // Cmd/Ctrl+R → refresh (same as F5)
        Key.R -> { onFocusSearch(); true }
        // Cmd/Ctrl+, → open settings
        Key.Comma -> { onTabSelected(DesktopTab.Settings); true }
        // Cmd/Ctrl+1-5 → switch tabs
        Key.One -> { onTabSelected(DesktopTab.Search); true }
        Key.Two -> { onTabSelected(DesktopTab.Collections); true }
        Key.Three -> { onTabSelected(DesktopTab.Prompts); true }
        Key.Four -> { onTabSelected(DesktopTab.Feed); true }
        Key.Five -> { onTabSelected(DesktopTab.Settings); true }
        else -> false
    }
}

@Composable
private fun DesktopNavigationRail(
    selectedTab: DesktopTab,
    onTabSelected: (DesktopTab) -> Unit,
) {
    NavigationRail {
        DesktopTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}

@Composable
private fun DesktopContent(
    selectedTab: DesktopTab,
    backstack: androidx.compose.runtime.snapshots.SnapshotStateList<DesktopRoute>,
    searchFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    when (selectedTab) {
        DesktopTab.Search -> SearchTabContent(
            backstack = backstack,
            searchFocusRequester = searchFocusRequester,
            modifier = modifier,
        )
        DesktopTab.Collections -> CollectionsTabContent(
            backstack = backstack,
            modifier = modifier,
        )
        DesktopTab.Prompts -> PromptsTabContent(modifier = modifier)
        DesktopTab.Feed -> FeedTabContent(
            backstack = backstack,
            modifier = modifier,
        )
        DesktopTab.Settings -> SettingsTabContent(
            backstack = backstack,
            modifier = modifier,
        )
    }
}
