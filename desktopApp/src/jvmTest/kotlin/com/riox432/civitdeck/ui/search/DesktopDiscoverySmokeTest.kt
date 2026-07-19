package com.riox432.civitdeck.ui.search

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.riox432.civitdeck.ui.testing.DiscoveryTestTags
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Desktop discovery smoke test (issue #990). Maestro does not cover the JVM target, so the
 * unified search bar — the entry point of the discovery flow — is exercised here with the
 * Compose UI test robot: it must render, be findable by its shared test tag, and forward
 * typed input to its callback.
 */
@OptIn(ExperimentalTestApi::class)
class DesktopDiscoverySmokeTest {

    @Test
    fun searchBarRendersAndForwardsInput() = runComposeUiTest {
        var query = ""
        setContent {
            DesktopSearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = {},
            )
        }

        onNodeWithTag(DiscoveryTestTags.SEARCH_FIELD).assertIsDisplayed()
        onNodeWithTag(DiscoveryTestTags.SEARCH_FIELD).performTextInput("anime")

        assertEquals("anime", query)
    }
}
