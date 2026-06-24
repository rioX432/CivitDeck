package com.riox432.civitdeck.plugin.usecase

import app.cash.turbine.test
import com.riox432.civitdeck.plugin.InMemoryPluginRegistry
import com.riox432.civitdeck.plugin.Plugin
import com.riox432.civitdeck.plugin.ThemeColorScheme
import com.riox432.civitdeck.plugin.ThemePlugin
import com.riox432.civitdeck.plugin.capability.ThemeCapability
import com.riox432.civitdeck.plugin.model.PluginManifest
import com.riox432.civitdeck.plugin.model.PluginState
import com.riox432.civitdeck.plugin.model.PluginType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies that [ObserveThemePluginsUseCase] narrows the THEME plugin stream to only
 * ThemePlugin instances (regardless of state).
 */
class ObserveThemePluginsUseCaseTest {

    private class FakeThemePlugin(override val manifest: PluginManifest) : ThemePlugin {
        override val state: PluginState = PluginState.INSTALLED
        override val themeCapabilities: Set<ThemeCapability> = emptySet()
        override fun getColorScheme(isDark: Boolean): ThemeColorScheme = ThemeColorScheme(
            primary = 0L, onPrimary = 0L, primaryContainer = 0L, onPrimaryContainer = 0L,
            secondary = 0L, onSecondary = 0L, secondaryContainer = 0L, onSecondaryContainer = 0L,
            tertiary = 0L, onTertiary = 0L, tertiaryContainer = 0L, onTertiaryContainer = 0L,
            background = 0L, onBackground = 0L, surface = 0L, onSurface = 0L,
            surfaceVariant = 0L, onSurfaceVariant = 0L,
            error = 0L, onError = 0L, errorContainer = 0L, onErrorContainer = 0L,
            outline = 0L, outlineVariant = 0L,
        )
        override suspend fun initialize() {}
        override suspend fun activate() {}
        override suspend fun deactivate() {}
        override suspend fun destroy() {}
    }

    @Test
    fun emitsAllRegisteredThemePlugins() = runTest {
        val a = FakeThemePlugin(manifest("a"))
        val b = FakeThemePlugin(manifest("b"))
        val registry = InMemoryPluginRegistry()
        registry.register(a)
        registry.register(b)
        val useCase = ObserveThemePluginsUseCase(registry)

        useCase().test {
            val ids = awaitItem().map { it.manifest.id }
            assertEquals(2, ids.size)
            assertTrue(ids.containsAll(listOf("a", "b")))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emitsEmptyList_whenNoThemePluginsRegistered() = runTest {
        val useCase = ObserveThemePluginsUseCase(InMemoryPluginRegistry())

        useCase().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun manifest(id: String) = PluginManifest(
        id = id,
        name = "Theme $id",
        version = "1.0.0",
        author = "Test",
        description = "Test theme",
        pluginType = PluginType.THEME,
        capabilities = emptyList(),
    )
}
