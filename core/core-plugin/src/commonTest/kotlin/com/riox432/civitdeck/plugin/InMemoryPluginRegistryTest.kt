package com.riox432.civitdeck.plugin

import app.cash.turbine.test
import com.riox432.civitdeck.plugin.model.PluginError
import com.riox432.civitdeck.plugin.model.PluginManifest
import com.riox432.civitdeck.plugin.model.PluginState
import com.riox432.civitdeck.plugin.model.PluginType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryPluginRegistryTest {

    private fun createRegistry() = InMemoryPluginRegistry()

    private fun createTestPlugin(
        id: String = "test-plugin",
        type: PluginType = PluginType.WORKFLOW_ENGINE,
        state: PluginState = PluginState.INSTALLED,
    ): Plugin = object : Plugin {
        override val manifest = PluginManifest(
            id = id,
            name = "Test Plugin",
            version = "1.0.0",
            author = "Test Author",
            description = "A test plugin",
            pluginType = type,
            capabilities = listOf("test"),
        )
        override val state = state
        override suspend fun initialize() {}
        override suspend fun activate() {}
        override suspend fun deactivate() {}
        override suspend fun destroy() {}
    }

    @Test
    fun registerPluginSucceeds() {
        val registry = createRegistry()
        val plugin = createTestPlugin()
        val result = registry.register(plugin)
        assertTrue(result.isSuccess)
        assertEquals(plugin, registry.getPlugin("test-plugin"))
    }

    @Test
    fun registerDuplicatePluginFails() {
        val registry = createRegistry()
        registry.register(createTestPlugin())
        val result = registry.register(createTestPlugin())
        assertTrue(result.isFailure)
        assertIs<PluginError.AlreadyRegistered>(result.exceptionOrNull())
    }

    @Test
    fun unregisterPluginSucceeds() {
        val registry = createRegistry()
        registry.register(createTestPlugin())
        val result = registry.unregister("test-plugin")
        assertTrue(result.isSuccess)
        assertNull(registry.getPlugin("test-plugin"))
    }

    @Test
    fun unregisterNonexistentPluginFails() {
        val registry = createRegistry()
        val result = registry.unregister("nonexistent")
        assertTrue(result.isFailure)
        assertIs<PluginError.NotFound>(result.exceptionOrNull())
    }

    @Test
    fun getPluginsByTypeFiltersCorrectly() {
        val registry = createRegistry()
        registry.register(createTestPlugin(id = "wf-1", type = PluginType.WORKFLOW_ENGINE))
        registry.register(createTestPlugin(id = "exp-1", type = PluginType.EXPORT_FORMAT))
        registry.register(createTestPlugin(id = "wf-2", type = PluginType.WORKFLOW_ENGINE))

        val workflows = registry.getPluginsByType(PluginType.WORKFLOW_ENGINE)
        assertEquals(2, workflows.size)
        assertTrue(workflows.all { it.manifest.pluginType == PluginType.WORKFLOW_ENGINE })

        val exports = registry.getPluginsByType(PluginType.EXPORT_FORMAT)
        assertEquals(1, exports.size)

        val themes = registry.getPluginsByType(PluginType.THEME)
        assertEquals(emptyList(), themes)
    }

    @Test
    fun observePluginsEmitsUpdates() = runTest {
        val registry = createRegistry()
        registry.observePlugins().test {
            assertEquals(emptyList(), awaitItem())

            registry.register(createTestPlugin(id = "p1"))
            assertEquals(1, awaitItem().size)

            registry.register(createTestPlugin(id = "p2"))
            assertEquals(2, awaitItem().size)

            registry.unregister("p1")
            val remaining = awaitItem()
            assertEquals(1, remaining.size)
            assertEquals("p2", remaining.first().manifest.id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observePluginsByTypeEmitsFilteredUpdates() = runTest {
        val registry = createRegistry()
        registry.observePluginsByType(PluginType.THEME).test {
            assertEquals(emptyList(), awaitItem())

            registry.register(createTestPlugin(id = "wf", type = PluginType.WORKFLOW_ENGINE))
            assertEquals(0, awaitItem().size)

            registry.register(createTestPlugin(id = "theme", type = PluginType.THEME))
            assertEquals(1, awaitItem().size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unregisterThenReRegisterSucceeds() {
        val registry = createRegistry()
        registry.register(createTestPlugin())
        registry.unregister("test-plugin")
        val result = registry.register(createTestPlugin())
        assertTrue(result.isSuccess)
        assertEquals("test-plugin", registry.getPlugin("test-plugin")?.manifest?.id)
    }

    @Test
    fun getPluginReturnsNullForUnknownId() {
        val registry = createRegistry()
        assertNull(registry.getPlugin("unknown"))
    }
}
