package com.riox432.civitdeck

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import com.riox432.civitdeck.domain.model.ThemeMode
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.ui.DesktopRoute
import com.riox432.civitdeck.ui.desktopFocusRing
import com.riox432.civitdeck.ui.theme.CivitDeckTheme
import com.riox432.civitdeck.util.removeLastOrNull
import androidx.compose.foundation.isSystemInDarkTheme
import org.koin.compose.viewmodel.koinViewModel

enum class DesktopTab(
    val label: String,
    val icon: ImageVector,
) {
    Discover("Discover", Icons.Default.Explore),
    Create("Create", Icons.Default.AutoAwesome),
    Library("Library", Icons.Default.FolderCopy),
    Settings("Settings", Icons.Default.Settings),
}

@Composable
fun DesktopApp(
    onScreenChanged: (String) -> Unit = {},
) {
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
        var selectedTab by remember { mutableStateOf(DesktopTab.Discover) }
        val backstack = remember {
            androidx.compose.runtime.mutableStateListOf<DesktopRoute>()
        }

        LaunchedEffect(selectedTab) {
            onScreenChanged(selectedTab.label)
        }
        val searchFocusRequester = remember { FocusRequester() }
        val isMac = remember {
            System.getProperty("os.name").lowercase().contains("mac")
        }
        val focusManager = LocalFocusManager.current

        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                // Tab / Shift+Tab -> move focus between interactive elements
                if (event.key == Key.Tab) {
                    if (event.isShiftPressed) {
                        focusManager.moveFocus(FocusDirection.Previous)
                    } else {
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                    return@onPreviewKeyEvent true
                }

                val hasModifier = if (isMac) event.isMetaPressed else event.isCtrlPressed
                handleKeyboardShortcut(
                    key = event.key,
                    hasModifier = hasModifier,
                    onTabSelected = { selectedTab = it; backstack.clear() },
                    onBack = { backstack.removeLastOrNull() },
                    onFocusSearch = {
                        selectedTab = DesktopTab.Discover
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

private fun handleKeyboardShortcut(
    key: Key,
    hasModifier: Boolean,
    onTabSelected: (DesktopTab) -> Unit,
    onBack: () -> Unit,
    onFocusSearch: () -> Unit,
): Boolean {
    // Escape -> go back / close overlay
    if (key == Key.Escape) {
        onBack()
        return true
    }

    // F5 -> refresh
    if (key == Key.F5) {
        onFocusSearch()
        return true
    }

    if (!hasModifier) return false

    return when (key) {
        Key.F -> { onFocusSearch(); true }
        Key.R -> { onFocusSearch(); true }
        Key.Comma -> { onTabSelected(DesktopTab.Settings); true }
        Key.One -> { onTabSelected(DesktopTab.Discover); true }
        Key.Two -> { onTabSelected(DesktopTab.Create); true }
        Key.Three -> { onTabSelected(DesktopTab.Library); true }
        Key.Four -> { onTabSelected(DesktopTab.Settings); true }
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
                modifier = Modifier.desktopFocusRing().focusTarget(),
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
        DesktopTab.Discover -> DiscoverTabContent(
            backstack = backstack,
            searchFocusRequester = searchFocusRequester,
            modifier = modifier,
        )
        DesktopTab.Create -> CreateTabContent(modifier = modifier)
        DesktopTab.Library -> LibraryTabContent(
            backstack = backstack,
            modifier = modifier,
        )
        DesktopTab.Settings -> SettingsTabContent(
            backstack = backstack,
            modifier = modifier,
        )
    }
}
