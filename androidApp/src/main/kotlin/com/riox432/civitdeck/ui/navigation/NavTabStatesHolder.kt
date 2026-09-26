package com.riox432.civitdeck.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.riox432.civitdeck.domain.model.NavShortcut

internal class TabState(
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

internal class NavTabStates(
    val fixed: Map<String, TabState>,
    val shortcut: Map<String, TabState>,
)

/**
 * Activity-scoped owner of every tab back stack, so stacks survive configuration changes.
 * Routes are not saveable, so the stacks still reset to their roots after process death.
 */
internal class NavTabStatesHolder : ViewModel() {
    val tabStates = NavTabStates(
        fixed = mapOf(
            Tab.Discover.name to TabState(mutableStateListOf<Any>(SearchRoute)),
            Tab.Create.name to TabState(mutableStateListOf<Any>(CreateHubRoute)),
            Tab.Library.name to TabState(mutableStateListOf<Any>(CollectionsRoute)),
            Tab.Settings.name to TabState(mutableStateListOf<Any>(SettingsRoute)),
        ),
        shortcut = mapOf(
            NavShortcut.OutputGallery.name to TabState(mutableStateListOf<Any>(ComfyUIHistoryRoute)),
            NavShortcut.Generate.name to TabState(mutableStateListOf<Any>(ComfyUIGenerationRoute)),
            NavShortcut.ImageGallery.name to TabState(mutableStateListOf<Any>(BrowseImagesRoute)),
            NavShortcut.ExternalServerGallery.name to TabState(mutableStateListOf<Any>(ExternalServerGalleryRoute)),
        ),
    )
}
