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
import kotlin.test.assertNull

/**
 * Verifies that [GetActiveThemeUseCase] filters the THEME plugin stream down to the
 * single ACTIVE ThemePlugin, ignoring non-theme and non-active plugins, and exposes
 * its color scheme.
 */
class GetActiveThemeUseCaseTest {

    private class FakeThemePlugin(
        override val manifest: PluginManifest,
        override val state: PluginState,
        private val darkPrimary: Long = 1L,
        private val lightPrimary: Long = 2L,
    ) : ThemePlugin {
        override val themeCapabilities: Set<ThemeCapability> = emptySet()
        override fun getColorScheme(isDark: Boolean): ThemeColorScheme =
            scheme(primary = if (isDark) darkPrimary else lightPrimary)
        override suspend fun initialize() {}
        override suspend fun activate() {}
        override suspend fun deactivate() {}
        override suspend fun destroy() {}
    }

    private class FakeWorkflowPlugin(id: String) : Plugin {
        override val manifest = manifest(id, PluginType.WORKFLOW_ENGINE)
        override val state: PluginState = PluginState.ACTIVE
        override suspend fun initialize() {}
        override suspend fun activate() {}
        override suspend fun deactivate() {}
        override suspend fun destroy() {}
    }

    @Test
    fun emitsActiveThemePlugin_whenOneIsActive() = runTest {
        val active = FakeThemePlugin(manifest("active", PluginType.THEME), PluginState.ACTIVE)
        val inactive = FakeThemePlugin(manifest("inactive", PluginType.THEME), PluginState.INACTIVE)
        val useCase = buildUseCase(active, inactive)

        useCase().test {
            assertEquals("active", awaitItem()?.manifest?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emitsNull_whenNoThemeIsActive() = runTest {
        val installed = FakeThemePlugin(manifest("t", PluginType.THEME), PluginState.INSTALLED)
        val useCase = buildUseCase(installed)

        useCase().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun ignoresActiveNonThemePlugins() = runTest {
        // An ACTIVE workflow plugin must never be returned as the active theme.
        val workflow = FakeWorkflowPlugin("wf")
        // observePluginsByType(THEME) only returns THEME plugins, but assert defensively anyway.
        val useCase = buildUseCase(workflow)

        useCase().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeColorScheme_returnsActiveThemeSchemeForRequestedMode() = runTest {
        val active = FakeThemePlugin(
            manifest("active", PluginType.THEME),
            PluginState.ACTIVE,
            darkPrimary = 100L,
            lightPrimary = 200L,
        )
        val useCase = buildUseCase(active)

        useCase.observeColorScheme(isDark = true).test {
            assertEquals(100L, awaitItem()?.primary)
            cancelAndIgnoreRemainingEvents()
        }
        useCase.observeColorScheme(isDark = false).test {
            assertEquals(200L, awaitItem()?.primary)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeColorScheme_returnsNull_whenNoActiveTheme() = runTest {
        val useCase = buildUseCase()

        useCase.observeColorScheme(isDark = true).test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun buildUseCase(vararg plugins: Plugin): GetActiveThemeUseCase {
        val registry = InMemoryPluginRegistry()
        plugins.forEach { registry.register(it) }
        return GetActiveThemeUseCase(registry)
    }

    private companion object {
        fun manifest(id: String, type: PluginType) = PluginManifest(
            id = id,
            name = "Plugin $id",
            version = "1.0.0",
            author = "Test",
            description = "Test plugin",
            pluginType = type,
            capabilities = emptyList(),
        )

        fun scheme(primary: Long) = ThemeColorScheme(
            primary = primary, onPrimary = 0L, primaryContainer = 0L, onPrimaryContainer = 0L,
            secondary = 0L, onSecondary = 0L, secondaryContainer = 0L, onSecondaryContainer = 0L,
            tertiary = 0L, onTertiary = 0L, tertiaryContainer = 0L, onTertiaryContainer = 0L,
            background = 0L, onBackground = 0L, surface = 0L, onSurface = 0L,
            surfaceVariant = 0L, onSurfaceVariant = 0L,
            error = 0L, onError = 0L, errorContainer = 0L, onErrorContainer = 0L,
            outline = 0L, outlineVariant = 0L,
        )
    }
}
