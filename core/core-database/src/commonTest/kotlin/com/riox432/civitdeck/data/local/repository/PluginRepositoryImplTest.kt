package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.PluginDao
import com.riox432.civitdeck.data.local.entity.PluginEntity
import com.riox432.civitdeck.domain.model.InstalledPlugin
import com.riox432.civitdeck.domain.model.InstalledPluginState
import com.riox432.civitdeck.domain.model.InstalledPluginType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [PluginRepositoryImpl] covering install/uninstall, state and config updates,
 * config default, and capabilities/type/state mapping between entity and domain.
 */
class PluginRepositoryImplTest {

    private class FakeDao : PluginDao {
        val entities = mutableListOf<PluginEntity>()
        private val flow = MutableStateFlow<List<PluginEntity>>(emptyList())

        private fun emit() {
            flow.value = entities.sortedByDescending { it.installedAt }.toList()
        }

        override suspend fun insert(entity: PluginEntity) {
            entities.removeAll { it.id == entity.id }
            entities.add(entity)
            emit()
        }

        override fun observeAll(): Flow<List<PluginEntity>> = flow

        override fun observeById(pluginId: String): Flow<PluginEntity?> =
            MutableStateFlow(entities.firstOrNull { it.id == pluginId })

        override suspend fun getById(pluginId: String): PluginEntity? =
            entities.firstOrNull { it.id == pluginId }

        override suspend fun delete(pluginId: String): Int {
            val before = entities.size
            entities.removeAll { it.id == pluginId }
            emit()
            return before - entities.size
        }

        override suspend fun updateState(pluginId: String, state: String, updatedAt: Long): Int =
            mutate(pluginId) { it.copy(state = state, updatedAt = updatedAt) }

        override suspend fun updateConfig(pluginId: String, configJson: String, updatedAt: Long): Int =
            mutate(pluginId) { it.copy(configJson = configJson, updatedAt = updatedAt) }

        private fun mutate(id: String, block: (PluginEntity) -> PluginEntity): Int {
            val idx = entities.indexOfFirst { it.id == id }
            if (idx < 0) return 0
            entities[idx] = block(entities[idx])
            emit()
            return 1
        }
    }

    private fun plugin(id: String = "p1") = InstalledPlugin(
        id = id,
        name = "Plugin",
        version = "1.0",
        author = "me",
        description = "desc",
        pluginType = InstalledPluginType.EXPORT_FORMAT,
        capabilities = listOf("export", "import"),
        minAppVersion = "2.0",
        state = InstalledPluginState.INSTALLED,
        configJson = """{"k":1}""",
    )

    @Test
    fun install_persists_plugin_with_joined_capabilities() = runTest {
        val dao = FakeDao()
        val repo = PluginRepositoryImpl(dao)
        repo.install(plugin())
        assertEquals("export,import", dao.entities[0].capabilities)
        assertEquals("EXPORT_FORMAT", dao.entities[0].pluginType)
    }

    @Test
    fun getById_maps_entity_to_domain() = runTest {
        val repo = PluginRepositoryImpl(FakeDao())
        repo.install(plugin())
        val result = repo.getById("p1")
        assertEquals(listOf("export", "import"), result?.capabilities)
        assertEquals(InstalledPluginState.INSTALLED, result?.state)
    }

    @Test
    fun getById_returns_null_when_absent() = runTest {
        val repo = PluginRepositoryImpl(FakeDao())
        assertNull(repo.getById("missing"))
    }

    @Test
    fun uninstall_removes_plugin() = runTest {
        val dao = FakeDao()
        val repo = PluginRepositoryImpl(dao)
        repo.install(plugin())
        repo.uninstall("p1")
        assertTrue(dao.entities.isEmpty())
    }

    @Test
    fun updateState_changes_state() = runTest {
        val repo = PluginRepositoryImpl(FakeDao())
        repo.install(plugin())
        repo.updateState("p1", InstalledPluginState.ACTIVE)
        assertEquals(InstalledPluginState.ACTIVE, repo.getById("p1")?.state)
    }

    @Test
    fun getConfig_returns_default_when_absent() = runTest {
        val repo = PluginRepositoryImpl(FakeDao())
        assertEquals("{}", repo.getConfig("missing"))
    }

    @Test
    fun updateConfig_persists_json() = runTest {
        val repo = PluginRepositoryImpl(FakeDao())
        repo.install(plugin())
        repo.updateConfig("p1", """{"x":2}""")
        assertEquals("""{"x":2}""", repo.getConfig("p1"))
    }

    @Test
    fun observeAll_maps_blank_capabilities_to_empty_list() = runTest {
        val dao = FakeDao()
        val repo = PluginRepositoryImpl(dao)
        repo.install(plugin().copy(capabilities = emptyList()))
        val result = repo.observeAll().first()
        assertTrue(result[0].capabilities.isEmpty())
    }
}
