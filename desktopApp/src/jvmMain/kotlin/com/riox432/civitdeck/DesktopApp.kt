package com.riox432.civitdeck

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.TextSnippet
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
import androidx.compose.ui.graphics.vector.ImageVector
import com.riox432.civitdeck.ui.DesktopRoute
import com.riox432.civitdeck.ui.theme.CivitDeckTheme

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
    CivitDeckTheme {
        var selectedTab by remember { mutableStateOf(DesktopTab.Search) }
        val backstack = remember { androidx.compose.runtime.mutableStateListOf<DesktopRoute>() }

        Surface(color = MaterialTheme.colorScheme.background) {
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
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
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
    modifier: Modifier = Modifier,
) {
    when (selectedTab) {
        DesktopTab.Search -> SearchTabContent(backstack = backstack, modifier = modifier)
        DesktopTab.Collections -> CollectionsTabContent(backstack = backstack, modifier = modifier)
        DesktopTab.Prompts -> PromptsTabContent(modifier = modifier)
        DesktopTab.Feed -> FeedTabContent(backstack = backstack, modifier = modifier)
        DesktopTab.Settings -> SettingsTabContent(modifier = modifier)
    }
}
