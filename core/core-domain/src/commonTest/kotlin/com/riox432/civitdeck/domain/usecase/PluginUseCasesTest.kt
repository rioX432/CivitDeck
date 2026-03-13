package com.riox432.civitdeck.domain.usecase

import app.cash.turbine.test
import com.riox432.civitdeck.domain.model.InstalledPlugin
import com.riox432.civitdeck.domain.model.InstalledPluginState
import com.riox432.civitdeck.domain.model.InstalledPluginType
import com.riox432.civitdeck.domain.repository.PluginRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PluginUseCasesTest {

    private fun createTestPlugin(
        id: String = "test-plugin",
        state: InstalledPluginState = InstalledPluginState.INSTALLED,
    ) = InstalledPlugin(
        id = id,
        name = "Test Plugin",
        version = "1.0.0",
        author = "Test Author",
        description = "A test plugin",
        pluginType = InstalledPluginType.WORKFLOW_ENGINE,
        capabilities = listOf("generate"),
        minAppVersion = "1.0.0",
        state = state,
    )

    @Test
    fun installPluginInsertsNewPlugin() = runTest {
        val repo = FakePluginRepository()
        val useCase = InstallPluginUseCase(repo)
        val plugin = createTestPlugin()
        useCase(plugin)
        assertEquals(InstalledPluginState.INSTALLED, repo.getById("test-plugin")?.state)
    }

    @Test
    fun installPluginSkipsDuplicate() = runTest {
        val repo = FakePluginRepository()
        val useCase = InstallPluginUseCase(repo)
        val plugin = createTestPlugin()
        useCase(plugin)
        useCase(plugin.copy(version = "2.0.0"))
        assertEquals("1.0.0", repo.getById("test-plugin")?.version)
    }

    @Test
    fun uninstallPluginRemovesPlugin() = runTest {
        val repo = FakePluginRepository()
        repo.install(createTestPlugin())
        val useCase = UninstallPluginUseCase(repo)
        useCase("test-plugin")
        assertNull(repo.getById("test-plugin"))
    }

    @Test
    fun activatePluginUpdatesState() = runTest {
        val repo = FakePluginRepository()
        repo.install(createTestPlugin())
        val useCase = ActivatePluginUseCase(repo)
        useCase("test-plugin")
        assertEquals(InstalledPluginState.ACTIVE, repo.getById("test-plugin")?.state)
    }

    @Test
    fun deactivatePluginUpdatesState() = runTest {
        val repo = FakePluginRepository()
        repo.install(createTestPlugin(state = InstalledPluginState.ACTIVE))
        val useCase = DeactivatePluginUseCase(repo)
        useCase("test-plugin")
        assertEquals(InstalledPluginState.INACTIVE, repo.getById("test-plugin")?.state)
    }

    @Test
    fun observeInstalledPluginsEmitsUpdates() = runTest {
        val repo = FakePluginRepository()
        val useCase = ObserveInstalledPluginsUseCase(repo)
        useCase().test {
            assertEquals(emptyList(), awaitItem())
            repo.install(createTestPlugin())
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getPluginConfigReturnsConfig() = runTest {
        val repo = FakePluginRepository()
        repo.install(createTestPlugin())
        repo.updateConfig("test-plugin", """{"key":"value"}""")
        val useCase = GetPluginConfigUseCase(repo)
        assertEquals("""{"key":"value"}""", useCase("test-plugin"))
    }

    @Test
    fun updatePluginConfigPersists() = runTest {
        val repo = FakePluginRepository()
        repo.install(createTestPlugin())
        val useCase = UpdatePluginConfigUseCase(repo)
        useCase("test-plugin", """{"updated":true}""")
        assertEquals("""{"updated":true}""", repo.getConfig("test-plugin"))
    }

    @Test
    fun getPluginConfigReturnsEmptyForUnknown() = runTest {
        val repo = FakePluginRepository()
        val useCase = GetPluginConfigUseCase(repo)
        assertEquals("{}", useCase("nonexistent"))
    }
}

private class FakePluginRepository : PluginRepository {
    private val store = MutableStateFlow<Map<String, InstalledPlugin>>(emptyMap())

    override fun observeAll(): Flow<List<InstalledPlugin>> =
        store.map { it.values.toList() }

    override fun observeById(pluginId: String): Flow<InstalledPlugin?> =
        store.map { it[pluginId] }

    override suspend fun getById(pluginId: String): InstalledPlugin? =
        store.value[pluginId]

    override suspend fun install(plugin: InstalledPlugin) {
        store.value = store.value + (plugin.id to plugin)
    }

    override suspend fun uninstall(pluginId: String) {
        store.value = store.value - pluginId
    }

    override suspend fun updateState(pluginId: String, state: InstalledPluginState) {
        val existing = store.value[pluginId] ?: return
        store.value = store.value + (pluginId to existing.copy(state = state))
    }

    override suspend fun getConfig(pluginId: String): String =
        store.value[pluginId]?.configJson ?: "{}"

    override suspend fun updateConfig(pluginId: String, configJson: String) {
        val existing = store.value[pluginId] ?: return
        store.value = store.value + (pluginId to existing.copy(configJson = configJson))
    }
}
