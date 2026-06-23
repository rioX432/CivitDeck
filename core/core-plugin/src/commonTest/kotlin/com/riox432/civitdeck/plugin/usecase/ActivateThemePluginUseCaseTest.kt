package com.riox432.civitdeck.plugin.usecase

import com.riox432.civitdeck.domain.model.InstalledPlugin
import com.riox432.civitdeck.domain.model.InstalledPluginState
import com.riox432.civitdeck.domain.repository.PluginRepository
import com.riox432.civitdeck.plugin.InMemoryPluginRegistry
import com.riox432.civitdeck.plugin.Plugin
import com.riox432.civitdeck.plugin.ThemePlugin
import com.riox432.civitdeck.plugin.ThemeColorScheme
import com.riox432.civitdeck.plugin.capability.ThemeCapability
import com.riox432.civitdeck.plugin.model.PluginManifest
import com.riox432.civitdeck.plugin.model.PluginState
import com.riox432.civitdeck.plugin.model.PluginType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActivateThemePluginUseCaseTest {

    // --- fakes ---

    /**
     * A mutable ThemePlugin whose state transitions when activate/deactivate are called.
     * Tracks call counts so tests can assert interaction behaviour.
     */
    private class FakeThemePlugin(
        override val manifest: PluginManifest,
        initialState: PluginState = PluginState.INSTALLED,
    ) : ThemePlugin {

        private var _state = initialState
        override val state: PluginState get() = _state

        var activateCalls = 0
        var deactivateCalls = 0

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

        override suspend fun activate() {
            activateCalls++
            _state = PluginState.ACTIVE
        }

        override suspend fun deactivate() {
            deactivateCalls++
            _state = PluginState.INACTIVE
        }

        override suspend fun destroy() {}
    }

    /**
     * A non-theme plugin registered alongside themes to confirm the use case ignores other types.
     */
    private class FakeWorkflowPlugin(
        override val manifest: PluginManifest,
        override val state: PluginState = PluginState.ACTIVE,
    ) : Plugin {
        var deactivateCalls = 0
        override suspend fun initialize() {}
        override suspend fun activate() {}
        override suspend fun deactivate() { deactivateCalls++ }
        override suspend fun destroy() {}
    }

    /** Records updateState calls for assertion. */
    private class FakePluginRepository : PluginRepository {
        data class StateUpdate(val pluginId: String, val state: InstalledPluginState)

        val stateUpdates = mutableListOf<StateUpdate>()

        override fun observeAll(): Flow<List<InstalledPlugin>> = flowOf(emptyList())
        override fun observeById(pluginId: String): Flow<InstalledPlugin?> = flowOf(null)
        override suspend fun getById(pluginId: String): InstalledPlugin? = null
        override suspend fun install(plugin: InstalledPlugin) {}
        override suspend fun uninstall(pluginId: String) {}
        override suspend fun updateState(pluginId: String, state: InstalledPluginState) {
            stateUpdates.add(StateUpdate(pluginId, state))
        }
        override suspend fun getConfig(pluginId: String): String = "{}"
        override suspend fun updateConfig(pluginId: String, configJson: String) {}
    }

    // --- helpers ---

    private fun themeManifest(id: String) = PluginManifest(
        id = id,
        name = "Theme $id",
        version = "1.0.0",
        author = "Test",
        description = "Test theme",
        pluginType = PluginType.THEME,
        capabilities = emptyList(),
    )

    private fun buildUseCase(
        vararg plugins: Plugin,
        repository: FakePluginRepository = FakePluginRepository(),
    ): Triple<ActivateThemePluginUseCase, InMemoryPluginRegistry, FakePluginRepository> {
        val registry = InMemoryPluginRegistry()
        plugins.forEach { registry.register(it) }
        return Triple(ActivateThemePluginUseCase(registry, repository), registry, repository)
    }

    // --- tests ---

    @Test
    fun invoke_withActiveTheme_deactivatesItAndActivatesTarget() = runTest {
        // Arrange: one currently-active theme, one inactive theme
        val active = FakeThemePlugin(themeManifest("active-theme"), PluginState.ACTIVE)
        val target = FakeThemePlugin(themeManifest("target-theme"), PluginState.INACTIVE)
        val repo = FakePluginRepository()
        val (useCase, _, _) = buildUseCase(active, target, repository = repo)

        // Act
        useCase.invoke("target-theme")

        // Assert — previously-active theme was deactivated
        assertEquals(1, active.deactivateCalls, "Previously active theme must be deactivated exactly once")
        assertEquals(PluginState.INACTIVE, active.state, "Active theme state must become INACTIVE")

        // Assert — target theme was activated
        assertEquals(1, target.activateCalls, "Target theme must be activated exactly once")
        assertEquals(PluginState.ACTIVE, target.state, "Target theme state must become ACTIVE")

        // Assert — repository received the correct updateState calls
        assertTrue(
            repo.stateUpdates.any { it.pluginId == "active-theme" && it.state == InstalledPluginState.INACTIVE },
            "Repository must record INACTIVE update for previously-active theme",
        )
        assertTrue(
            repo.stateUpdates.any { it.pluginId == "target-theme" && it.state == InstalledPluginState.ACTIVE },
            "Repository must record ACTIVE update for target theme",
        )
    }

    @Test
    fun invoke_withMultipleActiveThemes_deactivatesAllBeforeActivatingTarget() = runTest {
        // Arrange: two simultaneously-active themes (edge case — should not happen at runtime
        // but the use case must handle it defensively)
        val active1 = FakeThemePlugin(themeManifest("a"), PluginState.ACTIVE)
        val active2 = FakeThemePlugin(themeManifest("b"), PluginState.ACTIVE)
        val target = FakeThemePlugin(themeManifest("c"), PluginState.INACTIVE)
        val repo = FakePluginRepository()
        val (useCase, _, _) = buildUseCase(active1, active2, target, repository = repo)

        // Act
        useCase.invoke("c")

        // Assert — both previously-active themes were deactivated
        assertEquals(1, active1.deactivateCalls)
        assertEquals(1, active2.deactivateCalls)
        assertEquals(1, target.activateCalls)
        assertEquals(
            3,
            repo.stateUpdates.size,
            "Two INACTIVE + one ACTIVE update expected",
        )
    }

    @Test
    fun invoke_withNullPluginId_deactivatesAllThemesAndActivatesNone() = runTest {
        // Arrange: one active theme, null passed to revert to built-in theme
        val active = FakeThemePlugin(themeManifest("theme-x"), PluginState.ACTIVE)
        val repo = FakePluginRepository()
        val (useCase, _, _) = buildUseCase(active, repository = repo)

        // Act
        useCase.invoke(null)

        // Assert — active theme was deactivated
        assertEquals(1, active.deactivateCalls)
        assertEquals(PluginState.INACTIVE, active.state)

        // Assert — no activation calls were made
        assertEquals(0, active.activateCalls)

        // Assert — only one update: INACTIVE for the formerly-active theme
        assertEquals(1, repo.stateUpdates.size)
        assertEquals(InstalledPluginState.INACTIVE, repo.stateUpdates.first().state)

        // Assert — no ACTIVE update was written
        assertTrue(
            repo.stateUpdates.none { it.state == InstalledPluginState.ACTIVE },
            "Null pluginId must not produce any ACTIVE state update",
        )
    }

    @Test
    fun invoke_withUnknownPluginId_deactivatesActiveThemesButSkipsActivation() = runTest {
        // Arrange: one active theme, request an id that is not in the registry
        val active = FakeThemePlugin(themeManifest("existing"), PluginState.ACTIVE)
        val repo = FakePluginRepository()
        val (useCase, _, _) = buildUseCase(active, repository = repo)

        // Act
        useCase.invoke("nonexistent-id")

        // Assert — existing active theme was still deactivated
        assertEquals(1, active.deactivateCalls)

        // Assert — no plugin was activated because the target was not found
        assertEquals(0, active.activateCalls)

        // Assert — repository has no ACTIVE update
        assertTrue(
            repo.stateUpdates.none { it.state == InstalledPluginState.ACTIVE },
            "Unknown pluginId must not produce any ACTIVE state update",
        )
    }

    @Test
    fun invoke_withNonThemePluginAsTarget_doesNotActivateIt() = runTest {
        // Arrange: target id exists in the registry but is a workflow plugin, not a ThemePlugin
        val workflowPlugin = FakeWorkflowPlugin(
            PluginManifest(
                id = "workflow-x",
                name = "Workflow",
                version = "1.0",
                author = "Test",
                description = "A workflow plugin",
                pluginType = PluginType.WORKFLOW_ENGINE,
                capabilities = emptyList(),
            ),
            state = PluginState.INACTIVE,
        )
        val repo = FakePluginRepository()
        val registry = InMemoryPluginRegistry()
        registry.register(workflowPlugin)
        val useCase = ActivateThemePluginUseCase(registry, repo)

        // Act
        useCase.invoke("workflow-x")

        // Assert — use case checks `target is ThemePlugin`, so a workflow plugin is ignored
        assertTrue(
            repo.stateUpdates.none { it.state == InstalledPluginState.ACTIVE },
            "Non-ThemePlugin target must not be activated",
        )
    }

    @Test
    fun invoke_whenNoThemesRegistered_completesWithoutErrors() = runTest {
        // Arrange: empty registry, no plugins at all
        val repo = FakePluginRepository()
        val registry = InMemoryPluginRegistry()
        val useCase = ActivateThemePluginUseCase(registry, repo)

        // Act & Assert — must not throw
        useCase.invoke("any-id")

        assertTrue(repo.stateUpdates.isEmpty(), "No updates expected when registry is empty")
    }

    @Test
    fun invoke_withInactiveTargetTheme_doesNotDeactivateItPrematurely() = runTest {
        // Arrange: the target theme is already INACTIVE (the only theme registered)
        val target = FakeThemePlugin(themeManifest("t"), PluginState.INACTIVE)
        val repo = FakePluginRepository()
        val (useCase, _, _) = buildUseCase(target, repository = repo)

        // Act
        useCase.invoke("t")

        // Assert — the INACTIVE plugin was NOT deactivated again (only ACTIVE ones are deactivated)
        assertEquals(0, target.deactivateCalls, "An already-inactive theme must not be deactivated")

        // Assert — it was activated
        assertEquals(1, target.activateCalls)
    }
}
