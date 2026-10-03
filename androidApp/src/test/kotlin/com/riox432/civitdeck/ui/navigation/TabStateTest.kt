package com.riox432.civitdeck.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class TabStateTest {

    @Test
    fun showRootTopPopsToRootAndScrollsToTop() {
        val tab = TabState(mutableListOf(SettingsRoute, DownloadQueueRoute), scrollTrigger = 3)

        tab.showRootTop()

        assertEquals(listOf<Any>(SettingsRoute), tab.backStack)
        assertEquals(4, tab.scrollTrigger)
    }

    @Test
    fun showRootTopScrollsToTopWhenAlreadyAtRoot() {
        val tab = TabState(mutableListOf(SettingsRoute))

        tab.showRootTop()

        assertEquals(listOf<Any>(SettingsRoute), tab.backStack)
        assertEquals(1, tab.scrollTrigger)
    }

    @Test
    fun reselectingADeepTabOnlyPopsToRoot() {
        val tab = TabState(mutableListOf(SettingsRoute, DownloadQueueRoute))

        tab.onReselected()

        assertEquals(listOf<Any>(SettingsRoute), tab.backStack)
        assertEquals(0, tab.scrollTrigger)
    }
}
